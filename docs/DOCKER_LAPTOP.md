# Running Nexora on a laptop

The ordinary startup scripts select `compose.yml` plus `compose.laptop.yml`.
All six Java services, MongoDB, Kafka, MinIO, and the storefront remain enabled.
The DevOps labs are separate workloads; running the shop, Jenkins builds, Nexus,
and SonarQube together is unsuitable for a memory-constrained laptop.

## Everyday commands

From the repository directory in PowerShell:

```powershell
# Once Docker is responsive, stop any previously created lab containers and
# disable their old automatic restart policy. This preserves their data.
.\scripts\stop-tools.ps1
.\scripts\start.ps1

# After changing source code, rebuild sequentially using the existing cache.
.\scripts\start.ps1 -Build

# Finish your session and release app memory; named volumes remain.
.\scripts\stop.ps1
```

The POSIX equivalents are `bash scripts/stop-tools.sh`, `bash scripts/start.sh`,
`bash scripts/start.sh --build`, and `bash scripts/stop.sh`.
Quit Docker Desktop when it is not needed. Stopping only the frontend leaves the
databases, broker, and Java services running.

Startup refuses low disk space (less than 5 GB, or 10 GB when building). PowerShell
also checks available Windows RAM (3 GB to start; 4 GB to build). These checks are
headroom checks, not a guarantee: other applications can consume memory afterward,
and Docker's disk image may be located on a different drive. Inspect that drive too.
Do not use `docker system prune --volumes` to solve memory pressure.

## Limits and behavior

| Container | RAM ceiling | CPU ceiling |
|---|---:|---:|
| MongoDB | 512 MiB | 1 CPU |
| Kafka | 512 MiB | 1 CPU |
| MinIO | 320 MiB | 1 CPU |
| Discovery | 320 MiB | 1 CPU |
| Each of the five other Java services | 448 MiB | 1 CPU |
| Frontend | 64 MiB | 0.5 CPU |
| Temporary catalog seeder | 128 MiB | 0.5 CPU |

The ten long-running container ceilings sum to **3,968 MiB**. This is an upper
bound on their configured memory, not measured usage and not a Docker-wide cap.
The Linux VM, Docker daemon, image builds, and other projects use additional RAM.
No global Docker/WSL settings are changed by these files.

Java heaps start at 32 MiB and top out at 192 MiB (128 MiB for discovery), with
two visible processors and Serial GC. MongoDB's WiredTiger cache is 256 MiB.
Kafka uses a small TCP health probe instead of starting another Java process
every ten seconds; it checks listener readiness, while acceptance journeys
exercise event-producing operations. Other probes run every thirty seconds.
Kafka has a 256 MiB heap and a 32 MiB cleaner deduplication buffer; compaction
stays enabled. Its [default cleaner buffer](https://kafka.apache.org/39/configuration/broker-configs/#logcleanerdedupebuffersize)
alone reserves 128 MiB, which exhausted the initial 192 MiB heap during the
first CI startup trial. The committed settings include that correction.
Container swap is disabled to avoid exchanging an OOM for a long disk thrash.
Each container keeps at most two 5 MB Docker log files.

Builds use one Compose build operation at a time, with a 512 MiB Maven heap,
a 1 GiB Node heap, and two Angular workers. Normal startup reuses built images.
BuildKit can reuse the common backend build stage across all service images.

For a larger development machine, `start.ps1 -Full` / `start.sh --full` selects
the base application settings. CI and deployment scripts choose their profiles
explicitly. This option is not intended as a workaround for low host resources.

## Optional DevOps labs

New Jenkins, SonarQube, and Nexus containers use `restart: "no"`. Existing
containers retain their old policy until recreated or updated: `stop-tools.ps1`
updates just the three Nexora lab projects, then stops them without deleting
containers, volumes, artifacts, or history. It requires a responsive Docker engine.

Jenkins now starts one worker with one executor. For a larger lab host, start the
second worker explicitly:

```sh
docker compose -f jenkins/compose.yml --profile distributed up -d
```

Each worker has one executor. JCasC still lists the second
node, which stays offline until that optional worker starts. Local full-pipeline
work also needs SonarQube, Nexus, build memory, and nested Docker deployments;
run that workload on a machine sized for it. GitHub's hosted CI verifies the
project without taking laptop RAM.

## Verification

The `Browser and HTTPS acceptance` CI job starts the complete app with laptop
limits, runs the real HTTP and HTTPS API journeys and browser journeys, then
checks the running containers for applied limits, health, OOM kills, and restarts.
It uploads `laptop-http-resources.json` and `laptop-https-resources.json`, including
Docker memory/CPU snapshots. This verifies demo traffic; it is not a load test.

For manual inspection: `docker stats --no-stream`. If a service is OOM killed
under a heavier workload, inspect its logs and raise that service's limit on a
host with enough memory instead of repeatedly restarting the whole stack.
