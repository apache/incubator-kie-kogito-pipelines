import org.jenkinsci.plugins.workflow.libs.Library

@Library('jenkins-pipeline-shared-libraries')_

util = null

// Configuration of the pipeline is done via the `config/main.yaml` file
pipeline {
    agent {
        label 'kie-rhel8 && !master'
    }

    options {
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
    }

    stages {
        stage('Trigger seed job if needed') {
            steps {
                script {
                    checkout(githubscm.resolveRepository("${SEED_REPO}", "${SEED_AUTHOR}", "${SEED_BRANCH}", false))

                    util = load 'dsl/seed/jobs/scripts/util.groovy'

                    List listenToModifiedPaths = readJSON(text: env.LISTEN_TO_MODIFIED_PATHS)

                    if (params.FORCE_REBUILD ?: util.arePathsModified(listenToModifiedPaths)) {
                        echo "Build ${JOB_RELATIVE_PATH_TO_TRIGGER}"
                        currentBuild.displayName = '(Re)Build jobs'
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
