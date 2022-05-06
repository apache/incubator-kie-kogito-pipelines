SEED_FOLDER = 'dsl/seed'

util = null

def generate() {
    if ("${JOB_TYPE}" == 'TRIGGER') {
        triggerSeedJob()
    } else {
        generateJobs()
    }
}

def triggerSeedJob() {
    node('kie-rhel8-priority') {
        checkout scm

        dir("${SEED_REPO}") {
            checkout(githubscm.resolveRepository("${SEED_REPO}", "${SEED_AUTHOR}", "${SEED_BRANCH}", false))
            echo 'This is the generate repo seed jobs'

            util = load "${SEED_FOLDER}/jobs/scripts/util.groovy"
        }

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

def generateJobs() {
    node('kie-rhel8-priority') {
        checkout scm

        dir("${SEED_REPO}") {
            checkout(githubscm.resolveRepository("${SEED_REPO}", "${SEED_AUTHOR}", "${SEED_BRANCH}", false))
            echo 'This is the generate repo seed jobs'

            util = load "${SEED_FOLDER}/jobs/scripts/util.groovy"
        }

        stage('Prepare jobs') {
            def repoConfig = getRepoConfig()
            String jobsFilePath = "${repoConfig.git.jenkins_config_path}/dsl/jobs.groovy"
            if (repoConfig.disabled || repoConfig.disable.branch) {
                jobsFilePath = "${SEED_REPO}/${SEED_FOLDER}/jobs/empty_job_dsl.groovy"
            }
            echo "Copying DSL jobs file ${jobsFilePath}"
            sh "cp ${jobsFilePath} ${SEED_REPO}/${SEED_FOLDER}/jobs/jobs.groovy"
        }

        stage('Test jobs') {
            if (!params.SKIP_TESTS) {
                dir("${SEED_REPO}/${SEED_FOLDER}") {
                    try {
                        sh './gradlew clean test'
                    } finally {
                        junit 'build/test-results/**/*.xml'
                        archiveArtifacts 'build/reports/**'
                    }
                }
            } else {
                echo 'Tests are skipped'
            }
        }

        stage('Generate jobs') {
            def envProps = getRepoConfigAsEnvProperties()
            def repoConfig = getRepoConfig()
            envProps.put('GIT_MAIN_BRANCH', "${GIT_MAIN_BRANCH}")
            envProps.put('REPO_NAME', "${REPO_NAME}")

            // Add other repos `jenkins_config_path` var (useful if multijob PR checks for example)
            getAllBranchRepos().each { repoName ->
                String key = util.generateEnvKey(repoName, 'jenkins_config_path')
                envProps.put(key, getRepoConfig(repoName).git.jenkins_config_path)
            }

            if (util.isDebug()) {
                println '[DEBUG] Modified environment properties:'
                envProps.each {
                    println "[DEBUG] ${it.key} = ${it.value}"
                }
            }

            dir("${SEED_REPO}/${SEED_FOLDER}") {
                println "[INFO] Generate jobs for branch ${GENERATION_BRANCH} and repo ${REPO_NAME}."
                println "[INFO] Additional parameters: ${envProps}."
                jobDsl targets: 'jobs/jobs.groovy',
                    sandbox: false,
                    ignoreExisting: false,
                    ignoreMissingFiles: false,
                    removedJobAction: repoConfig.disable.branch || repoConfig.disabled ? 'DISABLE' : 'DELETE',
                    removedViewAction: 'DELETE',
                    //removedConfigFilesAction: 'IGNORE',
                    lookupStrategy: 'SEED_JOB',
                    additionalClasspath: 'src/main/groovy',
                    additionalParameters : envProps
            }
        }
    }
}

def getRepoConfig(String repoName = "${REPO_NAME}") {
    def cfg = util.getRepoConfig(repoName, "${GENERATION_BRANCH}", "${SEED_REPO}")

    String author = "${GIT_AUTHOR}"
    String branch = "${GIT_BRANCH}"

    // Override with data from environment
    cfg.git.branch = branch
    cfg.git.author.name = author

    if (util.isDebug()) {
        println '[DEBUG] Modified repo config:'
        println "[DEBUG] ${cfg}"
    }

    return cfg
}

def getRepoConfigAsEnvProperties(String repoName = "${REPO_NAME}") {
    return util.convertConfigToEnvProperties(getRepoConfig(repoName))
}

List getAllBranchRepos() {
    return util.readBranchConfig("${SEED_REPO}").repositories.collect { it.name }
}

return this
