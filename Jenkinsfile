@Library('jenkins-shared-library') _

pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKER_IMAGE         = "nithi1230/devsecops-app"
        DOCKER_TAG           = "${BUILD_NUMBER}"
        SONARQUBE_URL        = "http://sonarqube-server:9000"
        SNYK_TOKEN           = credentials('snyk-api-token')
        K8S_NAMESPACE        = "devsecops"
    }

    tools {
        maven 'Maven-3.9'
    }

    stages {

        // ──────────────────────────────────────────────
        // Stage 1: Slack – Pipeline Started
        // ──────────────────────────────────────────────
        stage('Notify Start') {
            steps {
                slackNotification('STARTED')
            }
        }

        // ──────────────────────────────────────────────
        // Stage 2: Checkout Source Code
        // ──────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // ──────────────────────────────────────────────
        // Stage 3: Maven Build
        // ──────────────────────────────────────────────
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests -B'
            }
        }

        // ──────────────────────────────────────────────
        // Stage 4: Unit Tests
        // ──────────────────────────────────────────────
        stage('Unit Tests') {
            steps {
                sh 'mvn test -B'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                         testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        // ──────────────────────────────────────────────
        // Stage 5: SonarQube Static Code Analysis
        // ──────────────────────────────────────────────
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube-Server') {
                    sh '''
                        mvn sonar:sonar \
                            -Dsonar.projectKey=devsecops-app \
                            -Dsonar.projectName="DevSecOps Task Manager" \
                            -Dsonar.host.url=${SONARQUBE_URL} \
                            -B
                    '''
                }
            }
        }

        // ──────────────────────────────────────────────
        // Stage 6: SonarQube Quality Gate
        // ──────────────────────────────────────────────
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ──────────────────────────────────────────────
        // Stage 7: Snyk Dependency Scan
        // ──────────────────────────────────────────────
        stage('Snyk Security Scan') {
            steps {
                sh '''
                    snyk auth ${SNYK_TOKEN}
                    snyk test --severity-threshold=high || true
                    snyk monitor
                '''
            }
        }

        // ──────────────────────────────────────────────
        // Stage 8: Docker Image Build
        // ──────────────────────────────────────────────
        stage('Docker Build') {
            steps {
                sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
            }
        }

        // ──────────────────────────────────────────────
        // Stage 9: Trivy – Container Image Scan
        // ──────────────────────────────────────────────
        stage('Trivy Image Scan') {
            steps {
                sh """
                    trivy image \
                        --severity HIGH,CRITICAL \
                        --format table \
                        --exit-code 0 \
                        --no-progress \
                        ${DOCKER_IMAGE}:${DOCKER_TAG}
                """
            }
        }

        // ──────────────────────────────────────────────
        // Stage 10: Push Image to DockerHub
        // ──────────────────────────────────────────────
        stage('Push to DockerHub') {
            steps {
                sh '''
                    echo "${DOCKERHUB_CREDENTIALS_PSW}" | docker login -u "${DOCKERHUB_CREDENTIALS_USR}" --password-stdin
                    docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    docker push ${DOCKER_IMAGE}:latest
                '''
            }
        }

        // ──────────────────────────────────────────────
        // Stage 11: Deploy to Kubernetes
        // ──────────────────────────────────────────────
        stage('Deploy to Kubernetes') {
            steps {
                sh """
                    # Create namespace if it doesn't exist
                    kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                    # Apply Kubernetes manifests
                    kubectl apply -f k8s/namespace.yaml
                    kubectl apply -f k8s/configmap.yaml
                    kubectl apply -f k8s/secret.yaml
                    kubectl apply -f k8s/mongodb/
                    kubectl apply -f k8s/deployment.yaml
                    kubectl apply -f k8s/service.yaml

                    # Update deployment image
                    kubectl set image deployment/devsecops-app \
                        devsecops-app=${DOCKER_IMAGE}:${DOCKER_TAG} \
                        -n ${K8S_NAMESPACE}

                    # Wait for rollout
                    kubectl rollout status deployment/devsecops-app \
                        -n ${K8S_NAMESPACE} --timeout=300s
                """
            }
        }

        // ──────────────────────────────────────────────
        // Stage 12: Trivy – Kubernetes Config Scan
        // ──────────────────────────────────────────────
        stage('Trivy K8s Scan') {
            steps {
                sh '''
                    trivy config \
                        --severity HIGH,CRITICAL \
                        --format table \
                        --exit-code 0 \
                        k8s/
                '''
            }
        }
    }

    // ──────────────────────────────────────────────
    // Post Actions – Slack Notifications
    // ──────────────────────────────────────────────
    post {
        success {
            slackNotification('SUCCESS')
        }
        failure {
            slackNotification('FAILURE')
        }
        unstable {
            slackNotification('UNSTABLE')
        }
        always {
            cleanWs()
            sh "docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} || true"
            sh "docker rmi ${DOCKER_IMAGE}:latest || true"
        }
    }
}
