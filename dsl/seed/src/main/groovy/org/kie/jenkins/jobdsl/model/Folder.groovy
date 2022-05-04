package org.kie.jenkins.jobdsl.model

import org.kie.jenkins.jobdsl.Utils

enum Folder {

    NIGHTLY(
        jobType: JobType.NIGHTLY,
        environment: Environment.DEFAULT,
    ),
    NIGHTLY_SONARCLOUD(
        jobType: JobType.NIGHTLY,
        environment: Environment.SONARCLOUD,
    ),
    NIGHTLY_NATIVE(
        jobType: JobType.NIGHTLY,
        environment: Environment.NATIVE,
    ),
    NIGHTLY_MANDREL(
        jobType: JobType.NIGHTLY,
        environment: Environment.MANDREL,
    ),
    NIGHTLY_QUARKUS_MAIN(
        jobType: JobType.NIGHTLY,
        environment: Environment.QUARKUS_MAIN,
    ),
    NIGHTLY_QUARKUS_BRANCH(
        jobType: JobType.NIGHTLY,
        environment: Environment.QUARKUS_BRANCH,
    ),

    PULLREQUEST(
        jobType: JobType.PULLREQUEST,
        environment: Environment.DEFAULT,
        configValues: [ // Setup config values for backward compatibility in job naming
            typeId: 'tests',
            typeName: 'build'
        ],
    ),
    PULLREQUEST_NATIVE(
        jobType: JobType.PULLREQUEST,
        environment: Environment.NATIVE,
    ),
    PULLREQUEST_MANDREL(
        jobType: JobType.PULLREQUEST,
        environment: Environment.MANDREL,
    ),
    PULLREQUEST_QUARKUS_MAIN(
        jobType: JobType.PULLREQUEST,
        environment: Environment.QUARKUS_MAIN,
    ),
    PULLREQUEST_QUARKUS_BRANCH(
        jobType: JobType.PULLREQUEST,
        environment: Environment.QUARKUS_BRANCH,
    ),
    PULLREQUEST_RUNTIMES_BDD(
        jobType: JobType.PULLREQUEST,
        environment: Environment.KOGITO_BDD,
        disableAutoGeneration: true
    ),

    RELEASE(
        jobType: JobType.RELEASE,
        environment: Environment.DEFAULT,
        defaultEnv: [
            RELEASE: 'true'
        ],
    ),

    // UPDATE_VERSION(
    //     jobType: JobType.UPDATE_VERSION,
    //     environment: Environment.DEFAULT,
    // ),

    TOOLS(
        jobType: JobType.TOOLS,
        environment: Environment.DEFAULT,
    ),
    OTHER(
        jobType: JobType.OTHER,
        environment: Environment.DEFAULT,
    )

    JobType jobType
    Environment environment
    boolean disableAutoGeneration

    Map configValues
    Map defaultEnv

    String getFolderName(String separator = '.') {
        String folderName = this.jobType.toFolderName()
        if (this.environment != Environment.DEFAULT) {
            folderName += "${separator}${environment.toId()}"
        }
        return folderName
    }

    Map getDefaultEnvVars(def script) {
        Map defaultEnv = this.environment?.getDefaultEnvVars(script)
        defaultEnv.putAll(this.defaultEnv ?: [:])
        return defaultEnv
    }

    Map getConfigValues(def script) {
        return this.configValues ?: [:]
    }

    boolean shouldAutoGenerateJobs() {
        return !this.disableAutoGeneration
    }

    boolean isNightly() {
        return this.jobType == JobType.NIGHTLY
    }

    boolean isRelease() {
        return this.jobType == JobType.RELEASE
    }

    boolean isPullRequest() {
        return this.jobType == JobType.PULLREQUEST
    }

    // boolean isUpdateVersion() {
    //     return this.jobType == JobType.UPDATE_VERSION
    // }

    boolean isTools() {
        return this.jobType == JobType.TOOLS
    }

    boolean isOther() {
        return this.jobType == JobType.OTHER
    }

    boolean isSonarCloud() {
        return this.environment == Environment.SONARCLOUD
    }

    boolean isNative() {
        return this.environment == Environment.NATIVE
    }

    boolean isMandrel() {
        return this.environment == Environment.MANDREL
    }

    boolean isQuarkusMain() {
        return this.environment == Environment.QUARKUS_MAIN
    }

    boolean isQuarkusBranch() {
        return this.environment == Environment.QUARKUS_BRANCH
    }

    // A folder is active if
    // - its job type is NOT optional (not matter the environment)
    // OR
    // - it is active and the environment is also active
    boolean isActive(def script) {
        // script.println("isActive call for folder ${this.name()}")
        // script.println("isActive call for folder ${this.name()}: jobtype name ? ${this.jobType.name()}")
        // script.println("isActive call for folder ${this.name()}: jobtype optional ? ${this.jobType.isOptional()}")
        // script.println("isActive call for folder ${this.name()}: jobtype active ? ${this.jobType.isActive(script)}")
        // script.println("isActive call for folder ${this.name()}: environment name ? ${this.environment.name()}")
        // script.println("isActive call for folder ${this.name()}: environment active ? ${this.environment.isActive(script)}")
        return !this.jobType.isOptional() || (this.jobType.isActive(script) && this.environment.isActive(script))
    }

    static List<Folder> getAllFolders(def script) {
        return getAllNightlyFolders(script) +
            getAllPullRequestFolders(script) +
            getAllReleaseFolders(script) +
            getAllToolsFolders(script) +
            getAllOtherFolders(script)
    // Folder.UPDATE_VERSION,
    }

    static List<Folder> getAllActiveFolders(def script) {
        return Folder.values().findAll { folder -> folder.isActive(script) }
    }

    static List<Folder> getAllNightlyFolders(def script) {
        return getAllFoldersByJobTypeAndEnvironments(script, JobType.NIGHTLY, Environment.getActiveEnvironments(script))
    }

    static List<Folder> getAllPullRequestFolders(def script) {
        return getAllFoldersByJobTypeAndEnvironments(script, JobType.PULLREQUEST, Environment.getActiveEnvironments(script))
    }

    static List<Folder> getAllReleaseFolders(def script) {
        return getAllFoldersByJobTypeAndEnvironments(script, JobType.RELEASE, Environment.getActiveEnvironments(script))
    }

    static List<Folder> getAllToolsFolders(def script) {
        return getAllFoldersByJobTypeAndEnvironments(script, JobType.TOOLS, Environment.getActiveEnvironments(script))
    }

    static List<Folder> getAllOtherFolders(def script) {
        return getAllFoldersByJobTypeAndEnvironments(script, JobType.OTHER, Environment.getActiveEnvironments(script))
    }

    static List<Folder> getAllFoldersByJobTypeAndEnvironments(def script, JobType jobType, List<Environment> environments) {
        return getAllActiveFolders(script).findAll { folder -> folder.jobType == jobType && environments.any { env -> env == folder.environment } }
    }

}
