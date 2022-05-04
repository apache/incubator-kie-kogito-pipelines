
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

    static boolean isQuarkusMainEnvironmentEnabled(def script) {
        return getBindingValue(script, 'ENVIRONMENT_QUARKUS_MAIN_ENABLED').toBoolean()
    }

    static boolean isQuarkusBranchEnvironmentEnabled(def script) {
        return getBindingValue(script, 'ENVIRONMENT_QUARKUS_BRANCH_ENABLED').toBoolean()
    }

    static String getQuarkusEnvironmentBranchName(def script) {
        return getBindingValue(script, 'ENVIRONMENT_QUARKUS_BRANCH_NAME')
    }

    static boolean isNativeEnvironmentEnabled(def script) {
        return getBindingValue(script, 'ENVIRONMENT_NATIVE_ENABLED').toBoolean()
    }

    static boolean isMandrelEnvironmentEnabled(def script) {
        return getBindingValue(script, 'ENVIRONMENT_MANDREL_ENABLED').toBoolean()
    }

    static String getMandrelEnvironmentBuilderImage(def script) {
        return getBindingValue(script, 'ENVIRONMENT_MANDREL_BUILDER_IMAGE')
    }

    static boolean isRuntimesBDDEnvironmentEnabled(def script) {
        return getBindingValue(script, 'ENVIRONMENT_RUNTIMES_BDD_ENABLED').toBoolean()
    }

    static boolean isProductizedBranch(def script) {
        return getBindingValue(script, 'PRODUCTIZED_BRANCH').toBoolean()
    }

    static String getGenerationBranch(def script) {
        return getBindingValue(script, 'GENERATION_BRANCH')
    }

    static boolean isMainBranch(def script) {
        boolean result = getGitBranch(script) == getGitMainBranch(script)
        // script.println("Branch=${getGitBranch(script)}. Main Branch=${getGitMainBranch(script)}. Is main branch ? => ${result}")
        return result
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

    static String getPipelinesJenkinsfilePath(def script, String jenkinsfileName) {
        return "${getPipelinesJenkinsConfigPath(script)}/${jenkinsfileName}"
    }

    static String getPipelinesJenkinsConfigPath(def script) {
        return getJenkinsConfigPath(script, KogitoConstants.KOGITO_PIPELINES_REPOSITORY)
    }

    static String getPipelinesJenkinsHelperScript(def script) {
        return "${getPipelinesJenkinsConfigPath(script)}/scripts/helper.groovy"
    }

    static String getJenkinsEmailCredsId(def script) {
        return getBindingValue(script, 'JENKINS_EMAIL_CREDS_ID')
    }

    static String getSeedAuthor(def script) {
        return getBindingValue(script, 'SEED_AUTHOR')
    }

    static String getSeedBranch(def script) {
        return getBindingValue(script, 'SEED_BRANCH')
    }
    
    static boolean isNewFolderStructure(def script) {
        return getBindingValue(script, 'NEW_FOLDER_STRUCTURE').toBoolean()
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

}
