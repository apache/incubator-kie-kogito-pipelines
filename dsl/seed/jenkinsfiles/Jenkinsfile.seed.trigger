import org.jenkinsci.plugins.workflow.libs.Library

@Library('jenkins-pipeline-shared-libraries')_

pipeline {
    agent {
        label "${env.AGENT_LABEL}"
    }

    options {
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
    }

    stages {
        stage('Trigger seed job if needed') {
            steps {
                script {
                    checkout(githubscm.resolveRepository("${REPO_NAME}", "${GIT_AUTHOR_NAME}", "${GIT_BRANCH_NAME}", false, "${GIT_AUTHOR_CREDS_ID}"))

                    List listenToModifiedPaths = readJSON(text: env.LISTEN_TO_MODIFIED_PATHS)
                    echo "Listening to paths: ${listenToModifiedPaths}"

                    if (params.FORCE_REBUILD ?: arePathsModified(listenToModifiedPaths)) {
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

boolean arePathsModified(List<String> paths) {
    def modified = false
    def changeLogSets = currentBuild.changeSets
    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            def files = new ArrayList(entry.affectedFiles)
            for (int k = 0; k < files.size(); k++) {
                def file = files[k]

                if (paths.any { file.path.startsWith(it) }) {
                    modified = true
                }
            }
        }
    }
    return modified
}