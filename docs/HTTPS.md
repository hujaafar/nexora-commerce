# HTTPS across the marketplace

The regular development stack uses HTTP. `compose.tls.yml` enables certificate-verified HTTPS for browser → Nginx → gateway → each service, Eureka registration/discovery, order → product inventory calls, and media → MinIO. Each server has a distinct certificate and key. Services receive only their own key plus the shared public CA truststore. Certificate and hostname verification stay enabled.

```powershell
# Initialize the regular .env first if this is a new clone.
./scripts/tls-start.ps1
curl.exe --cacert certs/tls/trust/ca.pem https://localhost:8443/api/products
```

On Linux/macOS:

```sh
docker run --rm -v "$PWD:/workspace" -w /workspace maven:3.9.11-eclipse-temurin-17 bash scripts/tls-init.sh
docker compose --env-file .env --env-file certs/tls/.env -f compose.yml -f compose.tls.yml up -d --build --wait
curl --cacert certs/tls/trust/ca.pem https://localhost:8443/api/products
```

Certificates and passwords are generated under ignored `certs/tls/`. The initializer retains existing identities on repeat runs. The CA private key is never mounted into an application container. Keep this directory private and back it up separately from source. Server certificates expire after one year; replace identities before expiry. The truststore password `changeit` protects a store containing only a public CA certificate, not private keys.

The local CA is not automatically installed into Windows or browsers. Use the explicit `--cacert` flag for CLI verification, or choose to trust the local CA yourself. Public hosting needs a real domain and publicly trusted certificates from your certificate provider. The separate `compose.https.yml` Caddy example demonstrates automated public ingress certificates; by itself it terminates TLS at ingress and does not encrypt internal API hops.

MongoDB and Kafka are separate non-HTTP protocols on the local private network. Their network traffic is not encrypted by this profile. Internet-facing production deployments need their own database/broker TLS and access policies.

Spring documents secure Eureka registration in its [Netflix reference](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/) and certificate verification in the [Gateway TLS reference](https://docs.spring.io/spring-cloud-gateway/reference/4.2/spring-cloud-gateway/tls-and-ssl.html).

Return to the ordinary local HTTP stack with `docker compose up -d --force-recreate`; application volumes are retained.
