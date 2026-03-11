#!/usr/bin/env bash
# ================================================================
# setup-jenkins.sh
# Installs Jenkins on an Ubuntu/Debian server and configures
# the required plugins for the DevSecOps pipeline
# Run as root or with sudo
# ================================================================
set -euo pipefail

echo "========================================="
echo " Jenkins Installation & Setup"
echo "========================================="

# ── 1. Install Jenkins ──
echo "[1/4] Installing Jenkins..."
apt-get update -y
apt-get install -y fontconfig openjdk-17-jre

curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | \
    tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/" | \
  tee /etc/apt/sources.list.d/jenkins.list > /dev/null

apt-get update -y
apt-get install -y jenkins

systemctl enable jenkins
systemctl start jenkins

echo "[1/4] Jenkins installed and started."

# ── 2. Print initial admin password ──
echo ""
echo "[2/4] Initial Admin Password:"
echo "-------------------------------"
cat /var/lib/jenkins/secrets/initialAdminPassword
echo ""
echo "-------------------------------"

# ── 3. Install Jenkins CLI (for plugin installation) ──
echo "[3/4] Waiting for Jenkins to be ready..."
until curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/login | grep -q "200"; do
    sleep 5
done
echo "Jenkins is ready."

JENKINS_URL="http://localhost:8080"
ADMIN_PASS=$(cat /var/lib/jenkins/secrets/initialAdminPassword)

# Download Jenkins CLI
wget -q "${JENKINS_URL}/jnlpJars/jenkins-cli.jar" -O /tmp/jenkins-cli.jar

# ── 4. Install required plugins ──
echo "[4/4] Installing required Jenkins plugins..."

PLUGINS=(
    "git"
    "pipeline-stage-view"
    "workflow-aggregator"
    "docker-plugin"
    "docker-workflow"
    "maven-plugin"
    "sonar"
    "kubernetes"
    "kubernetes-cli"
    "slack"
    "credentials-binding"
    "pipeline-utility-steps"
    "junit"
    "jacoco"
    "blueocean"
)

for plugin in "${PLUGINS[@]}"; do
    echo "  Installing: ${plugin}"
    java -jar /tmp/jenkins-cli.jar -s "${JENKINS_URL}" \
        -auth "admin:${ADMIN_PASS}" install-plugin "${plugin}" || true
done

echo "Restarting Jenkins to activate plugins..."
java -jar /tmp/jenkins-cli.jar -s "${JENKINS_URL}" \
    -auth "admin:${ADMIN_PASS}" safe-restart || systemctl restart jenkins

echo ""
echo "========================================="
echo " Jenkins setup complete!"
echo "========================================="
echo ""
echo "Access Jenkins at: http://<your-server-ip>:8080"
echo ""
echo "Required credentials to configure in Jenkins:"
echo "  1. dockerhub-credentials    – DockerHub username/password"
echo "  2. snyk-api-token           – Snyk API token (secret text)"
echo "  3. slack-bot-token          – Slack Bot OAuth token"
echo "  4. SonarQube-Server         – SonarQube server (under System Config)"
echo ""
echo "Shared Library config:"
echo "  Manage Jenkins → System → Global Pipeline Libraries"
echo "  Name: jenkins-shared-library"
echo "  Source: Git repository containing vars/slackNotification.groovy"
