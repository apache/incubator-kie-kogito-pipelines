import org.kie.jenkins.MavenCommand
import org.kie.jenkins.MavenSettingsUtils

void launch() {
    docker.image('quay.io/jan_stastny/kogito-ci-build:0.0.0-test2').inside {
        launchStages()
    }
}

void launchStages() {
    stage('Initialize') {
        sh 'printenv > env_props'
        archiveArtifacts artifacts: 'env_props'
    }
    stage('check space before build') {
        try {
            util.spaceLeft()
        } catch (err) {
            echo "Error when checking the space on node ... ${err}"
        }
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
    }
    stage('Build projects') {
        env.BUILD_MVN_OPTS_CURRENT = "${env.BUILD_MVN_OPTS_CURRENT ?: ''} ${getBuildMavenOptsCurrent()}"
        echo "BUILD_MVN_OPTS_CURRENT = ${BUILD_MVN_OPTS_CURRENT}"

        configFileProvider([configFile(fileId: 'kogito_pr_settings', variable: 'MAVEN_SETTINGS_FILE')]) { // TODO as env ?
            withCredentials([string(credentialsId: 'kie-ci3-token', variable: 'GITHUB_TOKEN')]) { // TODO as env ?
                env.BUILD_MVN_OPTS = "${env.BUILD_MVN_OPTS ?: ''} -s ${MAVEN_SETTINGS_FILE} -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true"
                echo "BUILD_MVN_OPTS = ${BUILD_MVN_OPTS}"

                try {
                    util.runWithPythonVirtualEnv("${getBuildChainCommandline()}", 'swf')
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
    stage('check space after build') {
        util.spaceLeft()
    }
}

String getBuildChainCommandline() {
    // TODO those should be parametrized
    String buildChainProject = env.BUILDCHAIN_PROJECT ?: CHANGE_REPO
    String buildChainConfigRepo = env.BUILDCHAIN_CONFIG_REPO ?: 'kogito-pipelines'
    String buildChainConfigBranch = env.BUILDCHAIN_CONFIG_BRANCH ?: '\${BRANCH:main}'
    String buildChainConfigGitAuthor = env.BUILDCHAIN_CONFIG_AUTHOR ?: '\${AUTHOR:kiegroup}'
    String buildChainConfigDefinitionFilePath = env.BUILDCHAIN_CONFIG_FILE_PATH ?: '.ci/pull-request-config.yaml'

    List buildChainAdditionalArguments = [
        "-p ${buildChainProject}",
        "-u ${CHANGE_URL}", // Provided by source branch plugin
    ]
    return "build-chain build cross_pr --token ${GITHUB_TOKEN} -f 'https://raw.githubusercontent.com/${buildChainConfigGitAuthor}/${buildChainConfigRepo}/${buildChainConfigBranch}/${buildChainConfigDefinitionFilePath}' -o 'bc' ${buildChainAdditionalArguments.join(' ')} --skipParallelCheckout"
}

String getBuildMavenOptsCurrent() {
    List opts_current = []
    isEnableSonarCloudAnalysis() ? opts_current.add('-Prun-code-coverage') : null
    return opts_current.join(' ')
}

boolean isEnableSonarCloudAnalysis() {
    return env.ENABLE_SONARCLOUD ? env.ENABLE_SONARCLOUD.toBoolean() : false
}

return this
