# Review policy

The public repository ships CI and CODEOWNERS. Branch protection is a GitHub
repository setting and is not claimed to be enabled by these files.

Recommended team workflow: open a pull request, pass `CI / backend`,
`CI / frontend`, and `CI / configuration`, address review feedback, and merge.
Teams can require one or two independent reviewers according to their needs.
The optional Sonar workflow is manual and needs a reachable configured server;
do not require its status before that infrastructure exists.

Secrets belong in runtime .env files or GitHub Actions secrets, never in source.
Public pull requests run on disposable hosted runners with read-only permissions.
