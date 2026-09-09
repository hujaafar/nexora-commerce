# Consolidation

The four educational repositories represent successive layers of one application:

| Source | Retained contribution |
|---|---|
| buy-01 | Identity, catalog, uploads, discovery, gateway, Angular, Compose |
| mr-jenk | Jenkins controller and agents, immutable image tags, deployment, rollback, Mailpit |
| safe-zone | SonarQube and coverage infrastructure; media immutability and tests |
| buy-02 | Orders, cart, wishlist, checkout, inventory operations, analytics |

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

The four original GitHub repositories have not been deleted. Before retiring
them, retain the bundles, verify the new repository and workflow results, and
decide whether any repository-level issues, releases, or settings should also be
exported. Git bundles preserve Git history and refs, not GitHub issues or settings.

Restore a source bundle with `git clone /path/to/buy-01.bundle buy-01-restored`.
