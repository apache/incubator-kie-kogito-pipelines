package org.kie.jenkins.jobdsl.model

import org.kie.jenkins.jobdsl.Utils

class Folder {

    String name

    JobType jobType
    Environment environment
    boolean disableAutoGeneration

    Map configValues
    Map defaultEnv

    String getFolderName(String separator = '.') {
        String folderName = this.jobType.toName()
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

    boolean isMandrelLTS() {
        return this.environment == Environment.MANDREL_LTS
    }

    boolean isQuarkusMain() {
        return this.environment == Environment.QUARKUS_MAIN
    }

    boolean isQuarkusBranch() {
        return this.environment == Environment.QUARKUS_BRANCH
    }

    boolean isQuarkusLTS() {
        return this.environment == Environment.QUARKUS_LTS
    }

    // A folder is active if jobType AND environment are active
    boolean isActive(def script) {
        return this.jobType.isActive(script) && this.environment.isActive(script)
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Folder Management

    /**
     * @deprecated Please use SETUP_BRANCH instead
     */
    @Deprecated
    public static final Folder INIT_BRANCH = new Folder(
        name: 'INIT_BRANCH',
        jobType: JobType.INIT_BRANCH,
        environment: Environment.DEFAULT,
    )

    public static final Folder SETUP_BRANCH = new Folder(
        name: 'SETUP_BRANCH',
        jobType: JobType.SETUP_BRANCH,
        environment: Environment.DEFAULT,
    )

    public static final Folder NIGHTLY = new Folder(
        name: 'NIGHTLY',
        jobType: JobType.NIGHTLY,
        environment: Environment.DEFAULT,
    )

    public static final Folder NIGHTLY_SONARCLOUD = new Folder(
        name: 'NIGHTLY_SONARCLOUD',
        jobType: JobType.NIGHTLY,
        environment: Environment.SONARCLOUD,
    )

    public static final Folder NIGHTLY_NATIVE = new Folder(
        name: 'NIGHTLY_NATIVE',
        jobType: JobType.NIGHTLY,
        environment: Environment.NATIVE,
    )

    public static final Folder NIGHTLY_MANDREL = new Folder(
        name: 'NIGHTLY_MANDREL',
        jobType: JobType.NIGHTLY,
        environment: Environment.MANDREL,
    )

    public static final Folder NIGHTLY_MANDREL_LTS = new Folder(
        name: 'NIGHTLY_MANDREL_LTS',
        jobType: JobType.NIGHTLY,
        environment: Environment.MANDREL_LTS,
    )

    public static final Folder NIGHTLY_QUARKUS_MAIN = new Folder(
        name: 'NIGHTLY_QUARKUS_MAIN',
        jobType: JobType.NIGHTLY,
        environment: Environment.QUARKUS_MAIN,
    )

    public static final Folder NIGHTLY_QUARKUS_BRANCH = new Folder(
        name: 'NIGHTLY_QUARKUS_BRANCH',
        jobType: JobType.NIGHTLY,
        environment: Environment.QUARKUS_BRANCH,
    )

    public static final Folder NIGHTLY_QUARKUS_LTS = new Folder(
        name: 'NIGHTLY_QUARKUS_LTS',
        jobType: JobType.NIGHTLY,
        environment: Environment.QUARKUS_LTS,
    )

    // TEMPORARY FIX for Optaplanner getting out of Kogito train
    public static final Folder NIGHTLY_ECOSYSTEM = new Folder(
        jobType: JobType.NIGHTLY,
        environment: Environment.ECOSYSTEM,
        disableAutoGeneration: true
    )

    public static final Folder PULLREQUEST = new Folder(
        name: 'PULLREQUEST',
        jobType: JobType.PULLREQUEST,
        environment: Environment.DEFAULT,
        configValues: [ // Setup config values for backward compatibility in job naming
            typeId: 'tests',
            typeName: 'build'
        ],
    )
    public static final Folder PULLREQUEST_NATIVE = new Folder(
        name: 'PULLREQUEST_NATIVE',
        jobType: JobType.PULLREQUEST,
        environment: Environment.NATIVE,
    )

    public static final Folder PULLREQUEST_MANDREL = new Folder(
        name: 'PULLREQUEST_MANDREL',
        jobType: JobType.PULLREQUEST,
        environment: Environment.MANDREL,
    )

    public static final Folder PULLREQUEST_MANDREL_LTS = new Folder(
        name: 'PULLREQUEST_MANDREL_LTS',
        jobType: JobType.PULLREQUEST,
        environment: Environment.MANDREL_LTS,
    )

    public static final Folder PULLREQUEST_QUARKUS_MAIN = new Folder(
        name: 'PULLREQUEST_QUARKUS_MAIN',
        jobType: JobType.PULLREQUEST,
        environment: Environment.QUARKUS_MAIN,
    )

    public static final Folder PULLREQUEST_QUARKUS_BRANCH = new Folder(
        name: 'PULLREQUEST_QUARKUS_BRANCH',
        jobType: JobType.PULLREQUEST,
        environment: Environment.QUARKUS_BRANCH,
    )

    public static final Folder PULLREQUEST_QUARKUS_LTS = new Folder(
        name: 'PULLREQUEST_QUARKUS_LTS',
        jobType: JobType.PULLREQUEST,
        environment: Environment.QUARKUS_LTS,
    )

    public static final Folder PULLREQUEST_RUNTIMES_BDD = new Folder(
        name: 'PULLREQUEST_RUNTIMES_BDD',
        jobType: JobType.PULLREQUEST,
        environment: Environment.KOGITO_BDD,
        disableAutoGeneration: true
    )

    public static final Folder RELEASE = new Folder(
        name: 'RELEASE',
        jobType: JobType.RELEASE,
        environment: Environment.DEFAULT,
        defaultEnv: [
            RELEASE: 'true'
        ],
    )

    public static final Folder TOOLS = new Folder(
        name: 'TOOLS',
        jobType: JobType.TOOLS,
        environment: Environment.DEFAULT,
    )

    public static final Folder OTHER = new Folder(
        name: 'OTHER',
        jobType: JobType.OTHER,
        environment: Environment.DEFAULT,
    )

    private static Set<Folder> FOLDERS = [
        INIT_BRANCH,
        SETUP_BRANCH,
        NIGHTLY,
        NIGHTLY_SONARCLOUD,
        NIGHTLY_NATIVE,
        NIGHTLY_MANDREL,
        NIGHTLY_MANDREL_LTS,
        NIGHTLY_QUARKUS_MAIN,
        NIGHTLY_QUARKUS_BRANCH,
        NIGHTLY_QUARKUS_LTS,
        PULLREQUEST,
        PULLREQUEST_NATIVE,
        PULLREQUEST_MANDREL,
        PULLREQUEST_MANDREL_LTS,
        PULLREQUEST_QUARKUS_MAIN,
        PULLREQUEST_QUARKUS_BRANCH,
        PULLREQUEST_QUARKUS_LTS,
        PULLREQUEST_RUNTIMES_BDD,
        RELEASE, TOOLS, OTHER
    ]

    static void register(Folder folder) {
        FOLDERS.add(folder)
    }

    static List getAllRegistered() {
        return new ArrayList(FOLDERS)
    }

    static Folder getByName(String name) {
        return FOLDERS.find { it.name == name }
    }

    static List<Folder> getAllFolders(def script) {
        return getAllInitBranchFolders(script) +
            getAllSetupBranchFolders(script) +
            getAllNightlyFolders(script) +
            getAllPullRequestFolders(script) +
            getAllReleaseFolders(script) +
            getAllToolsFolders(script) +
            getAllOtherFolders(script)
    }

    static List<Folder> getAllActiveFolders(def script) {
        return getAllRegistered().findAll { folder -> folder.isActive(script) }
    }

    /**
     * @deprecated Please use getAllSetupBranchFolders instead
     */
    @Deprecated
    static List<Folder> getAllInitBranchFolders(def script) {
        return getAllFoldersByJobTypeAndEnvironments(script, JobType.INIT_BRANCH, Environment.getActiveEnvironments(script))
    }

    static List<Folder> getAllSetupBranchFolders(def script) {
        return getAllFoldersByJobTypeAndEnvironments(script, JobType.SETUP_BRANCH, Environment.getActiveEnvironments(script))
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
