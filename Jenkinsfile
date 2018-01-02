pipeline {
  triggers {
        githubPush()
  }
  agent { 
      label 'java'
  }
  stages {
    stage('Build') {
      steps {
        sh '''echo "Building"
java -version'''
      }
    }
  }
