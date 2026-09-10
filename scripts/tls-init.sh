#!/usr/bin/env bash
# Create a local CA and distinct server identities. Run through tls-start.ps1,
# or in the documented Maven container (OpenSSL and keytool are required).
set -Eeuo pipefail
cd "$(dirname "$0")/.."
if [[ -f certs/tls/.env ]]; then
  echo 'TLS identities already exist; retaining the current CA and certificates.'
  exit 0
fi
umask 077
mkdir -p certs/tls/ca certs/tls/trust
export TLS_KEYSTORE_PASSWORD="$(openssl rand -hex 24)"
openssl req -x509 -newkey rsa:3072 -nodes -sha256 -days 730 \
  -keyout certs/tls/ca/private.key -out certs/tls/trust/ca.pem \
  -subj '/CN=Nexora local development CA' \
  -addext 'basicConstraints=critical,CA:TRUE' -addext 'keyUsage=critical,keyCertSign,cRLSign' 2>/dev/null
keytool -importcert -noprompt -alias nexora-local-ca -file certs/tls/trust/ca.pem \
  -keystore certs/tls/trust/truststore.p12 -storetype PKCS12 -storepass changeit >/dev/null 2>&1
for service in discovery-service gateway-service user-service product-service media-service order-service frontend minio; do
  directory="certs/tls/${service}"
  mkdir -p "$directory"
  openssl req -newkey rsa:2048 -nodes -keyout "${directory}/private.key" \
    -out "${directory}/server.csr" -subj "/CN=${service}" 2>/dev/null
  cat > "${directory}/extensions.cnf" <<EOF
subjectAltName=DNS:${service},DNS:localhost,IP:127.0.0.1
basicConstraints=critical,CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
EOF
  openssl x509 -req -in "${directory}/server.csr" -CA certs/tls/trust/ca.pem \
    -CAkey certs/tls/ca/private.key -CAcreateserial -days 365 -sha256 \
    -extfile "${directory}/extensions.cnf" -out "${directory}/public.crt" 2>/dev/null
  openssl pkcs12 -export -name server -inkey "${directory}/private.key" \
    -in "${directory}/public.crt" -certfile certs/tls/trust/ca.pem \
    -out "${directory}/server.p12" -passout env:TLS_KEYSTORE_PASSWORD
  # The Spring runtime UID can read its own mounted identity; no service gets
  # the CA key or another service's private key.
  chown -R 10001:10001 "$directory"
  chmod 750 "$directory"
done
chmod 755 certs/tls certs/tls/trust
chmod 644 certs/tls/trust/ca.pem certs/tls/trust/truststore.p12
printf 'TLS_KEYSTORE_PASSWORD=%s\n' "$TLS_KEYSTORE_PASSWORD" > certs/tls/.env
echo 'Generated local TLS identities. Trust only certs/tls/trust/ca.pem for local testing.'
