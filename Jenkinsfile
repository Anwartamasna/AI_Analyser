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
                            junit 'resumeanalyzer/target/surefire-reports/*.xml'
                            
                            // Publish JaCoCo coverage report
                            jacoco(
                                execPattern: 'resumeanalyzer/target/jacoco.exec',
                                classPattern: 'resumeanalyzer/target/classes',
                                sourcePattern: 'resumeanalyzer/src/main/java',
                                exclusionPattern: 'resumeanalyzer/src/test*'
                            )
                        }
                    }
                }
                
                stage('NLP Service - Python') {
                    steps {
                        dir('nlp-service') {
                            sh '''
                                echo "Running Python tests..."
                                pip install pytest pytest-cov --quiet
                                python -m pytest test_resume_processor.py -v \
                                    --cov=. \
                                    --cov-report=xml:coverage.xml \
                                    --cov-report=html:htmlcov \
                                    --junitxml=test-results.xml || true
                            '''
                        }
                    }
                    post {
                        always {
                            junit 'nlp-service/test-results.xml'
                        }
                    }
                }
                
                stage('Frontend - React') {
                    steps {
                        dir('airesumeanalyser') {
                            sh '''
                                echo "Building React Frontend..."
                                npm ci
                                npm run lint || true
                                npm run build
                            '''
                        }
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
