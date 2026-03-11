#!/usr/bin/env bash
# ================================================================
# install-tools.sh
# Installs all required DevSecOps tools on an Ubuntu/Debian server
# Run as root or with sudo
# ================================================================
set -euo pipefail

echo "========================================="
echo " DevSecOps Tools Installation Script"
echo "========================================="

# ── 1. System update ──
echo "[1/7] Updating system packages..."
apt-get update -y && apt-get upgrade -y

# ── 2. Java 17 (OpenJDK) ──
echo "[2/7] Installing Java 17..."
apt-get install -y openjdk-17-jdk
java -version

# ── 3. Maven ──
echo "[3/7] Installing Maven..."
apt-get install -y maven
mvn -version

# ── 4. Docker ──
echo "[4/7] Installing Docker..."
apt-get install -y ca-certificates curl gnupg lsb-release
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
    gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
systemctl start docker
docker --version

# Add jenkins user to docker group
usermod -aG docker jenkins || true

# ── 5. kubectl ──
echo "[5/7] Installing kubectl..."
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
rm -f kubectl
kubectl version --client

# ── 6. Trivy ──
echo "[6/7] Installing Trivy..."
apt-get install -y wget apt-transport-https
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | \
    gpg --dearmor | tee /usr/share/keyrings/trivy.gpg > /dev/null
echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" | \
    tee /etc/apt/sources.list.d/trivy.list
apt-get update -y
apt-get install -y trivy
trivy --version

# ── 7. Snyk CLI ──
echo "[7/7] Installing Snyk CLI..."
npm install -g snyk || {
    echo "npm not found – installing Node.js first..."
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
    apt-get install -y nodejs
    npm install -g snyk
}
snyk --version

echo ""
echo "========================================="
echo " All tools installed successfully!"
echo "========================================="
