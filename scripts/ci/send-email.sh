#!/usr/bin/env bash
# File purpose: Sends one Jenkins result through Mailpit or authenticated SMTP.
#
# Learning map:
# - Mailpit remains the clone-safe default and requires no credential.
# - Gmail credentials enter only through Jenkins withCredentials masking.
# - A temporary curl config keeps the App Password out of process arguments.
# - Header validation prevents a build parameter from injecting extra headers.
set -Eeuo pipefail

: "${SMTP_HOST:?SMTP_HOST is required}"
: "${SMTP_PORT:?SMTP_PORT is required}"
: "${EMAIL_TO:?EMAIL_TO is required}"
: "${EMAIL_SUBJECT:?EMAIL_SUBJECT is required}"
: "${EMAIL_BODY:?EMAIL_BODY is required}"

if [[ "${EMAIL_TO}" == *$'\r'* || "${EMAIL_TO}" == *$'\n'* ]]; then
  echo 'EMAIL_TO contains an invalid newline.' >&2
  exit 2
fi
if [[ "${EMAIL_SUBJECT}" == *$'\r'* || "${EMAIL_SUBJECT}" == *$'\n'* ]]; then
  echo 'EMAIL_SUBJECT contains an invalid newline.' >&2
  exit 2
fi
if [[ ! "${EMAIL_TO}" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$ ]]; then
  echo 'EMAIL_TO is not a valid email address.' >&2
  exit 2
fi

email_from="${SMTP_USERNAME:-jenkins@nexora-commerce.local}"
if [[ "${email_from}" == *$'\r'* || "${email_from}" == *$'\n'* ]]; then
  echo 'SMTP username contains an invalid newline.' >&2
  exit 2
fi

message_file="$(mktemp)"
credentials_file=''
cleanup() {
  rm -f "${message_file}"
  if [[ -n "${credentials_file}" ]]; then
    rm -f "${credentials_file}"
  fi
}
trap cleanup EXIT

{
  printf 'From: Nexora Commerce Jenkins <%s>\r\n' "${email_from}"
  printf 'To: %s\r\n' "${EMAIL_TO}"
  printf 'Subject: %s\r\n' "${EMAIL_SUBJECT}"
  printf 'Date: %s\r\n' "$(date -R)"
  printf 'Message-ID: <%s.%s@nexora-commerce.local>\r\n' "${BUILD_NUMBER:-manual}" "$(date +%s)"
  printf 'MIME-Version: 1.0\r\n'
  printf 'Content-Type: text/plain; charset=UTF-8\r\n'
  printf 'Content-Transfer-Encoding: 8bit\r\n'
  printf '\r\n'
  printf '%s\n' "${EMAIL_BODY}" | sed 's/$/\r/'
} > "${message_file}"

curl_arguments=(
  --silent
  --show-error
  --fail
  --connect-timeout 15
  --max-time 45
  --url "smtp://${SMTP_HOST}:${SMTP_PORT}"
  --mail-from "${email_from}"
  --mail-rcpt "${EMAIL_TO}"
  --upload-file "${message_file}"
)

case "${SMTP_USE_TLS:-false}" in
  true|TRUE|1|yes|YES)
    curl_arguments+=(--ssl-reqd)
    ;;
esac

if [[ -n "${SMTP_USERNAME:-}" || -n "${SMTP_APP_PASSWORD:-}" ]]; then
  : "${SMTP_USERNAME:?SMTP_USERNAME and SMTP_APP_PASSWORD must be set together}"
  : "${SMTP_APP_PASSWORD:?SMTP_USERNAME and SMTP_APP_PASSWORD must be set together}"
  if [[ "${SMTP_USERNAME}" == *'"'* || "${SMTP_APP_PASSWORD}" == *'"'* ]]; then
    echo 'SMTP credentials contain an unsupported quote character.' >&2
    exit 2
  fi
  credentials_file="$(mktemp)"
  chmod 600 "${credentials_file}"
  printf 'user = "%s:%s"\n' "${SMTP_USERNAME}" "${SMTP_APP_PASSWORD}" > "${credentials_file}"
  curl_arguments+=(--config "${credentials_file}")
fi

curl "${curl_arguments[@]}"
echo 'SMTP server accepted the email notification.'
