/////////////////////////////////////////////////////////////////////////////////
// This Jenkinsfile generate the respective Branch seed jobs
/////////////////////////////////////////////////////////////////////////////////

import org.jenkinsci.plugins.workflow.libs.Library

@Library('jenkins-pipeline-shared-libraries')_

util = null

// Configuration of the pipeline is done via the `config/main.yaml` file
pipeline {
    agent any

    options {
        timestamps()
    }

    // parameters {
    // See ./jobs/seed_job.groovy
    // }

    stages {
        stage('Trigger seed job if needed') {
            steps {
                script {
                    checkout scm
                    echo "${STAGE_NAME}"
                }
            }
            post {
                always {
                    cleanWs()
                }
            }
        }
    }
}
