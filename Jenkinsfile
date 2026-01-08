pipeline {
    agent any
    
    environment {
        SONAR_HOST_URL = 'http://sonarqube:9000'
        DOCKER_COMPOSE_FILE = 'docker-compose.yml'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo 'Source code checked out successfully'
            }
        }
        
        stage('Build & Test') {
            parallel {
                stage('Backend - Java') {
                    steps {
                        dir('resumeanalyzer') {
                            sh '''
                                echo "Building Spring Boot Backend..."
                                ./mvnw clean verify -B
                            '''
                        }
                    }
                    post {
                        always {
                            // Publish JUnit test results
                            junit allowEmptyResults: true, testResults: 'resumeanalyzer/target/surefire-reports/*.xml'
                        }
                    }
                }
                
                stage('NLP Service - Python') {
                    steps {
                        echo 'Skipping NLP tests (Python not installed in Jenkins container)'
                        echo 'NLP tests should be run in Docker container or locally'
                    }
                }
                
                stage('Frontend - React') {
                    steps {
                        echo 'Skipping Frontend build (Node.js not installed in Jenkins container)'
                        echo 'Frontend build should be run in Docker container or locally'
                    }
                }
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    dir('resumeanalyzer') {
                        sh '''
                            ./mvnw sonar:sonar \
                                -Dsonar.projectKey=ai-resume-analyzer-backend \
                                -Dsonar.projectName="AI Resume Analyzer - Backend" \
                                -Dsonar.java.binaries=target/classes \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Build Docker Images') {
            steps {
                sh '''
                    echo "Building Docker images..."
                    docker compose build app-backend nlp-service app-frontend
                '''
            }
        }
        
        stage('Deploy') {
            steps {
                sh '''
                    echo "Deploying application with Docker Compose..."
                    docker compose down || true
                    docker compose up -d postgres minio zookeeper kafka ollama
                    sleep 10
                    docker compose up -d app-backend nlp-service app-frontend kafka-ui
                    echo "Deployment complete!"
                '''
            }
        }
    }
    
    post {
        always {
            echo 'Pipeline execution completed'
            cleanWs()
        }
        success {
            echo 'Pipeline succeeded! Application deployed.'
        }
        failure {
            echo 'Pipeline failed! Check the logs for details.'
        }
    }
}
