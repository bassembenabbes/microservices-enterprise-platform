pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'your-registry.com'
        KUBECONFIG = credentials('kubeconfig')
        HELM_CHART_PATH = 'helm/ecommerce'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test Backend') {
            parallel {
                stage('Chatbot Service') {
                    steps {
                        dir('backend/chatbot-service') {
                            sh 'mvn clean test'
                        }
                    }
                }
                stage('Order Service') {
                    steps {
                        dir('backend/order-service') {
                            sh 'mvn clean test'
                        }
                    }
                }
            }
        }

        stage('Test Frontend') {
            steps {
                dir('frontend/react-app') {
                    sh 'npm ci'
                    sh 'npm test -- --coverage --watchAll=false'
                }
            }
        }

        stage('Build Images') {
            steps {
                script {
                    def services = ['user-service', 'product-service', 'order-service', 'chatbot-service', 'api-gateway', 'frontend', 'notification-service']
                    def builds = [:]

                    services.each { service ->
                        builds[service] = {
                            sh "docker build -t ${DOCKER_REGISTRY}/ecommerce-${service}:${BUILD_NUMBER} backend/${service}"
                            sh "docker push ${DOCKER_REGISTRY}/ecommerce-${service}:${BUILD_NUMBER}"
                        }
                    }

                    parallel builds
                }
            }
        }

        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    sh 'helm dependency update ${HELM_CHART_PATH}'
                    sh '''
                        helm upgrade --install ecommerce-staging ${HELM_CHART_PATH} \
                            --namespace ecommerce-staging \
                            --create-namespace \
                            --set global.imageRegistry=${DOCKER_REGISTRY} \
                            --set global.imageTag=${BUILD_NUMBER} \
                            --wait
                    '''
                }
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Deploy to Production?', ok: 'Deploy'
                script {
                    sh 'helm dependency update ${HELM_CHART_PATH}'
                    sh '''
                        helm upgrade --install ecommerce-prod ${HELM_CHART_PATH} \
                            --namespace ecommerce-prod \
                            --create-namespace \
                            --set global.imageRegistry=${DOCKER_REGISTRY} \
                            --set global.imageTag=${BUILD_NUMBER} \
                            --wait
                    '''
                }
            }
        }

        stage('Integration Tests') {
            steps {
                script {
                    // Run integration tests against deployed environment
                    sh '''
                        echo "Running integration tests..."
                        # Add your integration test commands here
                        # Example: ./run-integration-tests.sh
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'docker system prune -f'
            junit '**/target/surefire-reports/*.xml'
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'frontend/react-app/coverage',
                reportFiles: 'index.html',
                reportName: 'Frontend Coverage Report'
            ])
        }
        success {
            slackSend(
                channel: '#deployments',
                color: 'good',
                message: "Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} succeeded!"
            )
        }
        failure {
            slackSend(
                channel: '#deployments',
                color: 'danger',
                message: "Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} failed!"
            )
        }
    }
}
