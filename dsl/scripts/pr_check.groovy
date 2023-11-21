/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.kie.jenkins.MavenCommand

// TODO Docker image and args could be passed as env or anything ?
dockerGroups = [ 
    'docker',
    'input',
    'render',
]
dockerArgs = [
    '-v /var/run/docker.sock:/var/run/docker.sock',
    '--network host',
] + dockerGroups.collect { group -> "--group-add ${group}" }

void launch() {
    String builderImage = 'quay.io/kiegroup/kogito-ci-build:main-latest'
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
                postComment(
                    util.getMarkdownTestSummary('PR', getReproducer(true), "${BUILD_URL}", 'GITHUB'),
                    'kie-ci3-token'
                )
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
        sh 'sudo alternatives --install /usr/local/bin/build-chain build-chain ${NODE_HOME}/bin/build-chain 1'
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
                configFileProvider([configFile(fileId: 'kie-pr-settings', variable: 'MAVEN_SETTINGS_FILE')]) {
                    withCredentials([string(credentialsId: 'SONARCLOUD_TOKEN', variable: 'TOKEN')]) {
                        new MavenCommand(this)
                                .withProperty('sonar.login', "${TOKEN}")
                                .withProperty('sonar.organization', 'apache') // override what's in pom.xml for now
                                .withProperty('sonar.projectKey', env.SONAR_PROJECT_KEY)
                                .withLogFileName('sonar_analysis.maven.log')
                                .withSettingsXmlFile(MAVEN_SETTINGS_FILE)
                                .run("-e -nsu validate -Psonarcloud-analysis -Denforcer.skip=true ${env.SONARCLOUD_ANALYSIS_MVN_OPTS ?: ''}")
                    }
                }
            }
        }
    }
}

String getBuildChainCommandline() {
    // Those can be overriden in Jenkinsfiles
    String buildChainProject = env.BUILDCHAIN_PROJECT ?: CHANGE_REPO
    String buildChainConfigRepo = env.BUILDCHAIN_CONFIG_REPO ?: 'incubator-kie-kogito-pipelines'
    String buildChainConfigBranch = env.BUILDCHAIN_CONFIG_BRANCH ?: '\${BRANCH:main}'
    String buildChainConfigGitAuthor = env.BUILDCHAIN_CONFIG_AUTHOR ?: '\${AUTHOR:apache}'
    String buildChainConfigDefinitionFilePath = env.BUILDCHAIN_CONFIG_FILE_PATH ?: '.ci/buildchain-config-pr-cdb.yaml'

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

/**
* This method add a comment to current PR (for Github Branch Source plugin)
*/
void postComment(String commentText, String githubTokenCredsId = "kie-ci1-token") {
    if (!CHANGE_ID) {
        error "Pull Request Id variable (CHANGE_ID) is not set. Are you sure you are running with Github Branch Source plugin ?"
    }
    String filename = "${util.generateHash(10)}.build.summary"
    def jsonComment = [
        body : commentText
    ]
    writeJSON(json: jsonComment, file: filename)
    sh "cat ${filename}"
    withCredentials([string(credentialsId: githubTokenCredsId, variable: 'GITHUB_TOKEN')]) {
        sh "curl -s -H \"Authorization: token ${GITHUB_TOKEN}\" -X POST -d '@${filename}' \"https://api.github.com/repos/${BUILDCHAIN_PROJECT}/issues/${CHANGE_ID}/comments\""
    }
    sh "rm ${filename}"
}

return this
