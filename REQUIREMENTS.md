# DevSecOps CI/CD Pipeline – Requirements & Setup Guide

## Prerequisites

### Servers (Minimum)
| Server | Purpose | Recommended Spec |
|--------|---------|-----------------|
| Jenkins Server | CI/CD orchestration | 2 vCPU, 8 GB RAM, 30 GB disk |
| SonarQube Server | Static code analysis | 2 vCPU, 4 GB RAM, 20 GB disk |
| Kubernetes Cluster | Application deployment | 2 nodes, 2 vCPU / 4 GB RAM each |

Both servers should run **Ubuntu 22.04 LTS** (or similar Debian-based OS).

For Kubernetes, you can use **Minikube**, **kubeadm**, **k3s**, or any managed Kubernetes provider.

---

## Software Requirements

### On Jenkins Server
| Tool | Version | Purpose |
|------|---------|---------|
| Java (OpenJDK) | 17 | Jenkins & Maven runtime |
| Jenkins | 2.426+ (LTS) | CI/CD pipeline engine |
| Maven | 3.9.x | Java build tool |
| Docker | 24.x+ | Container build & push |
| kubectl | 1.29+ | Kubernetes management |
| Trivy | 0.49+ | Container & K8s scanning |
| Snyk CLI | Latest | Dependency vulnerability scanning |
| Git | 2.x+ | Source control |

> Run `scripts/install-tools.sh` to install all tools automatically.

### On SonarQube Server
| Tool | Version | Purpose |
|------|---------|---------|
| Java (OpenJDK) | 17 | SonarQube runtime |
| SonarQube | 10.x (Community) | Code quality analysis |
| PostgreSQL | 15+ | SonarQube database backend |

### SonarQube Docker Quick Start
```bash
docker run -d --name sonarqube \
    -p 9000:9000 \
    -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
    sonarqube:10-community
```
Default credentials: `admin` / `admin`

---

## Account & Token Requirements

| Service | What You Need | Where to Get It |
|---------|--------------|-----------------|
| **GitHub** | Repository with this source code | github.com |
| **DockerHub** | Account + access token | hub.docker.com → Account Settings → Security |
| **Snyk** | API token | app.snyk.io → Account Settings → API Token |
| **SonarQube** | Server token | SonarQube → My Account → Security → Generate Token |
| **Slack** | Bot OAuth token + channel | api.slack.com/apps → Create App → OAuth & Permissions |

---

## Jenkins Configuration

### Required Plugins
Install via **Manage Jenkins → Plugins → Available**:

- Pipeline (workflow-aggregator)
- Git
- Maven Integration
- Docker Pipeline
- SonarQube Scanner
- Kubernetes
- Kubernetes CLI
- Slack Notification
- Credentials Binding
- Pipeline Utility Steps
- JUnit
- JaCoCo
- Blue Ocean

> Run `scripts/setup-jenkins.sh` to install Jenkins and all plugins automatically.

### Credentials to Configure
Navigate to **Manage Jenkins → Credentials → System → Global credentials**:

| Credential ID | Type | Description |
|--------------|------|-------------|
| `dockerhub-credentials` | Username with password | DockerHub username + access token |
| `snyk-api-token` | Secret text | Snyk API token |
| `slack-bot-token` | Secret text | Slack Bot OAuth token |

### SonarQube Server Configuration
1. **Manage Jenkins → System → SonarQube servers**
2. Name: `SonarQube-Server`
3. Server URL: `http://<sonarqube-ip>:9000`
4. Authentication token: (add as secret text credential)

### Global Tool Configuration
1. **Manage Jenkins → Tools**
2. Maven: Name = `Maven-3.9`, Install automatically

### Jenkins Shared Library
1. **Manage Jenkins → System → Global Pipeline Libraries**
2. Name: `jenkins-shared-library`
3. Default version: `main`
4. Retrieval method: Modern SCM → Git
5. Project Repository: URL of the repo containing `vars/slackNotification.groovy`

---

## Kubernetes Cluster Setup

You can use any Kubernetes distribution. Here are common options:

### Option A: Minikube (Local Development)
```bash
minikube start --cpus=2 --memory=4096
kubectl get nodes
```

### Option B: kubeadm (Self-hosted)
```bash
# On master node
kubeadm init --pod-network-cidr=10.244.0.0/16
mkdir -p $HOME/.kube
cp /etc/kubernetes/admin.conf $HOME/.kube/config

# Install a CNI plugin (e.g., Flannel)
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml

# On worker nodes
kubeadm join <master-ip>:6443 --token <token> --discovery-token-ca-cert-hash <hash>
```

### Option C: k3s (Lightweight)
```bash
curl -sfL https://get.k3s.io | sh -
kubectl get nodes
```

Ensure `kubectl` on the Jenkins server is configured with a valid kubeconfig that can reach the cluster.

---

## Slack Workspace Setup

1. Go to [api.slack.com/apps](https://api.slack.com/apps) and create a new app
2. Under **OAuth & Permissions**, add the scope: `chat:write`
3. Install the app to your workspace
4. Copy the **Bot User OAuth Token** (starts with `xoxb-`)
5. Create a channel named `#devsecops-pipeline`
6. Invite the bot to the channel: `/invite @YourBotName`

---

## Running the Pipeline

### Option 1: Pipeline from SCM
1. Create a **New Item** in Jenkins → **Pipeline**
2. Under Pipeline, select **Pipeline script from SCM**
3. SCM: Git
4. Repository URL: your GitHub repo URL
5. Branch: `*/main`
6. Script Path: `Jenkinsfile`
7. Save and click **Build Now**

### Option 2: GitHub Webhook (Auto-trigger)
1. In GitHub repo, go to **Settings → Webhooks → Add webhook**
2. Payload URL: `http://<jenkins-ip>:8080/github-webhook/`
3. Content type: `application/json`
4. Events: Push events
5. In Jenkins job config, check **GitHub hook trigger for GITScm polling**

---

## Project Structure

```
devsecops/
├── src/
│   ├── main/
│   │   ├── java/com/devsecops/app/
│   │   │   ├── DevSecOpsApplication.java
│   │   │   ├── controller/
│   │   │   │   └── TaskController.java
│   │   │   ├── model/
│   │   │   │   └── Task.java
│   │   │   ├── repository/
│   │   │   │   └── TaskRepository.java
│   │   │   └── service/
│   │   │       └── TaskService.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/devsecops/app/
│           └── TaskControllerTest.java
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── mongodb/
│       ├── deployment.yaml
│       └── service.yaml
├── jenkins-shared-library/
│   └── vars/
│       └── slackNotification.groovy
├── scripts/
│   ├── install-tools.sh
│   └── setup-jenkins.sh
├── trivy/
│   └── trivy.yaml
├── pom.xml
├── Dockerfile
├── .dockerignore
├── Jenkinsfile
├── sonar-project.properties
├── .snyk
├── .gitignore
└── REQUIREMENTS.md
```

---

## API Endpoints

Once deployed, the application exposes:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/tasks/health` | Health check |
| GET | `/api/v1/tasks` | List all tasks |
| GET | `/api/v1/tasks/{id}` | Get task by ID |
| POST | `/api/v1/tasks` | Create a new task |
| PUT | `/api/v1/tasks/{id}` | Update a task |
| DELETE | `/api/v1/tasks/{id}` | Delete a task |
| GET | `/api/v1/tasks/status/{status}` | Filter by status |
| GET | `/api/v1/tasks/priority/{priority}` | Filter by priority |
| GET | `/api/v1/tasks/assignee/{name}` | Filter by assignee |

### Sample Request
```bash
curl -X POST http://<node-ip>:30080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Deploy v2",
    "description": "Deploy version 2 to production",
    "priority": "HIGH",
    "assignedTo": "devops-team"
  }'
```

---

## Cleanup

```bash
# Delete Kubernetes resources
kubectl delete -f k8s/ -n devsecops

# Remove Docker images
docker rmi yourdockerhubuser/devsecops-app:latest
```
