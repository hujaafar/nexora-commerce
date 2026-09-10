"""Provision and export an isolated SonarQube instance for a GitHub snapshot scan.

Only disposable, job-local credentials are generated here. No private runner or
repository secret is exposed to pull-request code.
"""
import base64
import json
import os
from pathlib import Path
import secrets
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

base = os.environ.get("SONAR_HOST_URL", "http://127.0.0.1:9000").rstrip("/")
project = "nexora-commerce"
state = Path(os.environ.get("RUNNER_TEMP", "test-results")) / "nexora-sonar-state.json"


def api(path, username, password="", data=None):
    authorization = base64.b64encode(f"{username}:{password}".encode()).decode()
    body = urllib.parse.urlencode(data).encode() if data is not None else None
    request = urllib.request.Request(base + path, data=body, headers={"Authorization": "Basic " + authorization})
    with urllib.request.urlopen(request, timeout=60) as response:
        content = response.read()
        return json.loads(content) if content else {}


def bootstrap():
    deadline = time.monotonic() + 300
    while True:
        try:
            if api("/api/system/status", "admin", "admin").get("status") == "UP":
                break
        except (OSError, urllib.error.HTTPError):
            pass
        if time.monotonic() >= deadline:
            raise TimeoutError("SonarQube did not become ready within five minutes")
        time.sleep(3)
    password = "Sq9!" + secrets.token_hex(24)
    api("/api/users/change_password", "admin", "admin", {
        "login": "admin", "previousPassword": "admin", "password": password,
    })
    api("/api/projects/create", "admin", password, {"project": project, "name": "Nexora Commerce CI snapshot"})
    gate = "Nexora snapshot"
    # Keep Sonar way's new-code conditions and add overall conditions. Fresh
    # snapshots still enforce coverage and existing issues on every branch/PR.
    api("/api/qualitygates/copy", "admin", password, {"name": gate, "sourceName": "Sonar way"})
    for metric, operator, threshold in (
        ("coverage", "LT", "60"),
        ("duplicated_lines_density", "GT", "3"),
        ("code_smells", "GT", "0"),
        ("software_quality_security_rating", "GT", "1"),
        ("software_quality_reliability_rating", "GT", "1"),
    ):
        api("/api/qualitygates/create_condition", "admin", password, {
            "gateName": gate, "metric": metric, "op": operator, "error": threshold,
        })
    api("/api/qualitygates/select", "admin", password, {"gateName": gate, "projectKey": project})
    token = api("/api/user_tokens/generate", "admin", password, {
        "name": "snapshot-analysis", "type": "PROJECT_ANALYSIS_TOKEN", "projectKey": project,
    })["token"]
    state.parent.mkdir(parents=True, exist_ok=True)
    state.write_text(json.dumps({"password": password, "token": token}))
    state.chmod(0o600)
    if os.environ.get("GITHUB_ENV"):
        print(f"::add-mask::{password}")
        print(f"::add-mask::{token}")
        with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as environment:
            environment.write(f"SONAR_TOKEN={token}\n")
    print("Isolated project and quality gate configured.")


def report():
    credentials = json.loads(state.read_text())
    destination = Path("test-results/sonar")
    destination.mkdir(parents=True, exist_ok=True)
    endpoints = {
        "quality-gate": f"/api/qualitygates/project_status?projectKey={project}",
        "issues": f"/api/issues/search?componentKeys={project}&resolved=false&ps=500",
        "metrics": f"/api/measures/component?component={project}&metricKeys=coverage,code_smells,vulnerabilities,bugs,duplicated_lines_density,security_hotspots",
    }
    for name, path in endpoints.items():
        result = api(path, "admin", credentials["password"])
        (destination / f"{name}.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    gate = json.loads((destination / "quality-gate.json").read_text())["projectStatus"]
    print("Snapshot quality gate:", gate["status"])
    if gate["status"] != "OK":
        raise SystemExit(1)


if __name__ == "__main__":
    if sys.argv[1] == "bootstrap":
        bootstrap()
    elif sys.argv[1] == "report":
        report()
    else:
        raise SystemExit("Use bootstrap or report")
