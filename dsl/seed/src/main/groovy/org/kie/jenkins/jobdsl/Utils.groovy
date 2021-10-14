
package org.kie.jenkins.jobdsl

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

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
        return getBindingValue(script, 'DISABLE_TRIGGERS').toBoolean()
    }

    static def getBindingValue(def script, String key) {
        return script.getBinding().hasVariable(key) ? script.getBinding().getVariable(key) : ''
    }

    static String getQuarkusLTSVersion(def script) {
        return getBindingValue(script, 'LTS_QUARKUS_VERSION')
    }

    static String getGenerationBranch(def script) {
        return getBindingValue(script, 'GENERATION_BRANCH')
    }

    static boolean isMainBranch(def script) {
        boolean result = getGitBranch(script) == getGitMainBranch(script)
        script.println("Branch=${getGitBranch(script)}. Main Branch=${getGitMainBranch(script)}. Is main branch ? => ${result}")
        return result
    }

    static boolean isLTSBranch(def script) {
        return getBindingValue(script, 'LTS_ENABLED').toBoolean()
    }

    static String getLTSNativeBuilderImage(def script) {
        return getBindingValue(script, 'LTS_NATIVE_BUILDER_IMAGE')
    }

    static String getRepoName(def script) {
        return getBindingValue(script, 'REPO_NAME')
    }

    static String getGitBranch(def script) {
        return getBindingValue(script, 'GIT_BRANCH')
    }

    static String getGitMainBranch(def script) {
        return getBindingValue(script, 'GIT_MAIN_BRANCH')
    }

    static String getGitAuthor(def script) {
        return getBindingValue(script, 'GIT_AUTHOR_NAME')
    }

    static String getGitAuthorCredsId(def script) {
        return getBindingValue(script, 'GIT_AUTHOR_CREDENTIALS_ID')
    }

    static String getGitAuthorTokenCredsId(def script) {
        return getBindingValue(script, 'GIT_AUTHOR_TOKEN_CREDENTIALS_ID')
    }

    static String getJenkinsConfigPath(def script, String repoName) {
        return getBindingValue(script, "${repoName.toUpperCase()}_JENKINS_CONFIG_PATH")
    }

    static String getPipelinesJenkinsConfigPath(def script) {
        return getJenkinsConfigPath(script, KogitoConstants.KOGITO_PIPELINES_REPOSITORY)
    }

    static String getJenkinsEmailCredsId(def script) {
        return getBindingValue(script, 'JENKINS_EMAIL_CREDS_ID')
    }

}
