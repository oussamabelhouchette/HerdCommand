#!/usr/bin/env bash
# Amazon Linux 2023: install Docker + Git, clone `stage`, start Compose, register Keycloak redirect URI.
set -euo pipefail

HERDCOMMAND_HOME="${HERDCOMMAND_HOME:-/opt/herdcommand}"
HERDCOMMAND_GIT_SSH="${HERDCOMMAND_GIT_SSH:-git@github.com:oussamabelhouchette/HerdCommand.git}"
HERDCOMMAND_BRANCH="${HERDCOMMAND_BRANCH:-stage}"
COMPOSE_PROJECT="${COMPOSE_PROJECT:-herdcommand}"

if [[ "${EUID}" -eq 0 ]]; then
  APP_USER="${SUDO_USER:-ec2-user}"
else
  APP_USER="${USER}"
fi
APP_HOME="$(getent passwd "${APP_USER}" | cut -d: -f6)"
APP_HOME="${APP_HOME:-/home/${APP_USER}}"
HERDCOMMAND_DEPLOY_KEY="${HERDCOMMAND_DEPLOY_KEY:-${APP_HOME}/.ssh/herdcommand_deploy}"

log() { printf '%s\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

need_sudo() {
  if [[ "${EUID}" -eq 0 ]]; then
    "$@"
  else
    sudo "$@"
  fi
}

as_app_user() {
  if [[ "$(id -un)" == "${APP_USER}" ]]; then
    "$@"
  else
    need_sudo -u "${APP_USER}" -H env GIT_SSH_COMMAND="${GIT_SSH_COMMAND:-}" "$@"
  fi
}

upsert_env() {
  local file="$1" key="$2" value="$3"
  python3 - "$file" "$key" "$value" <<'PY'
import pathlib, sys
path = pathlib.Path(sys.argv[1])
key, value = sys.argv[2], sys.argv[3]
text = path.read_text(encoding="utf-8") if path.exists() else ""
lines = text.splitlines()
out = []
found = False
for line in lines:
    if line.startswith(key + "=") or line.startswith("export " + key + "="):
        out.append(f"{key}={value}")
        found = True
    else:
        out.append(line)
if not found:
    if out and out[-1] != "":
        out.append("")
    out.append(f"{key}={value}")
path.write_text("\n".join(out) + "\n", encoding="utf-8")
PY
}

read_env() {
  local file="$1" key="$2"
  python3 - "$file" "$key" <<'PY'
import pathlib, sys
path = pathlib.Path(sys.argv[1])
key = sys.argv[2]
if not path.exists():
    raise SystemExit(0)
for line in path.read_text(encoding="utf-8").splitlines():
    if line.startswith(key + "="):
        print(line.split("=", 1)[1], end="")
        break
PY
}

public_ipv4() {
  local token ip
  token="$(curl -fsS -X PUT "http://169.254.169.254/latest/api/token" \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 21600" || true)"
  if [[ -n "${token}" ]]; then
    ip="$(curl -fsS -H "X-aws-ec2-metadata-token: ${token}" \
      http://169.254.169.254/latest/meta-data/public-ipv4 || true)"
    if [[ -z "${ip}" ]]; then
      ip="$(curl -fsS -H "X-aws-ec2-metadata-token: ${token}" \
        http://169.254.169.254/latest/meta-data/local-ipv4 || true)"
    fi
  fi
  if [[ -z "${ip}" ]]; then
    ip="$(curl -fsS http://169.254.169.254/latest/meta-data/public-ipv4 || true)"
  fi
  [[ -n "${ip}" ]] || die "could not read instance IPv4 from IMDS"
  printf '%s' "${ip}"
}

log "Installing Git and Docker (Amazon Linux 2023)…"
need_sudo dnf install -y git docker docker-compose-plugin
need_sudo systemctl enable --now docker
if ! id -nG "${APP_USER}" | grep -qw docker; then
  need_sudo usermod -aG docker "${APP_USER}"
  log "Added ${APP_USER} to the docker group. Log out and back in once for unprivileged docker."
fi

[[ -f "${HERDCOMMAND_DEPLOY_KEY}" ]] || die "deploy private key missing: ${HERDCOMMAND_DEPLOY_KEY}"
need_sudo chmod 600 "${HERDCOMMAND_DEPLOY_KEY}"
need_sudo chown "${APP_USER}:${APP_USER}" "${HERDCOMMAND_DEPLOY_KEY}"

export GIT_SSH_COMMAND="ssh -i ${HERDCOMMAND_DEPLOY_KEY} -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new"

if [[ -d "${HERDCOMMAND_HOME}/.git" ]]; then
  log "Updating ${HERDCOMMAND_HOME} (${HERDCOMMAND_BRANCH})…"
  as_app_user git -C "${HERDCOMMAND_HOME}" fetch origin "${HERDCOMMAND_BRANCH}"
  as_app_user git -C "${HERDCOMMAND_HOME}" checkout "${HERDCOMMAND_BRANCH}"
  as_app_user git -C "${HERDCOMMAND_HOME}" pull --ff-only origin "${HERDCOMMAND_BRANCH}"
else
  log "Cloning ${HERDCOMMAND_GIT_SSH} (${HERDCOMMAND_BRANCH}) into ${HERDCOMMAND_HOME}…"
  need_sudo rm -rf "${HERDCOMMAND_HOME}"
  need_sudo mkdir -p "${HERDCOMMAND_HOME}"
  need_sudo chown "${APP_USER}:${APP_USER}" "${HERDCOMMAND_HOME}"
  as_app_user git clone --branch "${HERDCOMMAND_BRANCH}" "${HERDCOMMAND_GIT_SSH}" "${HERDCOMMAND_HOME}"
fi

need_sudo chown -R "${APP_USER}:${APP_USER}" "${HERDCOMMAND_HOME}"

PUBLIC_IP="$(public_ipv4)"
PUBLIC_ORIGIN="http://${PUBLIC_IP}"
ENV_FILE="${HERDCOMMAND_HOME}/.env"
EXAMPLE="${HERDCOMMAND_HOME}/infra/ec2/env.example"
[[ -f "${EXAMPLE}" ]] || die "missing ${EXAMPLE} — is ${HERDCOMMAND_BRANCH} checked out?"

if [[ ! -f "${ENV_FILE}" ]]; then
  as_app_user cp "${EXAMPLE}" "${ENV_FILE}"
fi
upsert_env "${ENV_FILE}" PUBLIC_ORIGIN "${PUBLIC_ORIGIN}"

AUTH_SECRET="$(read_env "${ENV_FILE}" AUTH_SECRET)"
if [[ -z "${AUTH_SECRET}" || "${AUTH_SECRET}" == change-me-to-a-32-char-random-string!! ]]; then
  AUTH_SECRET="$(python3 -c 'import secrets; print(secrets.token_urlsafe(32))')"
  upsert_env "${ENV_FILE}" AUTH_SECRET "${AUTH_SECRET}"
fi
need_sudo chmod 600 "${ENV_FILE}"
need_sudo chown "${APP_USER}:${APP_USER}" "${ENV_FILE}"

log "PUBLIC_ORIGIN=${PUBLIC_ORIGIN}"
log "Starting Compose (${COMPOSE_PROJECT})…"
need_sudo docker compose \
  -p "${COMPOSE_PROJECT}" \
  -f "${HERDCOMMAND_HOME}/docker-compose.test.yml" \
  --env-file "${ENV_FILE}" \
  --project-directory "${HERDCOMMAND_HOME}" \
  up --build -d

log "Waiting for Keycloak…"
ready=0
for _ in $(seq 1 90); do
  if curl -fsS "http://127.0.0.1:9090/realms/herdcommand/.well-known/openid-configuration" >/dev/null; then
    ready=1
    break
  fi
  sleep 5
done
[[ "${ready}" -eq 1 ]] || die "Keycloak did not become ready on :9090"

ADMIN_USER="$(read_env "${ENV_FILE}" KEYCLOAK_ADMIN)"
ADMIN_PASS="$(read_env "${ENV_FILE}" KEYCLOAK_ADMIN_PASSWORD)"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"

export PUBLIC_ORIGIN ADMIN_USER ADMIN_PASS ENV_FILE
KC_OUT="$(python3 <<'PY'
import json, os, pathlib, urllib.parse, urllib.request

origin = os.environ["PUBLIC_ORIGIN"]
env_file = pathlib.Path(os.environ["ENV_FILE"])
admin_user = os.environ["ADMIN_USER"]
admin_pass = os.environ["ADMIN_PASS"]
base = "http://127.0.0.1:9090"

def request(method, url, data=None, token=None, form=False):
    headers = {}
    body = None
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if data is not None:
        if form:
            body = urllib.parse.urlencode(data).encode()
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        else:
            body = json.dumps(data).encode()
            headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=30) as resp:
        raw = resp.read()
        return json.loads(raw.decode()) if raw else None

token = request(
    "POST",
    f"{base}/realms/master/protocol/openid-connect/token",
    {
        "grant_type": "password",
        "client_id": "admin-cli",
        "username": admin_user,
        "password": admin_pass,
    },
    form=True,
)["access_token"]

clients = request(
    "GET",
    f"{base}/admin/realms/herdcommand/clients?clientId=herdcommand",
    token=token,
)
client = clients[0]
cid = client["id"]
redirect = f"{origin}:3000/*"
web = f"{origin}:3000"
uris = list(client.get("redirectUris") or [])
if redirect not in uris:
    uris.append(redirect)
origins = list(client.get("webOrigins") or [])
if web not in origins:
    origins.append(web)
attrs = dict(client.get("attributes") or {})
logout = f"{origin}:3000/*"
post = attrs.get("post.logout.redirect.uris") or ""
parts = [p for p in post.split("##") if p]
if logout not in parts:
    parts.append(logout)
attrs["post.logout.redirect.uris"] = "##".join(parts)
client["redirectUris"] = uris
client["webOrigins"] = origins
client["attributes"] = attrs
request("PUT", f"{base}/admin/realms/herdcommand/clients/{cid}", client, token=token)

admins = request(
    "GET",
    f"{base}/admin/realms/herdcommand/clients?clientId=herdcommand-admin",
    token=token,
)
aid = admins[0]["id"]
secret = request(
    "GET",
    f"{base}/admin/realms/herdcommand/clients/{aid}/client-secret",
    token=token,
)["value"]

text = env_file.read_text(encoding="utf-8")
lines = []
found = False
changed = False
for line in text.splitlines():
    if line.startswith("KEYCLOAK_ADMIN_CLIENT_SECRET="):
        prev = line.split("=", 1)[1]
        lines.append(f"KEYCLOAK_ADMIN_CLIENT_SECRET={secret}")
        found = True
        changed = prev != secret
    else:
        lines.append(line)
if not found:
    lines.append(f"KEYCLOAK_ADMIN_CLIENT_SECRET={secret}")
    changed = True
env_file.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("SECRET_CHANGED=1" if changed else "SECRET_CHANGED=0")
print(f"Added Keycloak redirect URI {redirect}")
PY
)"
printf '%s\n' "${KC_OUT}"

if printf '%s\n' "${KC_OUT}" | grep -q '^SECRET_CHANGED=1$'; then
  log "Recreating API with Keycloak admin client secret…"
  need_sudo docker compose \
    -p "${COMPOSE_PROJECT}" \
    -f "${HERDCOMMAND_HOME}/docker-compose.test.yml" \
    --env-file "${ENV_FILE}" \
    --project-directory "${HERDCOMMAND_HOME}" \
    up -d --force-recreate --no-deps api
fi

log "Stack is up."
log "  Web:      ${PUBLIC_ORIGIN}:3000"
log "  API:      ${PUBLIC_ORIGIN}:8080"
log "  Keycloak: ${PUBLIC_ORIGIN}:9090  (admin console, realm herdcommand)"
log "Sign in as farmowner / Farm123!"
