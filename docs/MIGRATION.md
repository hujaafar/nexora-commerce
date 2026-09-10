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

The public repository began with a reviewed source snapshot. It now also retains
the original **81 commits from buy-01**, attached to the consolidated development
branch through a history-preserving merge. Their commit IDs, authors, dates,
messages, and file changes are preserved exactly. No commits were invented,
backdated, or split to increase the count. Exact source heads are recorded in
`source-provenance.json`.

The merge uses Git's `ours` strategy because the current application already
incorporates and extends that older code. It connects the history without
replacing the current marketplace with the old source tree. The merge changes
only this provenance documentation in the current tree. The branch consequently
contains **106 reachable commits**: 24 Nexora commits, 81 original commits, and
the history merge. GitHub's count on `main` updates after the protected PR is
reviewed and merged; squash merging would discard this ancestry and must not be
used for that PR.

Before publication, Gitleaks 8.30.1 scanned all 81 source commits without findings.
A separate check examined 81 commit objects and 696 historical file blobs for
known local runtime secrets and private-key/token signatures, also without
findings. The historical path inventory contains `.env.example`, not runtime
`.env` files, private key files, or screenshots. These checks are not a complete
security audit.

All five original histories remain backed up in separate local Git bundles,
outside this repository. The other four original histories are retained there;
their source contributions are represented by the consolidated application.

Application packages, Maven coordinates, Compose names, session keys, demo
identities, Jenkins job names, and documentation now use the Nexora identity.
The old databases are not modified or imported by the new Compose project.

The four original GitHub repositories and the original Nexus educational repository have not been deleted. Before retiring
them, retain the bundles, verify the new repository and workflow results, and
decide whether any repository-level issues, releases, or settings should also be
exported. Git bundles preserve Git history and refs, not GitHub issues or settings.

Nexus is integrated as infrastructure for the actual six-service Java 17 reactor.
Its separate Java 11 demonstration app and duplicate Jenkins pipeline are not
part of the running marketplace. A new Java 11 artifact verifier provides tested
version retrieval and SHA-256 checking without duplicating the application.
This does not convert the six Java 17 services to Java 11.
Provisioning uses new ports, random credentials,
and a new volume. No old screenshots, credentials, or machine-specific evidence
were imported. Its full original Git history is retained in `nexus.bundle`.

Restore a source bundle with `git clone /path/to/buy-01.bundle buy-01-restored`.
