#!/bin/bash
set -e

DEEPSEEK_API_KEY="${DEEPSEEK_API_KEY:-sk-faa8c95f421e4865a90da24705cfe5e4}"
DEPLOY_DIR="/data/deploy"

echo "=== AI Agent Contact Deploy Script ==="

# 1. Install Docker if missing
if ! command -v docker &>/dev/null; then
    echo "[1/6] Installing Docker..."
    curl -fsSL https://get.docker.com | sh
    systemctl start docker
    systemctl enable docker
else
    echo "[1/6] Docker already installed: $(docker --version)"
fi

# 2. Install docker compose plugin if missing
if ! docker compose version &>/dev/null; then
    echo "[2/6] Installing docker compose plugin..."
    mkdir -p /usr/local/lib/docker/cli-plugins
    curl -SL https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64 \
        -o /usr/local/lib/docker/cli-plugins/docker-compose
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
else
    echo "[2/6] Docker Compose already installed: $(docker compose version)"
fi

# 3. Install git if missing
if ! command -v git &>/dev/null; then
    echo "Installing git..."
    apt-get install -y git 2>/dev/null || yum install -y git 2>/dev/null
fi

# 4. Clone / update repos
echo "[3/6] Cloning/updating repositories..."
mkdir -p "$DEPLOY_DIR"
cd "$DEPLOY_DIR"

if [ -d "agent-contact-server" ]; then
    cd agent-contact-server && git pull && cd ..
else
    git clone https://github.com/BlueBloodFire/agent-contact-server.git
fi

if [ -d "agent-contact-client" ]; then
    cd agent-contact-client && git pull && cd ..
else
    git clone https://github.com/BlueBloodFire/agent-contact-client.git
fi

# 5. Create .env
echo "[4/6] Writing .env..."
cat > "$DEPLOY_DIR/agent-contact-server/.env" <<EOF
DEEPSEEK_API_KEY=$DEEPSEEK_API_KEY
MYSQL_ROOT_PASSWORD=wangjin@test
EOF
echo ".env written."

# 6. Build and start
echo "[5/6] Building and starting containers (first build ~5-10 min)..."
cd "$DEPLOY_DIR/agent-contact-server"
docker compose up -d --build

# 7. Status
echo "[6/6] Container status:"
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

SERVER_IP=$(curl -s ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')
echo ""
echo "=== Deployment complete ==="
echo "Frontend : http://$SERVER_IP"
echo "Backend  : http://$SERVER_IP:8092"
