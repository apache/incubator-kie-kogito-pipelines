
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

package org.kie.jenkins.jobdsl

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

import org.kie.jenkins.jobdsl.utils.PrintUtils

class Utils {

    static def deepCopyObject(def originalMap) {
        return new JsonSlurper().parseText(JsonOutput.toJson(originalMap))
    }

    static String createRepositoryUrl(String author, String repositoryName) {
        return "https://github.com/${author}/${repositoryName}.git"
    }

    static String createProjectUrl(String author, String repositoryName) {
        return "https://github.com/${author}/${repositoryName}/"
    }

    static boolean areTriggersDisabled(def script) {
        return getBindingValue(script, 'DISABLE_TRIGGERS').toBoolean() || isTestEnvironment(script)
    }

    static def getBindingValue(def script, String key) {
        return hasBindingValue(script, key) ? script.getBinding().getVariable(key) : ''
    }

    static boolean hasBindingValue(def script, String key) {
        return script.getBinding().hasVariable(key)
    }

    static boolean isJobTypeDisabled(def script, String jobType) {
        String envVar = "JOB_TYPES_${jobType.toUpperCase()}_DISABLED"
        return getBindingValue(script, envVar).toBoolean()
    }

    static List getBindingValuesStartingWith(def script, String keyPrefix) {
        return script.getBinding()
                    .getVariables()
                    .keySet()
                    .findAll { key -> key.startsWith(keyPrefix) }
                    .collect { it } // Transform to list
    }

    static boolean hasBindingValuesStartingWith(def script, String keyPrefix) {
        return getBindingValuesStartingWith(script, keyPrefix).size() > 0
    }

    static String getGenerationBranch(def script) {
        return getBindingValue(script, 'GENERATION_BRANCH')
    }

    static boolean isMainBranch(def script) {
        boolean result = getGitBranch(script) == getGitMainBranch(script)
        PrintUtils.debug(script, "Branch=${getGitBranch(script)}. Main Branch=${getGitMainBranch(script)}. Is main branch ? => ${result}")
        return result
    }

    static String getRepoName(def script) {
        return getBindingValue(script, 'REPO_NAME')
    }

    static String getRepositoryJobDisplayName(def script, String repository) {
        return getBindingValue(script, "${repository.toUpperCase()}_JOB_DISPLAY_NAME") ?: repository
    }

    static String getJobDisplayName(def script) {
        return getRepositoryJobDisplayName(script, getRepoName(script))
    }

    static String getGitBranch(def script) {
        return getBindingValue(script, 'GIT_BRANCH')
    }

    static String getGitMainBranch(def script) {
        return getBindingValue(script, 'GIT_MAIN_BRANCH')
    }

    static String getStream(def script) {
        String gitMainBranch = Utils.getGitMainBranch(script)
        if (gitMainBranch == 'main') {
            return 'main'
        } else {
            return gitMainBranch.split("\\.")[0]
        }
    }

    static boolean hasGitAuthor(def script) {
        return hasBindingValue(script, 'GIT_AUTHOR_NAME')
    }

    static String getGitAuthor(def script) {
        return getBindingValue(script, 'GIT_AUTHOR_NAME')
    }

    static boolean hasSeedConfigFileGitAuthor(def script) {
        return hasBindingValue(script, 'SEED_CONFIG_FILE_GIT_AUTHOR_NAME')
    }

    static String getSeedConfigFileGitAuthor(def script) {
        return getBindingValue(script, 'SEED_CONFIG_FILE_GIT_AUTHOR_NAME')
    }

    static boolean isProdEnvironment(def script) {
        // Check for all possible `GIT_AUTHOR_NAME` variables
        return getSeedAuthor(script) == 'apache' &&
            (hasGitAuthor(script) ? getGitAuthor(script) == 'apache' : true) &&
            (hasSeedConfigFileGitAuthor(script) ? getSeedConfigFileGitAuthor(script) == 'apache' : true)
    }

    static boolean isTestEnvironment(def script) {
        return !isProdEnvironment(script)
    }

    static String getGitAuthorCredsId(def script) {
        return getBindingValue(script, 'GIT_AUTHOR_CREDENTIALS_ID')
    }

    static String getGitAuthorTokenCredsId(def script) {
        return getBindingValue(script, 'GIT_AUTHOR_TOKEN_CREDENTIALS_ID')
    }

    static String getGitAuthorPushCredsId(def script) {
        return getBindingValue(script, 'GIT_AUTHOR_PUSH_CREDENTIALS_ID')
    }

    static String getGitAuthorPushTokenCredsId(def script) {
        return getBindingValue(script, 'GIT_AUTHOR_PUSH_TOKEN_CREDENTIALS_ID')
    }

    static String getGitForkAuthorName(def script) {
        return getBindingValue(script, 'GIT_FORK_AUTHOR_NAME')
    }

    static String getGitForkAuthorCredsId(def script) {
        return getBindingValue(script, 'GIT_FORK_AUTHOR_CREDENTIALS_ID')
    }

    static String getGitForkAuthorPushCredsId(def script) {
        return getBindingValue(script, 'GIT_FORK_AUTHOR_PUSH_CREDENTIALS_ID')
    }

    static String getGitQuarkusBranch(def script) {
        return getBindingValue(script, 'GIT_QUARKUS_BRANCH')
    }

    static String getGitQuarkusAuthor(def script) {
        return getBindingValue(script, 'GIT_QUARKUS_AUTHOR_NAME')
    }

    static String getGitQuarkusAuthorCredsId(def script) {
        return getBindingValue(script, 'GIT_QUARKUS_AUTHOR_CREDENTIALS_ID')
    }

    static String getRepositoryGitBranch(def script, String repoName) {
        return getBindingValue(script, "${repoName.toUpperCase()}_GIT_BRANCH")
    }

    static String getRepositoryGitAuthor(def script, String repoName) {
        return getBindingValue(script, "${repoName.toUpperCase()}_GIT_AUTHOR")
    }

    static String getRepositoryGitAuthorCredentialsId(def script, String repoName) {
        return getBindingValue(script, "${repoName.toUpperCase()}_GIT_AUTHOR_CREDENTIALS_ID")
    }

    static String getRepositoryGitAuthorPushCredentialsId(def script, String repoName) {
        return getBindingValue(script, "${repoName.toUpperCase()}_GIT_AUTHOR_PUSH_CREDENTIALS_ID")
    }

    static String getRepositoryGitAuthorPushTokenCredentialsId(def script, String repoName) {
        return getBindingValue(script, "${repoName.toUpperCase()}_GIT_AUTHOR_PUSH_TOKEN_CREDENTIALS_ID")
    }

    static String getRepositoryJenkinsConfigPath(def script, String repoName) {
        return getBindingValue(script, "${repoName.toUpperCase()}_JENKINS_CONFIG_PATH")
    }

    static String getJenkinsEmailCredsId(def script) {
        return getBindingValue(script, 'JENKINS_EMAIL_CREDS_ID')
    }

    static String getJenkinsDefaultTools(def script, String toolsName) {
        return getBindingValue(script, "JENKINS_DEFAULT_TOOLS_${toolsName.toUpperCase()}")
    }

    static String getJenkinsDefaultJDKTools(def script) {
        return getJenkinsDefaultTools(script, 'jdk')
    }

    static String getJenkinsDefaultMavenTools(def script) {
        return getJenkinsDefaultTools(script, 'maven')
    }

    static String getJenkinsDefaultSonarJDKTools(def script) {
        return getJenkinsDefaultTools(script, 'sonar_jdk')
    }

    static String getSeedRepo(def script) {
        return getBindingValue(script, 'SEED_REPO')
    }

    static String getSeedAuthor(def script) {
        return getBindingValue(script, 'SEED_AUTHOR')
    }

    static String getSeedAuthorCredsId(def script) {
        return getBindingValue(script, 'SEED_AUTHOR_CREDS_ID')
    }

    static String getSeedAuthorPushCredsId(def script) {
        return getBindingValue(script, 'SEED_AUTHOR_PUSH_CREDS_ID')
    }

    static String getSeedBranch(def script) {
        return getBindingValue(script, 'SEED_BRANCH')
    }

    static String getBuildChainConfigRepo(def script) {
        return getBindingValue(script, 'BUILDCHAIN_CONFIG_GIT_REPOSITORY')
    }

    static String getBuildChainConfigAuthor(def script) {
        return getBindingValue(script, 'BUILDCHAIN_CONFIG_GIT_AUTHOR_NAME')
    }

    static String getBuildChainConfigBranch(def script) {
        return getBindingValue(script, 'BUILDCHAIN_CONFIG_GIT_BRANCH')
    }

    static String getBuildChainConfigFilePath(def script) {
        return getBindingValue(script, 'BUILDCHAIN_CONFIG_GIT_FILE_PATH')
    }

    static String getBuildChainConfigTokenCredentialsId(def script) {
        return getBindingValue(script, 'BUILDCHAIN_CONFIG_GIT_TOKEN_CREDENTIALS_ID')
    }

    static String getMavenArtifactsUploadRepositoryUrl(def script) {
        return getBindingValue(script, 'MAVEN_ARTIFACTS_UPLOAD_REPOSITORY_URL')
    }

    static String getMavenArtifactsUploadRepositoryCredentialsId(def script) {
        return getBindingValue(script, 'MAVEN_ARTIFACTS_UPLOAD_REPOSITORY_CREDS_ID')
    }

    static String getMavenQuarkusPlatformRepositoryUrl(def script) {
        return getBindingValue(script, 'MAVEN_QUARKUS_PLATFORM_REPOSITORY_URL')
    }

    static String getMavenQuarkusPlatformRepositoryCredentialsId(def script) {
        return getBindingValue(script, 'MAVEN_QUARKUS_PLATFORM_REPOSITORY_CREDS_ID')
    }

    static String getJenkinsAgentDockerImage(def script, String imageId) {
        return getBindingValue(script, "JENKINS_AGENT_DOCKER_${imageId.toUpperCase()}_IMAGE")
    }

    static String getJenkinsAgentDockerArgs(def script, String imageId) {
        return getBindingValue(script, "JENKINS_AGENT_DOCKER_${imageId.toUpperCase()}_ARGS")
    }

    static String getSeedJenkinsfilePath(def script, String jenkinsfileName) {
        return "${KogitoConstants.SEED_JENKINSFILES_PATH}/${jenkinsfileName}"
    }

    static boolean isOldFolderStructure(def script) {
        return getBindingValue(script, 'OLD_FOLDER_STRUCTURE')?.toBoolean()
    }

    static String getGenerationMissingEnvironment(def script) {
        return getBindingValue(script, 'GENERATION_CONFIG_MISSING_ENVIRONMENT')
    }

    static boolean isGenerationIgnoreOnMissingEnvironment(def script) {
        return getGenerationMissingEnvironment(script) == 'ignore'
    }

    static boolean isGenerationFailOnMissingEnvironment(def script) {
        return getGenerationMissingEnvironment(script) == 'fail'
    }

    /**
    *   Return the given string with all words beginning with first letter as upper case
    */
    static String allFirstLetterUpperCase(String str) {
        List words = str.split('-') as List
        return words.collect { firstLetterUpperCase(it) }.join(' ')
    }

    /**
    * Set first letter as upper case
    */
    static String firstLetterUpperCase(String str) {
        return str.length() > 1 ? "${str.substring(0, 1).toUpperCase()}${str.substring(1)}" : str.toUpperCase()
    }

    static String getRepoNameCamelCase(String repo) {
        List words = repo.split('-') as List
        return words.collect { it.isEmpty() ? it : it.substring(0, 1).toUpperCase() + it.substring(1).toLowerCase() }.join(' ')
    }

    static boolean isDeployDisabled(def script) {
        return getBindingValue(script, 'DISABLE_DEPLOY').toBoolean() || isTestEnvironment(script)
    }

    static boolean isImagesDeployDisabled(def script) {
        return getBindingValue(script, 'DISABLE_IMAGES_DEPLOY').toBoolean() || isTestEnvironment(script)
    }

    static boolean isPrCheckDisabled(def script) {
        return getBindingValue(script, 'DISABLE_PR_CHECK').toBoolean() || isTestEnvironment(script)
    }

    static String getReleaseGpgSignKeyCredentialsId(def script) {
        return getBindingValue(script, 'RELEASE_GPG_SIGN_KEY_CREDENTIALS_ID')
    }
    static String getReleaseGpgSignPassphraseCredentialsId(def script) {
        return getBindingValue(script, 'RELEASE_GPG_SIGN_PASSPHRASE_CREDENTIALS_ID')
    }
    static String getReleaseSvnCredentialsId(def script) {
        return getBindingValue(script, 'RELEASE_SVN_CREDENTIALS_ID')
    }
    static String getReleaseSvnStagingRepository(def script) {
        return getBindingValue(script, 'RELEASE_SVN_STAGING_REPOSITORY')
    }

}
