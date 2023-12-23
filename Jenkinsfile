def gv
def matcher
def nextIncrementVersion
def major
def minor
def patch

void setBuildStatus(String message, String state) {
  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: "https://github.com/my-org/my-repo"],
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: "ci/jenkins/build-status"],
      errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}

pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JDK'
    }

    environment {
       IMAGE_NAME = 'fyp-grading-evaluation-service'
       BRANCH_NAME = 'evaluation-service-pipeline'
       GitHub_REPO = 'fyp-grading-evaluation-service'
       GitHub_USR = 'FirasKhalife'
       VERSION = readMavenPom().getVersion()
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out SCM'
                checkout scm
            }
        }

        stage("init"){
            steps{
                script{
                    gv = load("semver.groovy")
                    echo "semver.groovy loaded, ${gv}"
                }
            }
        }

        stage('Build') {
            steps {
                echo 'Building'
                bat 'mvn clean install'
            }
        }

        stage('Test') {
            steps {
                echo 'Testing'
                bat 'mvn test'
            }
            post{
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage("increment version"){
            steps{
                script{
                    env.VERSION = gv.version()
                    echo "VERSION: ${env.VERSION}"
                }
            }
        }

        stage('Build and Push Docker Image') {
            steps {
                echo 'Building and pushing Docker image'
                expression {
                    env.BRANCH_NAME == 'add_evaluation_pipeline'
                }
            }
            post {
                success {
                    echo 'Build and push Docker image successful'

                    withCredentials([usernamePassword(credentialsId: 'Docker_Hub_Credentials2',
                    usernameVariable: 'dockerHubCredentials2_USR',
                    passwordVariable: 'dockerHubCredentials2_PSW')]) {

                        bat 'docker login -u %dockerHubCredentials2_USR% -p %dockerHubCredentials2_PSW%'
                        echo 'Logged in to Docker Hub'

                        bat "docker build -t %dockerHubCredentials2_USR%/${env.IMAGE_NAME} ."
                        echo "Built Docker image %dockerHubCredentials2_USR%/${env.IMAGE_NAME}"

                        // Tag the image with 'latest' and version
                        bat "docker tag %dockerHubCredentials2_USR%/${env.IMAGE_NAME} %dockerHubCredentials2_USR%/${env.IMAGE_NAME}:latest"
                        echo "Tagged Docker image with latest"

                        bat "docker tag %dockerHubCredentials2_USR%/${env.IMAGE_NAME} %dockerHubCredentials2_USR%/${env.IMAGE_NAME}:${env.VERSION}"
                        echo "Tagged Docker image with version ${env.VERSION}"

                        // Push the tags to the Docker Hub
                        bat "docker push %dockerHubCredentials2_USR%/${env.IMAGE_NAME}:latest"
                        echo "Pushed Docker image with latest"

                        bat "docker push %dockerHubCredentials2_USR%/${env.IMAGE_NAME}:${env.VERSION}"
                        echo "Pushed Docker image with version ${env.VERSION}"
                    }
                }
            }
        }

    }
    post {
        success {
            setBuildStatus("Build succeeded", "SUCCESS");
            emailext (
                subject: "Build SUCCESS - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "The build of ${env.JOB_NAME} was successful! Build Number: ${env.BUILD_NUMBER}",
                from: "gaellesaid65@gmail.com",
                to: "gaellesaid5@gmail.com",
                replyTo: "gaellesaid65@gmail.com"
            )
        }
        failure {
            setBuildStatus("Build failed", "FAILURE");
            emailext (
                attachLog: true,
                subject: "Build FAILURE - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "The build of ${env.JOB_NAME} failed. Please find the attached logs for details. Build Number: ${env.BUILD_NUMBER}\nPlease go to ${env.BUILD_URL}/consoleText for more details.",
                from: "gaellesaid65@gmail.com",
                to: "gaellesaid5@gmail.com",
                replyTo: "gaellesaid65@gmail.com",
                attachmentsPattern: 'build-logs.zip'
            )
        }
    }
}
