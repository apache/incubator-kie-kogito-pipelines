import org.jenkinsci.plugins.workflow.libs.Library

@Library('jenkins-pipeline-shared-libraries')_

util = null

// Configuration of the pipeline is done via the `config/main.yaml` file
pipeline {
    agent 'kie-rhel8 && !master'

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
                    checkout(githubscm.resolveRepository("${SEED_REPO}", "${SEED_AUTHOR}", "${SEED_BRANCH}", false))

                    util = load 'dsl/seed/jobs/scripts/util.groovy'

                    List listenToModifiedPaths = readJSON(text: env.LISTEN_TO_MODIFIED_PATHS)

                    if (params.FORCE_REBUILD ?: util.arePathsModified(listenToModifiedPaths)) {
                        echo "Build ${JOB_RELATIVE_PATH_TO_TRIGGER}"
                        build(job: "${JOB_RELATIVE_PATH_TO_TRIGGER}", parameters: [], wait: false)
                    } else {
                        echo "No force rebuild or modified paths ${listenToModifiedPaths}"
                        echo 'Nothing done'
                        currentBuild.displayName = 'No generation'
                    }
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
