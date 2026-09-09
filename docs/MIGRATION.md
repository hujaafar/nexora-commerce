# Consolidation

The five educational repositories represent successive layers of one application:

| Source | Retained contribution |
|---|---|
| buy-01 | Identity, catalog, uploads, discovery, gateway, Angular, Compose |
| mr-jenk | Jenkins controller and agents, immutable image tags, deployment, rollback, Mailpit |
| safe-zone | SonarQube and coverage infrastructure; media immutability and tests |
| buy-02 | Orders, cart, wishlist, checkout, inventory operations, analytics |
| nexus | Maven hosted/proxy/group repositories, Docker registry, role separation, versioned publication and retrieval |

The most complete source snapshot, buy-02, provides the base. Safe-zone's media
implementation and its matching tests were brought forward. The other projects
were compared by tracked file paths and content; the older marketplace and
Jenkins components already exist in the base.

The public repository begins with a reviewed source snapshot. Private historical
commits are preserved in separate local Git bundles, outside this repository.
Exact source heads are recorded in `source-provenance.json`.

Application packages, Maven coordinates, Compose names, session keys, demo
identities, Jenkins job names, and documentation now use the Nexora identity.
The old databases are not modified or imported by the new Compose project.

The four original GitHub repositories and the original Nexus educational repository have not been deleted. Before retiring
them, retain the bundles, verify the new repository and workflow results, and
decide whether any repository-level issues, releases, or settings should also be
exported. Git bundles preserve Git history and refs, not GitHub issues or settings.

Nexus is integrated as infrastructure for the actual six-service Java 17 reactor.
Its separate Java 11 demonstration app and duplicate Jenkins pipeline are not
part of the running marketplace. Provisioning uses new ports, random credentials,
and a new volume. No old screenshots, credentials, or machine-specific evidence
were imported. Its full original Git history is retained in `nexus.bundle`.

Restore a source bundle with `git clone /path/to/buy-01.bundle buy-01-restored`.
