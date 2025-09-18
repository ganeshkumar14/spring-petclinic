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
    image: maven:3.9.6
    command: ["cat"]
    tty: true
    volumeMounts:
    - mountPath: "/root/.m2/repository"
      name: cache

  - name: git
    image: bitnami/git:latest
    command: ["cat"]
    tty: true

  - name: sonarcli
    image: ganeshkumar20/sonarcli:rootca
    command: ["cat"]
    tty: true
    volumeMounts:
      - name: ca-certificates
        mountPath: /custom-ca
        readOnly: false
    env:
      - name: SONAR_SCANNER_OPTS
        value: "-Djavax.net.ssl.trustStore=/custom-ca/sonarqube-truststore.jks -Djavax.net.ssl.trustStorePassword=changeit"

  volumes:
  - name: cache
    persistentVolumeClaim:
      claimName: maven-cache
  - name: ca-certificates
    configMap:
      name: ca-certificates
    '''
}

    }
    environment{
        NEXUS_VERSION = "nexus3"
        NEXUS_PROTOCOL = "http"
        NEXUS_URL = "nexus-service.nexus.svc.rke-cluster:8081/nexus"
        NEXUS_REPOSITORY = "maven-hosted"
        NEXUS_CREDENTIAL_ID = "nexus-creds"
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
            when { expression {true} }
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
        // -Dsonar.scanner.skipCertificateCheck=true
        stage('Sonar scan') { 
            steps {
                container('sonarcli') {
                    withSonarQubeEnv(credentialsId: 'sonar', installationName: 'sonarserver') {
                        sh '''
                            /opt/sonar-scanner/bin/sonar-scanner \
                            -Dsonar.projectKey=petclinic \
                            -Dsonar.projectName=petclinic \
                            -Dsonar.projectVersion=1.0 \
                            -Dsonar.sources=src/main \
                            -Dsonar.tests=src/test \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.language=java \
                            -Dsonar.sourceEncoding=UTF-8 \
                            -Dsonar.java.libraries=target/classes \
                            -Dsonar.ssl.verify=false
                        '''
                    }
                }
            }
        }
        stage('Wait for Quality gate') {
            when { expression { true } }
            steps {
                container('sonarcli') {
                    timeout(time: 1, unit: 'HOURS') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }
        stage('Upload Maven artifact to nexus') {
            when { expression { true} }
            steps{
                container('jnlp') {
                    script {
                        pom = readMavenPom file: "pom.xml";
                        filesByGlob = findFiles(glob: "target/*.${pom.packaging}"); 
                        echo "${filesByGlob[0].name} ${filesByGlob[0].path} ${filesByGlob[0].directory} ${filesByGlob[0].length} ${filesByGlob[0].lastModified}"
                        artifactPath = filesByGlob[0].path;
                        artifactExists = fileExists artifactPath;
                        if(artifactExists) {
                            echo "*** File: ${artifactPath}, group: ${pom.groupId}, packaging: ${pom.packaging}, version ${pom.version}";
                            nexusArtifactUploader(
                                nexusVersion: NEXUS_VERSION,
                                protocol: NEXUS_PROTOCOL,
                                nexusUrl: NEXUS_URL,
                                groupId: pom.groupId,
                                version: pom.version,
                                repository: NEXUS_REPOSITORY,
                                credentialsId: NEXUS_CREDENTIAL_ID,
                                artifacts: [
                                    [artifactId: pom.artifactId,
                                    classifier: '',
                                    file: artifactPath,
                                    type: pom.packaging],

                                    [artifactId: pom.artifactId,
                                    classifier: '',
                                    file: "pom.xml",
                                    type: "pom"]
                                ]
                            );

                        } else {
                            error "*** File: ${artifactPath}, could not be found";
                        }
                    }
                }
            }
        }
    }
}