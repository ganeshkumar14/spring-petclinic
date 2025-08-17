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
                    command:
                    - cat
                    tty: true
                    volumeMounts:
                    - mountPath: "/root/.m2/repository"
                    name: cache
                - name: git
                    image: "bitnami/git:latest"
                    command:
                    - cat
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
            steps {
                container('git') {
                    git branch: 'main', url: 'https://github.com/ganeshkumar14/spring-petclinic.git'
                }
            }
        }
        stage('Build project') {
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
    }
}