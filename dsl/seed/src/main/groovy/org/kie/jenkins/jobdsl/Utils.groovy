
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

    static boolean isEnvironmentQuarkusMainEnabled(def script) {
        return getBindingValue(script, 'ENVIRONMENT_QUARKUS_MAIN_ENABLED').toBoolean()
    }

    static boolean isEnvironmentQuarkusBranchEnabled(def script) {
        return getBindingValue(script, 'ENVIRONMENT_QUARKUS_BRANCH_ENABLED').toBoolean()
    }

    static String getEnvironmentQuarkusBranchVersion(def script) {
        return getBindingValue(script, 'ENVIRONMENT_QUARKUS_BRANCH_VERSION')
    }

    static boolean isEnvironmentQuarkusLTSEnabled(def script) {
        return getBindingValue(script, 'ENVIRONMENT_QUARKUS_LTS_ENABLED').toBoolean()
    }

    static String getEnvironmentQuarkusLTSVersion(def script) {
        return getBindingValue(script, 'ENVIRONMENT_QUARKUS_LTS_VERSION')
    }

    static boolean isEnvironmentNativeEnabled(def script) {
        return getBindingValue(script, 'ENVIRONMENT_NATIVE_ENABLED').toBoolean()
    }

    static boolean isEnvironmentMandrelEnabled(def script) {
        return getBindingValue(script, 'ENVIRONMENT_MANDREL_ENABLED').toBoolean()
    }

    static String getEnvironmentMandrelBuilderImage(def script) {
        return getBindingValue(script, 'ENVIRONMENT_MANDREL_BUILDER_IMAGE')
    }

    static boolean isEnvironmentRuntimesBDDEnabled(def script) {
        return getBindingValue(script, 'ENVIRONMENT_RUNTIMES_BDD_ENABLED').toBoolean()
    }

    static boolean isProductizedBranch(def script) {
        return getBindingValue(script, 'PRODUCTIZED_BRANCH').toBoolean()
    }

    static String getGenerationBranch(def script) {
        return getBindingValue(script, 'GENERATION_BRANCH')
    }

    static boolean isMainBranch(def script) {
        boolean result = getGenerationBranch(script) == getGitMainBranch(script)
        // script.println("Branch=${getGenerationBranch(script)}. Main Branch=${getGitMainBranch(script)}. Is main branch ? => ${result}")
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

    static String getSeedRepo(def script) {
        return getBindingValue(script, 'SEED_REPO')
    }

    static String getSeedAuthor(def script) {
        return getBindingValue(script, 'SEED_AUTHOR')
    }

    static String getSeedAuthorCredsId(def script) {
        return getBindingValue(script, 'SEED_AUTHOR_CREDS_ID')
    }

    static String getSeedBranch(def script) {
        return getBindingValue(script, 'SEED_BRANCH')
    }

    static String getSeedJenkinsfilePath(def script, String jenkinsfileName) {
        return "${KogitoConstants.SEED_JENKINSFILES_PATH}/${jenkinsfileName}"
    }

    static boolean isOldFolderStructure(def script) {
        return getBindingValue(script, 'OLD_FOLDER_STRUCTURE')?.toBoolean()
    }

    static String getCloudImageRegistry(def script) {
        return getBindingValue(script, 'CLOUD_IMAGE_REGISTRY')
    }

    static String getCloudImageNamespace(def script) {
        return getBindingValue(script, 'CLOUD_IMAGE_NAMESPACE')
    }

    static String getCloudImageRegistryCredentialsNightly(def script) {
        return getBindingValue(script, 'CLOUD_IMAGE_REGISTRY_CREDENTIALS_NIGHTLY')
    }

    static String getCloudImageRegistryCredentialsRelease(def script) {
        return getBindingValue(script, 'CLOUD_IMAGE_REGISTRY_CREDENTIALS_RELEASE')
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
