# EC2 test box (Amazon Linux 2023)

Installs Docker and Git, clones the `stage` branch with a **read-only GitHub deploy key**, then starts `docker-compose.test.yml`. Browser URLs use the instance public IP (`PUBLIC_ORIGIN`). Postgres stays on the Docker network; do not open port 5432 in the security group.

This is a private test. No HTTPS, CI, or personal `gh auth login` on the server.

## Once: deploy key (not in Git)

On your PC:

```bash
ssh-keygen -t ed25519 -f herdcommand_deploy -N "" -C "herdcommand-ec2-readonly"
```

GitHub → this repo → **Settings → Deploy keys → Add deploy key**. Paste `herdcommand_deploy.pub` only. Leave **write access** off.

Copy the **private** key to the instance (never commit it):

```bash
scp -i YOUR_AWS_KEY.pem herdcommand_deploy ec2-user@THE_IP:~/.ssh/herdcommand_deploy
ssh -i YOUR_AWS_KEY.pem ec2-user@THE_IP 'chmod 600 ~/.ssh/herdcommand_deploy'
```

## Security group

Allow from **your IP** only:

| Port | Why |
|---|---|
| 22 | SSH |
| 3000 | Web |
| 8080 | API |
| 9090 | Keycloak |

Do **not** open 5432.

## Run

SSH in as `ec2-user`. If this is a blank Amazon Linux 2023 box:

```bash
sudo dnf install -y git
GIT_SSH_COMMAND='ssh -i ~/.ssh/herdcommand_deploy -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new' \
  git clone --branch stage git@github.com:oussamabelhouchette/HerdCommand.git /opt/herdcommand
cd /opt/herdcommand
chmod +x infra/ec2/bootstrap.sh
./infra/ec2/bootstrap.sh
```

If the repo is already at `/opt/herdcommand`, just run `./infra/ec2/bootstrap.sh` again (it pulls `stage` and recreates Compose).

Override the Git URL if the repo moves:

```bash
export HERDCOMMAND_GIT_SSH='git@github.com:ORG/HerdCommand.git'
```

The script uses `sudo docker compose`, so you do not need to log out of the docker group on the first run. First `--build` takes several minutes.

Do **not** put the deploy private key in EC2 user-data (it shows up in the instance metadata). scp the key, then run the script. User-data can invoke the same script **after** the key is on disk.

## After it exits 0

- Web: `http://THE_IP:3000` — sign in as `farmowner` / `Farm123!`
- Keycloak: `http://THE_IP:9090` — console `admin` / `admin`, realm `herdcommand`

Secrets live in `/opt/herdcommand/.env` (mode 600), copied from `infra/ec2/env.example`. The script fills `PUBLIC_ORIGIN` and `KEYCLOAK_ADMIN_CLIENT_SECRET`. Re-running keeps existing secrets and only updates `PUBLIC_ORIGIN` if the IP changed.

## Local PC (same Compose)

```powershell
cd C:\Users\Lenovo\Documents\HerdCommand
copy infra\ec2\env.example .env
docker compose -p herdcommand-test -f docker-compose.test.yml up --build
```

Leave `PUBLIC_ORIGIN=http://localhost`. Stop host Next (3000), Spring (8080), and Keycloak (9090) first.
