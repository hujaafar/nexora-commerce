#!/usr/bin/env bash
# Intended for a disposable hosted runner. This creates its own local secrets
# and verifies the exact production artifacts built by the preceding CI jobs.
set -Eeuo pipefail
mkdir -p test-results/acceptance
python3 - <<'PY'
import pathlib, secrets
source = pathlib.Path('.env.example').read_text()
values = {'JWT_SECRET': secrets.token_hex(32), 'INTERNAL_SERVICE_TOKEN': secrets.token_hex(32),
          'MONGO_ROOT_PASSWORD': secrets.token_hex(24), 'MINIO_ROOT_PASSWORD': secrets.token_hex(24),
          'SEED_DEMO_USERS': 'false', 'MEDIA_PUBLIC_BASE_URL': 'http://localhost:4200/api/media/images'}
pathlib.Path('.env').write_text('\n'.join(
    f'{line.split("=", 1)[0]}={values[line.split("=", 1)[0]]}'
    if '=' in line and line.split('=', 1)[0] in values else line
    for line in source.splitlines()) + '\n')
pathlib.Path('.env').chmod(0o600)
PY
bash scripts/ci/build-artifact-images.sh
docker compose -f compose.yml -f compose.jenkins.yml -f compose.laptop.yml up -d --no-build --wait --wait-timeout 600
bash scripts/ci/health-check.sh http://localhost:4200 360
curl --fail --silent --show-error --max-time 15 http://localhost:4200/media | grep -Fq '<app-root'
API_BASE=http://localhost:4200/api node scripts/integration-test.mjs 2>&1 | tee test-results/acceptance/http-api.log
(cd frontend && npm run test:e2e) 2>&1 | tee test-results/acceptance/browser.log
python3 scripts/ci/check-laptop-runtime.py | tee test-results/acceptance/laptop-http-resources.json

# Reuse the same persistent test databases but switch every HTTP service hop
# to separately issued TLS identities. No certificate verification is disabled.
docker run --rm -v "$PWD:/workspace" -w /workspace maven:3.9.11-eclipse-temurin-17 bash scripts/tls-init.sh
sudo chown "$(id -u):$(id -g)" certs/tls/.env
docker compose --env-file .env --env-file certs/tls/.env \
  -f compose.yml -f compose.jenkins.yml -f compose.laptop.yml -f compose.tls.yml \
  up -d --no-build --wait --wait-timeout 600
for attempt in $(seq 1 60); do
  if curl --fail --silent --cacert certs/tls/trust/ca.pem https://localhost:8443/api/products > /dev/null; then break; fi
  sleep 3
done
NODE_EXTRA_CA_CERTS="$PWD/certs/tls/trust/ca.pem" API_BASE=https://localhost:8443/api \
  node scripts/integration-test.mjs 2>&1 | tee test-results/acceptance/https-api.log
curl --fail --silent --show-error --max-time 15 --cacert certs/tls/trust/ca.pem \
  https://localhost:8443/media | grep -Fq '<app-root'
python3 scripts/ci/check-laptop-runtime.py | tee test-results/acceptance/laptop-https-resources.json
echo 'PASS: production browser journeys and certificate-verified HTTPS API journeys.'
