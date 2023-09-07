// TODO Docker image and args could be passed as env or anything ?
dockerGroups = [ 
    'docker',
    'input',
    'render',
]
dockerArgs = [
    '-v /var/run/docker.sock:/var/run/docker.sock',
] + dockerGroups.collect { group -> "--group-add ${group}" }

void launch() {
    String builderImage = 'quay.io/kiegroup/kogito-ci-build:latest'
    sh "docker rmi -f ${builderImage} || true" // Remove before launching

    try {
        launchInDocker(builderImage)
    } finally {
        sh "docker rmi -f ${builderImage} || true"
    }
}

void launchInDocker(String builderImage) {
    docker.image(builderImage).inside(dockerArgs.join(' ')) {
        // Debug. To be removed in the future
        sh "printenv"
        sh 'ls -last /var/run/docker.sock'
        try {
            launchStages()
        } finally {
            echo "Got build result ${currentBuild.currentResult}"
            if (currentBuild.currentResult != 'SUCCESS') {
                // TODO ci token as env ?
                pullrequest.postComment(util.getMarkdownTestSummary('PR', getReproducer(true), "${BUILD_URL}", 'GITHUB'), "kie-ci3-token")
            }
        }
    }
}

void launchStages() {
    stage('Initialize') {
        sh 'printenv > env_props'
        archiveArtifacts artifacts: 'env_props'
    }
    stage('Install build-chain tool') {
        println '[INFO] Getting build-chain version from composite action file'
        def buildChainVersion = buildChain.getBuildChainVersionFromCompositeActionFile()
        if ([null, 'null'].contains(buildChainVersion)) {
            def errorMessage = "[ERROR] The build-chain version can't be recovered. Please contact administrator"
            println errorMessage
            error(errorMessage)
        }
        println "[INFO] build-chain version recovered '${buildChainVersion}'"
        sh "npm install -g @kie/build-chain-action@${buildChainVersion}${env.NPM_REGISTRY_URL ? " -registry=${NPM_REGISTRY_URL}" : ''}"

        sh 'npm list -g | grep build-chain'

        sh 'sudo alternatives --install /usr/local/bin/build-chain build-chain /home/nonrootuser/.nvm/versions/node/v16.20.0/bin/build-chain 1'
        sh 'build-chain || true'
    }
    stage('Build projects') {
        configFileProvider([configFile(fileId: 'kie-pr-settings', variable: 'MAVEN_SETTINGS_FILE')]) { // TODO as env ?
            withCredentials([string(credentialsId: 'kie-ci3-token', variable: 'GITHUB_TOKEN')]) { // TODO as env ?
                env.BUILD_MVN_OPTS = "${env.BUILD_MVN_OPTS ?: ''} -s ${MAVEN_SETTINGS_FILE} -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true"
                echo "BUILD_MVN_OPTS = ${BUILD_MVN_OPTS}"

                try {
                    sh getBuildChainCommandline()
                } catch (err) {
                    echo 'Error running the build-chain ...'
                    util.archiveConsoleLog('', 300)
                    throw err
                } finally {
                    // Remove `node_modules` to avoid heap space issues with junit command thereafter
                    // Related to https://github.com/jenkinsci/junit-plugin/issues/478 and https://github.com/jenkinsci/junit-plugin/issues/467
                    sh 'find . -type d -name node_modules -exec rm -rf {} \\; || true'

                    junit(testResults: '**/junit.xml, **/target/surefire-reports/**/*.xml, **/target/failsafe-reports/**/*.xml, **/target/invoker-reports/**/*.xml', allowEmptyResults: true)
                    archiveArtifacts(artifacts: '**/cypress/screenshots/**,**/cypress/videos/**', fingerprint: false, allowEmptyArchive: true)
                }
            }
        }
    }
    stage('Sonar analysis') {
        if (isEnableSonarCloudAnalysis()) {
            dir(getProjectFolder()) {
                    maven.runMavenWithSettingsSonar(settingsXmlId, "-e -nsu validate -Psonarcloud-analysis -Denforcer.skip=true ${env.SONARCLOUD_ANALYSIS_MVN_OPTS ?: ''}", 'SONARCLOUD_TOKEN', 'sonar_analysis.maven.log')
            }
        }
    }
}

String getBuildChainCommandline() {
    // Those can be overriden in Jenkinsfiles
    String buildChainProject = env.BUILDCHAIN_PROJECT ?: CHANGE_REPO
    String buildChainConfigRepo = env.BUILDCHAIN_CONFIG_REPO ?: 'kogito-pipelines'
    String buildChainConfigBranch = env.BUILDCHAIN_CONFIG_BRANCH ?: '\${BRANCH:main}'
    String buildChainConfigGitAuthor = env.BUILDCHAIN_CONFIG_AUTHOR ?: '\${AUTHOR:kiegroup}'
    String buildChainConfigDefinitionFilePath = env.BUILDCHAIN_CONFIG_FILE_PATH ?: '.ci/pull-request-config.yaml'

    List buildChainAdditionalArguments = [
        "-p ${buildChainProject}",
        "-u ${CHANGE_URL}", // Provided by source branch plugin
    ]
    // TODO remove debug option
    return "build-chain build full_downstream ${env.GITHUB_TOKEN ? "--token ${GITHUB_TOKEN} " : ''} -f 'https://raw.githubusercontent.com/${buildChainConfigGitAuthor}/${buildChainConfigRepo}/${buildChainConfigBranch}/${buildChainConfigDefinitionFilePath}' -o 'bc' ${buildChainAdditionalArguments.join(' ')} --skipParallelCheckout"
}

boolean isEnableSonarCloudAnalysis() {
    return env.ENABLE_SONARCLOUD ? env.ENABLE_SONARCLOUD.toBoolean() : false
}

String getReproducer(boolean isGH = false) {
    String reproducer = """
${env.QUARKUS_BRANCH ? "export QUARKUS_BRANCH=${env.QUARKUS_BRANCH}" : ''}
${env.BUILD_MVN_OPTS_CURRENT ? "export BUILD_MVN_OPTS_CURRENT=${env.BUILD_MVN_OPTS_CURRENT}" : ''}
${getBuildChainCommandline()}

NOTE: To install the build-chain tool, please refer to https://github.com/kiegroup/github-action-build-chain#local-execution
"""

    if(isGH) {
        return """
<details>
<summary><b>Reproducer</b></summary>
${reproducer}
</details>
"""
    } else {
        return """
```spoiler Reproducer
${reproducer}
```
"""
    }
}

return this
