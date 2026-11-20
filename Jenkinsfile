pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Smoke Tests') {
            steps {
                script {
                    // sh على Linux و bat على Windows عشان الـ agent يشتغل في الحالتين
                    def cmd = 'mvn clean test -Dsuite=testng-smoke.xml -Dheadless=true'
                    if (isUnix()) { sh cmd } else { bat cmd }
                }
            }
        }
    }
    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'test-outputs/**, src/main/java/Shots/**'
        }
    }
}
