SEED_FOLDER = 'dsl/seed'

util = null
paths = []

def generate() {
    // Preliminary check
    shouldRun = false
    node {
        initNode()
        stage('Preliminary check') {
            def repoConfig = getRepoConfig()
            paths.add(repoConfig.git.jenkins_config_path)
            shouldRun = params.FORCE_REBUILD ?: util.arePathsModified(paths)
        }
    }

    if (!shouldRun) {
        echo "No force rebuild or modified paths ${paths}"
        echo 'Nothing done'
        currentBuild.displayName = 'No generation'
        return
    }

    node('kie-rhel7 && kie-mem4g') {
        initNode()

        stage('Prepare jobs') {
            def repoConfig = getRepoConfig()
            String jobsFilePath = "${repoConfig.git.jenkins_config_path}/dsl/jobs.groovy"
            if (repoConfig.disable.branch) {
                jobsFilePath = "${SEED_REPO}/${SEED_FOLDER}/jobs/empty_job_dsl.groovy"
            }
            echo "Copying DSL jobs file ${jobsFilePath}"
            sh """
                cp ${jobsFilePath} ${SEED_REPO}/${SEED_FOLDER}/jobs/jobs.groovy
            """
        }

        stage('Test jobs') {
            dir("${SEED_REPO}/${SEED_FOLDER}") {
                try {
                    sh './gradlew clean test'
                } finally {
                    junit 'build/test-results/**/*.xml'
                    archiveArtifacts 'build/reports/**'
                }
            }
        }

        stage('Generate jobs') {
            def envProps = getRepoEnvProperties()
            envProps.put('JOB_BRANCH_FOLDER', "${GENERATION_BRANCH}")
            envProps.put('GIT_MAIN_BRANCH', "${GIT_MAIN_BRANCH}")

            // Add other repos `jenkins_config_path` var (useful if multijob PR checks for example)
            getAllBranchRepos().each { repoName ->
                String key = generateEnvKey(repoName, 'jenkins_config_path')
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
                jobDsl targets: 'jobs/jobs.groovy',
                sandbox: false,
                ignoreExisting: false,
                ignoreMissingFiles: false,
                // TODO DELETE should be done once all jobs of a branch are in separate branch folder
                // Doing it before that is dangerous and may remove some other jobs ...
                removedJobAction: 'IGNORE',
                removedViewAction: 'IGNORE',
                //removedConfigFilesAction: 'IGNORE',
                lookupStrategy: 'SEED_JOB',
                additionalClasspath: 'src/main/groovy',
                additionalParameters : envProps
            }
        }
    }
}

def initNode() {
    checkout scm

    dir("${SEED_REPO}") {
        checkout(githubscm.resolveRepository("${SEED_REPO}", "${SEED_AUTHOR}", "${SEED_BRANCH}", false))
        echo 'This is the generate repo seed jobs'

        util = load "${SEED_FOLDER}/jobs/scripts/util.groovy"
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

def getRepoEnvProperties() {
    Map envProperties = [:]
    fillEnvProperties(envProperties, '', getRepoConfig())
    if (util.isDebug()) {
        println '[DEBUG] Environment properties:'
        envProperties.each {
            println "[DEBUG] ${it.key} = ${it.value}"
        }
    }
    return envProperties
}

void fillEnvProperties(Map envProperties, String envKeyPrefix, Map propsMap) {
    propsMap.each { it ->
        String newKey = generateEnvKey(envKeyPrefix, it.key)
        def value = it.value
        if (util.isDebug()) {
            println "[DEBUG] Setting key ${newKey} and value ${value}"
        }
        if (value instanceof Map) {
            fillEnvProperties(envProperties, newKey, value as Map)
        } else if (value instanceof List) {
            envProperties.put(newKey, (value as List).join(','))
        } else {
            envProperties.put(newKey, value)
        }
    }
}

String generateEnvKey(String envKeyPrefix, String key) {
    return (envKeyPrefix ? "${envKeyPrefix}_${key}" : key).toUpperCase()
}

List getAllBranchRepos() {
    return util.readBranchConfig("${SEED_REPO}").repositories.collect { it.name }
}

return this
