# Local SMTP trust certificates

This directory is mounted read-only into the Jenkins controller and build
agents. Public root certificates ending in `.crt` are imported into their Linux
CA stores at startup. The certificate files are ignored by Git because they
are specific to the host machine.

`scripts/configure-gmail.ps1` automatically exports the public AVG Web/Mail
Shield root here when AVG is installed. It never exports a private key. This
lets Git/curl verify AVG-inspected HTTPS and SMTP connections without disabling
TLS checks. The controller needs this trust before it can fetch the
`Jenkinsfile`; agents need it for checkout and notification stages.
