pipeline {
    agent {
        kubernetes {
            yaml '''
                apiVersion: v1
                kind: Pod
                metadata:
                  labels:
                    app: jenkins-agent
                spec:
                containers:
                - name: maven
                  image: "maven:3.9.6"
                  command: ["cat"]
                  tty: true
                  volumeMounts:
                  - mountPath: "/root/.m2/repository"
                    name: cache
                - name: git
                  image: "bitnami/git:latest"
                  command: ["cat"]
                  tty: true
                - name: sonarcli
                  image: "sonarsource/sonar-scanner-cli:latest"
                  command: ["cat"]
                  tty: true
                volumes:
                - name: cache
                  persistentVolumeClaim:
                    claimName: maven-cache
            '''
        }
    }
    stages {
        stage('Clone repo') {
            when { expression { true } }
            steps {
                container('git') {
                    git branch: 'main', url: 'https://github.com/ganeshkumar14/spring-petclinic.git'
                }
            }
        }
        stage('Build project') {
            when { expression { true } }
            steps {
                container('maven') {
                    sh '''
                        mvn -Dmaven.test.failure.ignore=true clean package
                    '''
                }
            }
            post {
                success {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Sonar scan') {
            when { expression { true } }
            steps{
                container('sonarcli') {
                    withSonarQubeEnv(credentialsId: 'sonar', installationName: 'sonarserver') { 
                        sh '''/opt/sonar-scanner/bin/sonar-scanner \
                        -Dsonar.projectKey=petclinic \
                        -Dsonar.projectName=petclinic \
                        -Dsonar.projectVersion=1.0 \
                        -Dsonar.sources=src/main \
                        -Dsonar.tests=src/test \
                        -Dsonar.java.binaries=target/classes  \
                        -Dsonar.language=java \
                        -Dsonar.sourceEncoding=UTF-8 \
                        -Dsonar.java.libraries=target/classes
                        '''
                    }
                }
            }
        }
    }
}