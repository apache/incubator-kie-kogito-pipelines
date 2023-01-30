package org.kie.jenkins.jobdsl

import groovy.json.JsonOutput

import org.kie.jenkins.jobdsl.model.Folder
import org.kie.jenkins.jobdsl.model.Environment
import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.utils.EnvUtils
import org.kie.jenkins.jobdsl.utils.JobParamsUtils
import org.kie.jenkins.jobdsl.utils.PrintUtils
import org.kie.jenkins.jobdsl.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils

/**
* Job utils
**/
class KogitoJobUtils {

    /**
    * *DEPRECATED* section
    * Should be deleted once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    *
    * Agrement default job params with some more information given as parameters of the method
    *
    * parameters:
    *   - jobName: Name of the job
    *   - jobFolder: Folder for the job to be created in
    *   - jenkinsfilePath: Path to the jenkinsfile on defined repository
    *   - jobDescription: (optional) Update the job description, if given
    *   - defaultJobParamsGetter: (optional) Closure to get the job default params
    */
    @Deprecated
    static def getBasicJobParams(def script, String jobName, Folder jobFolder, String jenkinsfilePath, String jobDescription = '', Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        def jobParams = defaultJobParamsGetter(script)
        jobParams.job.name = jobName
        jobParams.job.folder = jobFolder
        jobParams.jenkinsfile = jenkinsfilePath
        jobParams.job.description = jobDescription ?: jobParams.job.description
        return jobParams
    }

    /**
    * *DEPRECATED* section
    * Should be deleted once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    *
    * Seed job params are used for `common` jenkinsfiles which are taken from the seed
    **/
    @Deprecated
    static def getSeedJobParams(def script, String jobName, Folder jobFolder, String jenkinsfileName, String jobDescription = '', Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        def jobParams = getBasicJobParams(script, jobName, jobFolder, Utils.getSeedJenkinsfilePath(script, jenkinsfileName), jobDescription, defaultJobParamsGetter)
        jobParams.git.repository = Utils.getSeedRepo(script)
        jobParams.git.author = Utils.getSeedAuthor(script)
        jobParams.git.branch = Utils.getSeedBranch(script)
        return jobParams
    }

    /**
    * *DEPRECATED* section
    * Should be deleted once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    **/
    @Deprecated
    static def setupJobParamsDefaultMavenConfiguration(def script, def jobParams) {
        JobParamsUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)
    }

    /**
    *   See createVersionUpdateToolsJob(script, repository, dependencyName ...)
    *
    */
    static def createVersionUpdateToolsJobForCurrentRepo(def script, String dependencyName, def mavenUpdate = [:], def gradleUpdate = [:], def filepathReplaceRegex = [], def scriptCalls = []) {
        return createVersionUpdateToolsJob(script, Utils.getRepoName(script), dependencyName, mavenUpdate, gradleUpdate, filepathReplaceRegex, scriptCalls)
    }

    /**
    * Create a version update tools job
    *
    * @param repository Repository to update
    * @param dependencyName Name of the dependency which will be updated
    * @param mavenUpdate Maven update configuration
    *       .modules                  => Maven bom modules list to update with new version
    *       .compare_deps_remote_poms => Remote poms to compare dependencies with
    *       .properties               => Properties to update in the given modules
    * @param gradleUpdate Gradle update configuration
    *       .regex                    => Regex to update the version in build.gradle files
    * @param filepathReplaceRegex List of Filepath/Regex sed commands.
    *   For each element:
    *       .filepath                 => Filepath to update
    *       .regex                    => Regex to use in sed command
    * @param scriptCalls List of script calls string.
    *
    */
    static def createVersionUpdateToolsJob(def script, String repository, String dependencyName, def mavenUpdate = [:], def gradleUpdate = [:], def filepathReplaceRegex = [], def scriptCalls = []) {
        def jobParams = JobParamsUtils.getSeedJobParams(script, "update-${dependencyName.toLowerCase()}-${repository}", JobType.TOOLS, 'Jenkinsfile.tools.update-dependency-version', "Update ${dependencyName} version for ${repository}")
        JobParamsUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)
        // Setup correct checkout branch for pipelines
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),

            DEPENDENCY_NAME: "${dependencyName}",
            PROPERTIES_FILENAME: KogitoConstants.PIPELINE_PROPERTIES_FILENAME,

            PR_PREFIX_BRANCH: Utils.getGenerationBranch(script),

            BUILD_BRANCH_NAME: Utils.getGitBranch(script),
            GIT_AUTHOR:  Utils.getGitAuthor(script),
            AUTHOR_CREDS_ID: Utils.getGitAuthorCredsId(script),
        ])
        if (mavenUpdate) {
            mavenUpdate.modules ? jobParams.env.put('MAVEN_MODULES',  JsonOutput.toJson(mavenUpdate.modules)) : null
            mavenUpdate.compare_deps_remote_poms ? jobParams.env.put('MAVEN_COMPARE_DEPS_REMOTE_POMS', JsonOutput.toJson(mavenUpdate.compare_deps_remote_poms)) : null
            mavenUpdate.properties ? jobParams.env.put('MAVEN_PROPERTIES', JsonOutput.toJson(mavenUpdate.properties)) : null
        }
        if (gradleUpdate) {
            gradleUpdate.regex ? jobParams.env.put('GRADLE_REGEX', JsonOutput.toJson(gradleUpdate.regex)) : null
        }
        if (filepathReplaceRegex) {
            jobParams.env.put('FILEPATH_REPLACE_REGEX', JsonOutput.toJson(filepathReplaceRegex))
        }
        if (scriptCalls) {
            jobParams.env.put('SCRIPTS_CALLS', JsonOutput.toJson(scriptCalls))
        }
        def job = KogitoJobTemplate.createPipelineJob(script, jobParams)
        job?.with {
            parameters {
                stringParam('NEW_VERSION', '', 'Which version to set ?')
                stringParam('PR_BRANCH', '', '(Optional) Which PR branch name to use ? If none given, a name will be generated automatically.')
            }
        }
        return job
    }

    /**
    * Create a Quarkus update job which allow to update the quarkus version into current repository, via Maven or Gradle
    */
    static def createQuarkusVersionUpdateToolsJobForCurrentRepo(def script, def mavenUpdate = [:], def gradleUpdate = [:], def filepathReplaceRegex = [:]) {
        return createQuarkusUpdateToolsJob(script, Utils.getRepoName(script), 'Quarkus', mavenUpdate, gradleUpdate, filepathReplaceRegex)
    }

    /**
    * Create a Quarkus update job which allow to update the quarkus version into a repository, via Maven or Gradle
    */
    static def createQuarkusUpdateToolsJob(def script, String repository, def mavenUpdate = [:], def gradleUpdate = [:], def filepathReplaceRegex = [], def scriptCalls = []) {
        return createVersionUpdateToolsJob(script, repository, 'Quarkus', mavenUpdate, gradleUpdate, filepathReplaceRegex, scriptCalls)
    }

    /**
    * Create main quarkus update tools job which will update the quarkus version for the global ecosystem project
    * and will call the different projects `update-quarkus-{project}` jobs. Those should be best created with method `createQuarkusUpdateToolsJob`.
    */
    static def createMainQuarkusUpdateToolsJob(def script, List projectsToUpdate, List reviewers = []) {
        def jobParams = JobParamsUtils.getSeedJobParams(script, 'update-quarkus-all', JobType.TOOLS, 'Jenkinsfile.ecosystem.update-quarkus-all', 'Update Quarkus version for the whole ecosystem')
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),

            BUILD_BRANCH_NAME: Utils.getGitBranch(script),

            PROJECTS_TO_UPDATE: projectsToUpdate.join(','),
            REVIEWERS: reviewers.join(','),

            PROPERTIES_FILENAME: KogitoConstants.PIPELINE_PROPERTIES_FILENAME,

            GITHUB_TOKEN_CREDS_ID: Utils.getGitAuthorTokenCredsId(script),

            SEED_BRANCH_CONFIG_FILE_GIT_REPOSITORY: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_REPOSITORY'),
            SEED_BRANCH_CONFIG_FILE_GIT_AUTHOR_NAME: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_AUTHOR_NAME'),
            SEED_BRANCH_CONFIG_FILE_GIT_AUTHOR_CREDS_ID: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID'),
            SEED_BRANCH_CONFIG_FILE_GIT_BRANCH: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_BRANCH'),
            SEED_BRANCH_CONFIG_FILE_PATH: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_PATH'),
        ])
        JobParamsUtils.setupJobParamsSeedRepoEnv(script, jobParams)
        def job = KogitoJobTemplate.createPipelineJob(script, jobParams)
        job?.with {
            parameters {
                stringParam('NEW_VERSION', '', 'Which version to set ?')
                stringParam('PR_BRANCH', '', '(Optional) Which PR branch name to use ? If none given, a name will be generated automatically.')
            }
        }
        return job
    }

    /**
    * Create a quarkus platform update tools job
    *
    * @param project Project to update on the platform
    *
    */
    static def createQuarkusPlatformUpdateToolsJob(def script, String project) {
        def jobParams = JobParamsUtils.getSeedJobParams(script, "update-quarkus-platform-${project}", JobType.TOOLS, 'Jenkinsfile.update-quarkus-platform', "Update Quarkus platform with new version of ${project}")
        JobParamsUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),
            BUILD_BRANCH_NAME: Utils.getGitBranch(script),
            GIT_AUTHOR: Utils.getGitAuthor(script),
            GIT_AUTHOR_CREDENTIALS_ID: Utils.getGitAuthorCredsId(script),

            QUARKUS_PLATFORM_BRANCH: Utils.getGitQuarkusBranch(script),
            QUARKUS_PLATFORM_AUTHOR_NAME: Utils.getGitQuarkusAuthor(script),
            QUARKUS_PLATFORM_AUTHOR_CREDENTIALS_ID: Utils.getGitQuarkusAuthorCredsId(script),
            PROJECT_NAME: project,

            FORK_GIT_AUTHOR: Utils.getGitForkAuthorName(script),
            FORK_GIT_AUTHOR_CREDS_ID: Utils.getGitForkAuthorCredsId(script),
        ])
        def job = KogitoJobTemplate.createPipelineJob(script, jobParams)
        job?.with {
            parameters {
                stringParam('NEW_VERSION', '', 'Which version to set ?')

                stringParam('PR_BRANCH', '', '(Optional) Which PR branch name to use ? If none given, a name will be generated automatically.')

                choiceParam('COMMAND', ['stage', 'finalize'], 'Choose if you want to use staged artifacts or released artifacts.')
            }
        }
        return job
    }

    /**
    * *DEPRECATED* section
    * Should be deleted once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    */
    @Deprecated
    static List createAllEnvsPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        PrintUtils.deprecated(script, 'createAllEnvsPerRepoPRJobs', 'createAllEnvironmentsPerRepoPRJobs')
        return createPerEnvPerRepoPRJobs(script, Environment.getActiveEnvironments(script), jobsRepoConfigGetter, defaultParamsGetter)
    }

    /**
    * *DEPRECATED* section
    * Should be deleted once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    */
    @Deprecated
    static List createPerEnvPerRepoPRJobs(def script, List<Environment> environments, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        PrintUtils.deprecated(script, "createPerEnvPerRepoPRJobs for environments ${environments}", 'createPerEnvironmentPerRepoPRJobs')
        List allJobs = []

        Folder.getAllFoldersByJobTypeAndEnvironments(script, JobType.PULLREQUEST, environments)
            .findAll { folder -> folder.shouldAutoGenerateJobs() }
            .each { folder ->
                if (folder.environment.isOptional()) {
                    allJobs.addAll(KogitoJobTemplate.createPerRepoPRJobs(script, folder, getOptionalJobsRepoConfigClosure(jobsRepoConfigGetter), defaultParamsGetter))
                } else {
                    allJobs.addAll(KogitoJobTemplate.createPerRepoPRJobs(script, folder, jobsRepoConfigGetter, defaultParamsGetter))
                }
            }

        return allJobs
    }

    static List createAllEnvironmentsPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        PrintUtils.debug(script, 'createAllEnvironmentsPerRepoPRJobs')
        return createPerEnvironmentPerRepoPRJobs(script, EnvUtils.getAllAutoGeneratedEnvironments(script), jobsRepoConfigGetter, defaultParamsGetter)
    }

    static List createPerEnvironmentPerRepoPRJobs(def script, List<String> environments, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        PrintUtils.debug(script, "createPerEnvironmentPerRepoPRJobs for envs ${environments}")
        List allJobs = []

        // Generate default env
        allJobs.addAll(KogitoJobTemplate.createPerRepoPRJobs(script, '', jobsRepoConfigGetter, defaultParamsGetter))

        // Generate environments one
        environments.each { envName ->
            allJobs.addAll(KogitoJobTemplate.createPerRepoPRJobs(script, envName, getOptionalJobsRepoConfigClosure(jobsRepoConfigGetter), defaultParamsGetter))
        }

        return allJobs
    }

    /**
    * *DEPRECATED* section
    * Should be deleted once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    *
    * Create a Build-Chain Build&Test job in the current folder for the current repo.
    *
    * See also `createBranchBuildChainJob` method
    */
    @Deprecated
    static def createNightlyBuildChainBuildAndTestJobForCurrentRepo(def script, Folder jobFolder, boolean enableNotification = false, String notificationJobName = '', Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        return createNightlyBuildChainBuildAndTestJob(script, jobFolder, Utils.getRepoName(script), [:], enableNotification, notificationJobName, defaultJobParamsGetter)
    }

    /**
    * Create a Nightly Build-Chain Build&Test job for the given env for the current repository
    *
    * See also `createBranchBuildChainJob` method
    */
    static def createNightlyBuildChainBuildAndTestJobForCurrentRepo(def script, String envName = '', boolean enableNotification = false, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        return createNightlyBuildChainBuildAndTestJob(script, envName, Utils.getRepoName(script), [:], enableNotification, defaultJobParamsGetter)
    }

    /**
    * *DEPRECATED* section
    * Should be deleted once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    *
    * Create a Build-Chain Build&Test job in the current folder with an extra env.
    *
    * See also `createBranchBuildChainJob` method
    */
    @Deprecated
    static def createNightlyBuildChainBuildAndTestJob(def script, Folder jobFolder, String repository, Map extraEnv = [:], boolean enableNotification = false, String notificationJobName = '', Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        def jobParams = getSeedJobParams(script, "${repository}.build-and-test", jobFolder, KogitoConstants.BUILD_CHAIN_JENKINSFILE, "Build & Test for ${repository} using the build-chain", defaultJobParamsGetter)
        jobParams.triggers = [ cron : '@midnight' ] // To remove once environment nightlies are managed by main nightly pipeline
        return createBranchBuildChainJob(script, jobParams, repository, enableNotification, notificationJobName)
    }

    /**
    * Create a Nightly Build-Chain Build&Test job for the given env.
    *
    * See also `createBranchBuildChainJob` method
    */
    static def createNightlyBuildChainBuildAndTestJob(def script, String envName = '', String repository, Map extraEnv = [:], boolean enableNotification = false, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        def jobParams = JobParamsUtils.getSeedJobParamsWithEnv(script, "${repository}.build-and-test", JobType.NIGHTLY, envName, KogitoConstants.BUILD_CHAIN_JENKINSFILE, "Build & Test for ${repository} using the build-chain", defaultJobParamsGetter)
        jobParams.env.putAll(extraEnv)
        jobParams.triggers = [ cron : '@midnight' ] // To remove once environment nightlies are managed by main nightly pipeline
        return createBranchBuildChainJob(script, jobParams, repository, enableNotification, envName)
    }

    /**
    * *DEPRECATED* section
    * Should be deleted once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    *
    * Create a Build-Chain Build&Deploy job in the current folder for current repo.
    *
    * See also `createBranchBuildChainJob` method
    */
    @Deprecated
    static def createNightlyBuildChainBuildAndDeployJobForCurrentRepo(def script, Folder jobFolder, boolean enableNotification = false, String notificationJobName = '', Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        return createNightlyBuildChainBuildAndDeployJob(script, jobFolder, Utils.getRepoName(script), [:], enableNotification, notificationJobName, defaultJobParamsGetter)
    }

    /**
    * Create a Nightly Build-Chain Build&Deploy job for the given env for the current repository
    *
    * See also `createBranchBuildChainJob` method
    */
    static def createNightlyBuildChainBuildAndDeployJobForCurrentRepo(def script, String envName = '', boolean enableNotification = false, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        return createNightlyBuildChainBuildAndDeployJob(script, envName, Utils.getRepoName(script), [:], enableNotification, defaultJobParamsGetter)
    }

    /**
    * *DEPRECATED* section
    * Should be deleted once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    *
    * Create a Build-Chain Build&Deploy job in the current folder with an extra env.
    *
    * See also `createBranchBuildChainJob` method
    */
    @Deprecated
    static def createNightlyBuildChainBuildAndDeployJob(def script, Folder jobFolder, String repository, Map extraEnv = [:], boolean enableNotification = false, String notificationJobName = '', Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        def jobParams = getSeedJobParams(script, "${repository}.build-and-deploy", jobFolder, KogitoConstants.BUILD_CHAIN_JENKINSFILE, "Build & Test for ${repository} using the build-chain", defaultJobParamsGetter)
        jobParams.env.putAll([
            ENABLE_DEPLOY: true,
            MAVEN_DEPLOY_REPOSITORY: Utils.getMavenArtifactsUploadRepositoryUrl(script),
            MAVEN_DEPLOY_REPOSITORY_CREDS_ID: Utils.getMavenArtifactsUploadRepositoryCredentialsId(script),
        ])
        jobParams.env.putAll(extraEnv)
        return createBranchBuildChainJob(script, jobParams, repository, enableNotification, notificationJobName)
    }

    /**
    * Create a Nightly Build-Chain Build&Deploy job for the given env.
    *
    * See also `createBranchBuildChainJob` method
    */
    static def createNightlyBuildChainBuildAndDeployJob(def script, String envName = '', String repository, Map extraEnv = [:], boolean enableNotification = false, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        def jobParams = JobParamsUtils.getSeedJobParamsWithEnv(script, "${repository}.build-and-deploy", JobType.NIGHTLY, envName, KogitoConstants.BUILD_CHAIN_JENKINSFILE, "Build & Test for ${repository} using the build-chain", defaultJobParamsGetter)
        jobParams.env.putAll([
            ENABLE_DEPLOY: true,
            MAVEN_DEPLOY_REPOSITORY: Utils.getMavenArtifactsUploadRepositoryUrl(script),
            MAVEN_DEPLOY_REPOSITORY_CREDS_ID: Utils.getMavenArtifactsUploadRepositoryCredentialsId(script),
        ])
        jobParams.env.putAll(extraEnv)
        return createBranchBuildChainJob(script, jobParams, repository, enableNotification, envName)
    }

    /**
    * Create a Nightly job creating an integration branch when performing the build
    *
    * This job will call the build-chain with extra environment variables to allow for the creation of an integration branch
    *
    */
    static def createNightlyBuildChainIntegrationJob(def script, String envName, String repository, boolean enableNotification = false, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        def jobParams = JobParamsUtils.getSeedJobParamsWithEnv(script, "${repository}.integration", JobType.NIGHTLY, envName, KogitoConstants.BUILD_CHAIN_JENKINSFILE, "Integration with Quarkus for ${repository} using the build-chain", defaultJobParamsGetter)
        if (!envName) {
            throw new RuntimeException("Please provide a non-empty environment to generate an integration branch job...")
        }
        jobParams.env.putAll([
            INTEGRATION_BRANCH_CURRENT: "${Utils.getGenerationBranch(script)}-integration-${envName}",
            COMMIT_MESSAGE: "Update for ${envName} environment",
            GITHUB_USER: 'kie-ci',
        ])
        return createBranchBuildChainJob(script, jobParams, repository, enableNotification, envName)
    }

    /**
    * Create a Build-Chain branch job.
    *
    * parameters:
    *   - jobParams: Base job params (Please see `JobParamsUtils.getSeedJobParams*` to set them up)
    *   - repository: Which repository should be tested ?
    *   - enableNotification: Whether notification should be sent in case of unsuccessful pipeline
    *   - notificationJobName: Identifier for the notification stream
    */
    static def createBranchBuildChainJob(def script, def jobParams, String repository, boolean enableNotification = false, String notificationJobName = '') {
        JobParamsUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)
        JobParamsUtils.setupJobParamsBuildChainConfiguration(script, jobParams, repository, 'branch', notificationJobName)

        jobParams.parametersClosures.add({
            stringParam('DISPLAY_NAME', '', 'Setup a specific build display name')

            stringParam('GIT_BRANCH_NAME', Utils.getGitBranch(script), 'Set the Git branch to test')

            booleanParam('SKIP_TESTS', false, 'Skip tests')
            booleanParam('SKIP_INTEGRATION_TESTS', false, 'Skip IT tests')
        })
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),
            ENABLE_NOTIFICATION: enableNotification,

            GIT_AUTHOR: Utils.getGitAuthor(script),

            MAVEN_SETTINGS_CONFIG_FILE_ID: Utils.getBindingValue(script, 'MAVEN_SETTINGS_FILE_ID'),
        ])

        return KogitoJobTemplate.createPipelineJob(script, jobParams)
    }

    // Add optional information to per repo config
    private static Closure getOptionalJobsRepoConfigClosure(Closure jobsRepoConfigGetter) {
        return { jobFolder ->
            Map jobsRepoConfig = jobsRepoConfigGetter(jobFolder)
            jobsRepoConfig.optional = true
            jobsRepoConfig.jobs.each { job ->
                job.env = job.env ?: [:]
                job.env.DISABLE_SONARCLOUD = true
            }
            return jobsRepoConfig
        }
    }

}
