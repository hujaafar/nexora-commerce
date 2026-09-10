# Review policy

Main branch protection was enabled and read back from GitHub on 10 September
2026. It applies to administrators and requires one independent approval after
the last push. New commits dismiss stale reviews; conversations must be resolved.
The branch must be current with main and pass `backend`, `frontend`,
`configuration`, `Java 11 artifact verifier`, `SonarQube quality gate`,
`Browser and HTTPS acceptance`, and `Deployment and rollback acceptance`.
Force pushes and branch deletion are disabled.

The audit is in [PR #1](https://github.com/hujaafar/nexora-commerce/pull/1).
Its author is the repository owner, so another reviewer is still required.
Assistant testing and technical hotspot review do not constitute that independent
approval. Resolve findings or document specific technical justifications before
approval. No protection bypass is part of this workflow.

These are server settings, not settings inherited by a clone. New repositories
must apply equivalent [GitHub branch protection](https://docs.github.com/en/rest/branches/branch-protection).

Secrets belong in runtime .env files or GitHub Actions secrets, never in source.
Public pull requests run on disposable hosted runners with read-only permissions.
