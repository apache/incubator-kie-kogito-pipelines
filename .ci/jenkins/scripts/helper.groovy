import org.kie.jenkins.MavenCommand

prBranchHash = util.generateHash(10)
mavenDeployLocalDir = "${WORKSPACE}/maven_deploy_dir"

void init(String displayName = '') {
    currentBuild.displayName = displayName ?: params.DISPLAY_NAME ?: "#${env.BUILD_NUMBER}"

    executeInPipelinesDirectory() {
        deleteDir()
        checkout scm
    }

    setPipelinePropertyIfNeeded('git.branch', getGitBranch())
    setPipelinePropertyIfNeeded('git.author', getGitAuthor())
    setPipelinePropertyIfNeeded('kogito.version', getKogitoVersion())
    setPipelinePropertyIfNeeded('optaplanner.version', getOptaPlannerVersion())
    setPipelinePropertyIfNeeded('release', isRelease())
}

void sendNotification(String msgBody, String repo = getRepoName()) {
    if (params.SEND_NOTIFICATION || currentBuild.currentResult != 'SUCCESS') {
        emailext body: msgBody,
             subject: "[${getGitBranch()}] ${getRepoNameCamelCase(repo)}",
             to: env.KOGITO_CI_EMAIL_TO
    } else {
        echo 'No notification sent per configuration'
    }
}

void executeInRepositoryDirectory(String repository = getRepoName(), Closure executeClosure) {
    dir("${WORKSPACE}/${repository}") {
        return executeClosure()
    }
}

void executeInPipelinesDirectory(Closure executeClosure) {
    return executeInRepositoryDirectory('pipelines', executeClosure)
}

////////////////////////////////////////////////////////////////////////////
//  Scripting

String constructFullScriptPath(String scriptId, String scriptName, String baseScriptPath = '.ci/jenkins/scripts') {
    return "${baseScriptPath}/${scriptId}/${scriptName}"
}

String constructFullShellScriptPath(String scriptId, String scriptNameWithoutExtension, String baseScriptPath = '.ci/jenkins/scripts') {
    return "${constructFullScriptPath(scriptId, "${scriptNameWithoutExtension}.sh", baseScriptPath)}"
}

String constructFullGroovyScriptPath(String scriptId, String scriptNameWithoutExtension, String baseScriptPath = '.ci/jenkins/scripts') {
    return "${constructFullScriptPath(scriptId, "${scriptNameWithoutExtension}.groovy", baseScriptPath)}"
}

boolean isScriptExisting(String scriptId, String scriptNameWithoutExtension, String baseScriptPath = '.ci/jenkins/scripts') {
    return isShellScriptExisting(scriptId, scriptNameWithoutExtension, baseScriptPath) || isGroovyScriptExisting(scriptId, scriptNameWithoutExtension, baseScriptPath)
}

boolean isShellScriptExisting(String scriptId, String scriptNameWithoutExtension, String baseScriptPath = '.ci/jenkins/scripts') {
    return fileExists(constructFullShellScriptPath(scriptId, scriptNameWithoutExtension, baseScriptPath))
}

boolean isGroovyScriptExisting(String scriptId, String scriptNameWithoutExtension, String baseScriptPath = '.ci/jenkins/scripts') {
    return fileExists(constructFullGroovyScriptPath(scriptId, scriptNameWithoutExtension, baseScriptPath))
}

void executeScriptIfExists(String scriptId, String scriptNameWithoutExtension, String repository = getRepoName(), boolean failOnMissingScript = false, String baseScriptPath = '.ci/jenkins/scripts') {
    Closure executeClosure = {
        if (isShellScriptExisting(scriptId, scriptNameWithoutExtension, baseScriptPath)) {
            String scriptPath = constructFullShellScriptPath(scriptId, scriptNameWithoutExtension, baseScriptPath)
            echo "Execute script ${scriptPath} for repository ${repository}"
            sh "${scriptPath}"
            return true
        } else if (isGroovyScriptExisting(scriptId, scriptNameWithoutExtension, baseScriptPath)) {
            String scriptPath = constructFullGroovyScriptPath(scriptId, scriptNameWithoutExtension, baseScriptPath)
            echo "Execute script ${scriptPath} for repository ${repository}"
            def preDeployScript = load "${scriptPath}"
            preDeployScript.execute(this)
            return true
        }
        return false
    }

    if(!executeInRepositoryDirectory(repository, executeClosure) && !executeInPipelinesDirectory(executeClosure)) {
        String msg = "Script with id ${scriptId}, name ${scriptNameWithoutExtension} and base path ${baseScriptPath} has no supported extension (*.groovy, *.shell) in repository ${repository}, nor in pipelines repository. You should define one."
        if (failOnMissingScript) {
            error "[ERROR] ${msg}"
        } else {
            println "[WARN] ${msg}"
        }
    }
}

void executeInitializeScriptIfExists(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    executeScriptIfExists(env.SCRIPT_ID, 'init', repository, false, baseScriptPath)
}

boolean isScriptExistingInRepository(String repository, String scriptId, String scriptNameWithoutExtension, String baseScriptPath = '.ci/jenkins/scripts') {
    Closure lookupClosure = { return isScriptExisting(scriptId, scriptNameWithoutExtension, baseScriptPath) }
    return executeInRepositoryDirectory(repository, lookupClosure) || executeInPipelinesDirectory(lookupClosure)
}

boolean isInitializeScriptExisting(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    return isScriptExistingInRepository(repository, env.SCRIPT_ID, 'init', baseScriptPath)
}

void executeBeforeMainScriptIfExists(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    executeScriptIfExists(env.SCRIPT_ID, 'before_main', repository, false, baseScriptPath)
}

boolean isBeforeMainScriptExisting(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    return isScriptExistingInRepository(repository, env.SCRIPT_ID, 'before_main', directory, baseScriptPath)
}

void executeMainScriptIfExists(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    executeScriptIfExists(env.SCRIPT_ID, 'main', repository, true, baseScriptPath)
}

boolean isMainScriptExisting(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    return isScriptExistingInRepository(repository, env.SCRIPT_ID, 'main', baseScriptPath)
}

void executeAfterMainScriptIfExists(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    executeScriptIfExists(env.SCRIPT_ID, 'after_main', repository, false, baseScriptPath)
}

boolean isAfterMainScriptExisting(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    return isScriptExistingInRepository(repository, env.SCRIPT_ID, 'after_main', baseScriptPath)
}

void executeGitStageFilesScriptIfExists(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    executeScriptIfExists(env.SCRIPT_ID, 'git_stage_files', repository, false, baseScriptPath)
}

boolean isGitStageFilesScriptExisting(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    return isScriptExistingInRepository(repository, env.SCRIPT_ID, 'git_stage_files', baseScriptPath)
}

void executePostScriptIfExists(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    executeScriptIfExists(env.SCRIPT_ID, 'post', repository, false, baseScriptPath)
}

boolean isPostScriptExisting(String repository = getRepoName(), String baseScriptPath = '.ci/jenkins/scripts') {
    return isScriptExistingInRepository(repository, env.SCRIPT_ID, 'post', baseScriptPath)
}

////////////////////////////////////////////////////////////////////////////
//  Maven

void saveMavenReports(boolean allowEmpty=false) {
    junit testResults: '**/target/surefire-reports/**/*.xml, **/target/failsafe-reports/**/*.xml', allowEmptyResults: allowEmpty
}

MavenCommand getDefaultMavenCommand() {
    def mvnCmd = new MavenCommand(this, isRelease() ? [] : ['-fae'])

    if (env.MAVEN_SETTINGS_CONFIG_FILE_ID) {
        mvnCmd.withSettingsXmlId(env.MAVEN_SETTINGS_CONFIG_FILE_ID)
    }
    if (env.MAVEN_DEPENDENCIES_REPOSITORY) {
        mvnCmd.withDependencyRepositoryInSettings('deps-repo', env.MAVEN_DEPENDENCIES_REPOSITORY)
    }

    return mvnCmd
}

////////////////////////////////////////////////////////////////////////////
//  Git

void checkoutRepo(String repository = getRepoName()) {
    executeInRepositoryDirectory(repository) {
        deleteDir()
        checkout(githubscm.resolveRepository(repository, getGitAuthor(), getGitBranch(), false))
        sh "git checkout ${getGitBranch()}"
    }
}

prepareForPRRepos = [:]

void prepareForPR(String repository = getRepoName(), String branchName = getPRBranch()) {
    executeInRepositoryDirectory(repository) {
        sh "git checkout ${getGitBranch()}"
        githubscm.createBranch(branchName)
    }
    prepareForPRRepos.put(repository, branchName)
}

String getCommitMessageForVersionUpdate() {
    String msg = "[${getGitBranch()}] Update version to"
    msg += getKogitoVersion() ? " Kogito ${getKogitoVersion()}" : ''
    msg += getOptaPlannerVersion() ? " OptaPlanner ${getOptaPlannerVersion()}" : ''
    return msg
}

void stageAllFiles(String repository = getRepoName()) {
    executeInRepositoryDirectory(repository) {
        sh 'git add .'
    }
}

void commitChanges(String commitMsg, boolean failOnNoChanges = false, String repository = getRepoName()) {
    executeInRepositoryDirectory(repository) {
        if (failOnNoChanges && !githubscm.isThereAnyChanges()) {
            error 'No files to stage'
        }

        githubscm.commitChanges(commitMsg, { })
    }
}

def createPR(String prMsg, boolean shouldBeMergedAutomatically = false, String repository = getRepoName()) {
    if (!prepareForPRRepos.containsKey(repository)) {
        error "Repository ${repository} has not been prepared for PR"
    }

    def prBody = "Generated by build ${BUILD_TAG}: ${BUILD_URL}.\n"
    if (shouldBeMergedAutomatically) {
        prBody = 'Please do not merge, it should be merged automatically.'
    } else {
        prBody = 'Please review and merge.'
    }

    String prLink = ''
    executeInRepositoryDirectory(repository) {
        githubscm.pushObject('origin', prepareForPRRepos[repository], getGitAuthorCredsId())
        prLink = githubscm.createPR(prMsg, prBody, getGitBranch(), getGitAuthorCredsId())
    }

    setPipelinePropertyIfNeeded("${repository}.pr.link", prLink)
    setPipelinePropertyIfNeeded("${repository}.pr.source.ref", prepareForPRRepos[repository])

    return prLink
}

void mergeAndPushPR(boolean removeSourceBranch = true, String repository = getRepoName()) {
    String prLink = getPRLink(repository)
    String sourceBranch = getPRSourceBranch(repository)
    if (prLink) {
        executeInRepositoryDirectory(repository) {
            githubscm.mergePR(prLink, getGitAuthorCredsId())
            githubscm.pushObject('origin', getGitBranch(), getGitAuthorCredsId())

            if (removeSourceBranch && sourceBranch) {
                githubscm.pushObject('origin', "--delete ${sourceBranch}", getGitAuthorCredsId())
            }
        }
    } else {
        echo "No PR found for repository ${repository}"
    }
}

void tagRepository(String repository = getRepoName()) {
    if (getGitTag()) {
        executeInRepositoryDirectory(repository) {
            githubscm.tagLocalAndRemoteRepository('origin', getGitTag(), getGitAuthorCredsId(), env.BUILD_TAG, true)
        }
    }
}

////////////////////////////////////////////////////////////////////////////
//  Pipeline Properties

pipelineProperties = [:]

void setPipelinePropertyIfNeeded(String key, def value) {
    if (value) {
        pipelineProperties[key] = value
    }
}

void archivePipelineProperties() {
    def propertiesStr = pipelineProperties.collect { entry ->  "${entry.key}=${entry.value}" }.join('\n')
    writeFile(text: propertiesStr, file: 'pipeline.properties')
    archiveArtifacts(artifacts: 'pipeline.properties')
}

void readPipelineProperties() {
    String propsUrl = params.DEPLOY_BUILD_URL
    if (propsUrl != '') {
        if (!propsUrl.endsWith('/')) {
            propsUrl += '/'
        }
        sh "wget ${propsUrl}artifact/${PROPERTIES_FILE_NAME} -O ${PROPERTIES_FILE_NAME}"
        pipelineProperties = readProperties file: PROPERTIES_FILE_NAME
        // echo all properties
        echo pipelineProperties.collect { entry -> "${entry.key}=${entry.value}" }.join('\n')
    }
}

boolean hasPipelineProperty(String key) {
    return pipelineProperties[key] != null
}

String getPipelineProperty(String key) {
    if (hasPipelineProperty(key)) {
        return pipelineProperties[key]
    }
    return ''
}

String getParamOrPipelineProperty(String paramKey, String deployPropertyKey) {
    return params[paramKey] ?: getPipelineProperty(deployPropertyKey)
}

String getPRLink(String repository = getRepoName()) {
    return getParamOrPipelineProperty('PR_LINK', "${repository}.pr.link")
}

String getPRSourceBranch(String repository = getRepoName()) {
    return getParamOrPipelineProperty('PR_SOURCE_BRANCH', "${repository}.pr.source.ref")
}

///////////////////////////////////////////////////////////////////////////
// Getters

boolean isRelease() {
    return "${getParamOrEnv('RELEASE')}".toBoolean()
}

boolean isCreatePR() {
    return "${getParamOrEnv('CREATE_PR')}".toBoolean()
}

boolean isPRMergedAutomatically() {
    return "${getParamOrEnv('MERGE_PR_AUTOMATICALLY')}".toBoolean()
}

String getRepoName() {
    return getParamOrEnv('REPO_NAME')
}

String getRepoNameCamelCase(String repo = getRepoName()) {
    List words = repo.split('-') as List
    return words.collect { it.isEmpty() ? it : it.substring(0, 1).toUpperCase() + it.substring(1).toLowerCase() }.join(' ')
}

String getGitBranch() {
    return getParamOrEnv('GIT_BRANCH_NAME')
}

String getGitAuthor() {
    return getParamOrEnv('GIT_AUTHOR')
}

String getGitAuthorCredsId() {
    return getParamOrEnv('GIT_AUTHOR_CREDS_ID')
}

String getGitAuthorTokenCredsId() {
    return getParamOrEnv('GIT_AUTHOR_TOKEN_CREDS_ID')
}

String getKogitoVersion() {
    return getParamOrEnv('KOGITO_VERSION')
}

String getOptaPlannerVersion() {
    return getParamOrEnv('OPTAPLANNER_VERSION')
}

String getDroolsVersion() {
    return getParamOrEnv('DROOLS_VERSION')
}

String getPRBranch() {
    return getParamOrEnv('PR_BRANCH') ?: "${getGitBranch()}-${prBranchHash}"
}

String getGitTag() {
    return getParamOrEnv('GIT_TAG')
}

def getParamOrEnv(String key) {
    return params[key] ?: env[key]
}

boolean isNotTestingBuild() {
    return getGitAuthor() == 'kiegroup'
}


return this
