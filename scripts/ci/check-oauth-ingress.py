"""Exercise the real Caddy OAuth route over trusted local TLS with fixture upstreams."""
import pathlib
import subprocess
import tempfile
import time
import uuid


def docker(*args):
    return subprocess.run(
        ["docker", *args], check=True, capture_output=True, text=True, timeout=60
    ).stdout.strip()


root = pathlib.Path(__file__).resolve().parents[2]
name = "nexora-ingress-test-" + uuid.uuid4().hex[:12]
with tempfile.TemporaryDirectory(prefix="nexora-ingress-") as temporary:
    directory = pathlib.Path(temporary)
    config = (root / "deploy/Caddyfile").read_text(encoding="utf-8")
    config = config.replace("gateway-service:8080", "127.0.0.1:18080")
    config = config.replace("frontend:80", "127.0.0.1:18081")
    config += '\nhttp://:18080 {\n respond "gateway|{http.request.uri}|{http.request.header.X-Forwarded-Proto}"\n}\n'
    config += '\nhttp://:18081 {\n respond "frontend|{http.request.uri}|{http.request.header.X-Forwarded-Proto}"\n}\n'
    config_path = directory / "Caddyfile"
    config_path.write_text(config, encoding="utf-8")
    container = None
    try:
        container = docker(
            "run", "-d", "--name", name, "--memory=64m", "--cpus=0.5",
            "-e", "DOMAIN=localhost", "--network=none",
            "--mount", f"type=bind,source={config_path},target=/etc/caddy/Caddyfile,readonly",
            "caddy:2.10-alpine",
        )
        def fetch(path, trusted=True):
            certificate = "/data/caddy/pki/authorities/local/root.crt" if trusted else "/dev/null"
            return subprocess.run(
                ["docker", "exec", "-e", f"SSL_CERT_FILE={certificate}",
                 "-e", "SSL_CERT_DIR=/nonexistent", container,
                 "wget", "-S", "-O", "-", "-T", "5", f"https://localhost{path}"],
                capture_output=True, text=True, timeout=10,
            )

        for attempt in range(30):
            ready = fetch("/login")
            if ready.returncode == 0:
                break
            if attempt == 29:
                raise AssertionError("Caddy did not become ready: " + ready.stderr)
            time.sleep(1)
        if fetch("/login", trusted=False).returncode == 0:
            raise AssertionError("TLS request unexpectedly succeeded without trusting the fixture CA")
        print("PASS: untrusted certificate rejected")
        cases = [
            ("/api/auth/oauth2/callback/google?code=fixture&state=example", "gateway", "/auth/oauth2/callback/google?code=fixture&state=example"),
            ("/api/auth/oauth2/callback/github?code=fixture&state=example", "gateway", "/auth/oauth2/callback/github?code=fixture&state=example"),
            ("/api/auth/oauth2/authorize/google", "gateway", "/auth/oauth2/authorize/google"),
            ("/login", "frontend", "/login"),
            ("/api/products", "frontend", "/api/products"),
        ]
        for path, upstream, forwarded_path in cases:
            response = fetch(path)
            if response.returncode:
                raise AssertionError("Trusted TLS request failed: " + response.stderr)
            actual = response.stdout
            expected = f"{upstream}|{forwarded_path}|https"
            if actual != expected:
                raise AssertionError(f"Ingress route mismatch: {actual!r} != {expected!r}")
            if upstream == "gateway":
                headers = response.stderr.lower()
                assert "cache-control: no-store" in headers
                assert "referrer-policy: no-referrer" in headers
            print(f"PASS: trusted HTTPS routing for {path.split('?')[0]}")
    except Exception:
        if container:
            print(docker("logs", container))
        raise
    finally:
        if container:
            docker("rm", "-f", "-v", container)
