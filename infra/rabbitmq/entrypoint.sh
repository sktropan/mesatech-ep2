#!/bin/bash
# Genera definitions.json dentro de la imagen (no trae envsubst ni python) y arranca RabbitMQ.
set -euo pipefail

: "${RABBITMQ_CATALOGO_PASSWORD:?Falta RABBITMQ_CATALOGO_PASSWORD}"
: "${RABBITMQ_SOLICITUDES_PASSWORD:?Falta RABBITMQ_SOLICITUDES_PASSWORD}"
: "${RABBITMQ_UI_PASSWORD:?Falta RABBITMQ_UI_PASSWORD}"

json_escape() {
  local value=$1
  value=${value//\\/\\\\}
  value=${value//\"/\\\"}
  printf '%s' "$value"
}

catalogo_pass=$(json_escape "$RABBITMQ_CATALOGO_PASSWORD")
solicitudes_pass=$(json_escape "$RABBITMQ_SOLICITUDES_PASSWORD")
ui_pass=$(json_escape "$RABBITMQ_UI_PASSWORD")

template=$(cat /etc/rabbitmq/definitions.template.json)
template=${template//__CATALOGO_PASSWORD__/$catalogo_pass}
template=${template//__SOLICITUDES_PASSWORD__/$solicitudes_pass}
template=${template//__UI_PASSWORD__/$ui_pass}
printf '%s\n' "$template" > /etc/rabbitmq/definitions.json

exec docker-entrypoint.sh rabbitmq-server
