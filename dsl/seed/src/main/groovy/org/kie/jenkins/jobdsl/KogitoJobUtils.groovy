package org.kie.jenkins.jobdsl

import groovy.json.JsonOutput

import org.kie.jenkins.jobdsl.model.Folder
import org.kie.jenkins.jobdsl.model.Environment
import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils

/**
* Job utils
**/
class KogitoJobUtils {

    static final Closure DEFAULT_PARAMS_GETTER = { script ->
        return getDefaultJobParams(script)
    }

    static def getDefaultJobParams(def script) {
        def jobParams = [
            job: [
                name: Utils.getRepoName(script)
            ],
            git: [
                author: Utils.getGitAuthor(script),
                branch: Utils.getGitBranch(script),
                repository: Utils.getRepoName(script),
                credentials: Utils.getGitAuthorCredsId(script),
                token_credentials: Utils.getGitAuthorTokenCredsId(script)
            ],
            parametersClosures: [],
            env: [
                REPO_NAME: Utils.getRepoName(script)
            ],
            pr: [
                excluded_regions: [
                    'LICENSE',
                    '\\.gitignore',
                    '.*\\.md',
                    '.*\\.adoc',
                    '.*\\.txt',
                    '\\.github/.*',
                    '\\.ci/jenkins/.*',
                    'docsimg/.*',
                ],
                ignore_for_labels: [ 'skip-ci' ],
            ]
        ]
        if (Utils.isProdEnvironment(script)) {
            jobParams.pr.ignore_for_labels.add(KogitoConstants.LABEL_DSL_TEST)
        }
        return jobParams
    }

    /**
    * Agrement default job params with some more information given as parameters of the method
    *
    * parameters:
    *   - jobName: Name of the job
    *   - jobFolder: Folder for the job to be created in
    *   - jenkinsfilePath: Path to the jenkinsfile on defined repository
    *   - jobDescription: (optional) Update the job description, if given
    *   - defaultJobParamsGetter: (optional) Closure to get the job default params
    */
    static def getBasicJobParams(def script, String jobName, Folder jobFolder, String jenkinsfilePath, String jobDescription = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        def jobParams = defaultJobParamsGetter(script)
        jobParams.job.name = jobName
        jobParams.job.folder = jobFolder
        jobParams.jenkinsfile = jenkinsfilePath
        jobParams.job.description = jobDescription ?: jobParams.job.description
        return jobParams
    }

    static def setupJobParamsSeedRepoEnv(def script, def jobParams) {
        jobParams.env = jobParams.env ?: [:]
        jobParams.env.putAll([
            SEED_REPO: Utils.getSeedRepo(script),
            SEED_AUTHOR: Utils.getSeedAuthor(script),
            SEED_BRANCH: Utils.getSeedBranch(script),
            SEED_AUTHOR_CREDS_ID: Utils.getSeedAuthorCredsId(script)
        ])
    }

    static def setupJobParamsDefaultJDKConfiguration(def script, def jobParams) {
        jobParams.env = jobParams.env ?: [:]
        jobParams.env.putAll([
            BUILD_JDK_TOOL: Utils.getJenkinsDefaultJDKTools(script),
        ])
    }

    static def setupJobParamsDefaultMavenConfiguration(def script, def jobParams) {
        jobParams.env = jobParams.env ?: [:]
        setupJobParamsDefaultJDKConfiguration(script, jobParams)
        jobParams.env.putAll([
            BUILD_MAVEN_TOOL: Utils.getJenkinsDefaultMavenTools(script),
        ])
    }

    /**
    * Seed job params are used for `common` jenkinsfiles which are taken from the seed
    **/
    static def getSeedJobParams(def script, String jobName, Folder jobFolder, String jenkinsfileName, String jobDescription = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        def jobParams = getBasicJobParams(script, jobName, jobFolder, Utils.getSeedJenkinsfilePath(script, jenkinsfileName), jobDescription, defaultJobParamsGetter)
        jobParams.git.repository = Utils.getSeedRepo(script)
        jobParams.git.author = Utils.getSeedAuthor(script)
        jobParams.git.branch = Utils.getSeedBranch(script)
        return jobParams
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
        def jobParams = getSeedJobParams(script, "update-${dependencyName.toLowerCase()}-${repository}", Folder.TOOLS, 'Jenkinsfile.tools.update-dependency-version', "Update ${dependencyName} version for ${repository}")
        KogitoJobUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)
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
        def jobParams = getSeedJobParams(script, 'update-quarkus-all', Folder.TOOLS, 'Jenkinsfile.ecosystem.update-quarkus-all', 'Update Quarkus version for the whole ecosystem')
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
        setupJobParamsSeedRepoEnv(script, jobParams)
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

    static List createAllEnvsPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, Environment.getActiveEnvironments(script), jobsRepoConfigGetter, defaultParamsGetter)
    }

    static List createPerEnvPerRepoPRJobs(def script, List<Environment> environments, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = DEFAULT_PARAMS_GETTER) {
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

    static List createDefaultPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.DEFAULT ], jobsRepoConfigGetter, defaultParamsGetter)
    }

    static def createNativePerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.NATIVE ], jobsRepoConfigGetter, defaultParamsGetter)
    }

    static def createMandrelPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.MANDREL ], jobsRepoConfigGetter, defaultParamsGetter)
    }

    static def createMandrelLTSPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.MANDREL_LTS ], jobsRepoConfigGetter, defaultParamsGetter)
    }

    static def createQuarkusMainPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.QUARKUS_MAIN ], jobsRepoConfigGetter, defaultParamsGetter)
    }

    static def createQuarkusBranchPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.QUARKUS_BRANCH ], jobsRepoConfigGetter, defaultParamsGetter)
    }

    /**
    * Create a Build-Chain Build&Test job in the current folder for the current repo.
    *
    * See also createBranchBuildChainJob(script, jobFolder, repository, ...)
    */
    static def createNightlyBuildChainBuildAndTestJobForCurrentRepo(def script, Folder jobFolder, boolean enableNotification = false, String notificationJobName = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createNightlyBuildChainBuildAndTestJobForCurrentRepoWithEnv(script, jobFolder, [:], enableNotification, notificationJobName, defaultJobParamsGetter)
    }

    /**
    * Create a Build-Chain Build&Test job in the current folder with an extra env for the current repo.
    *
    * See also createBranchBuildChainJob(script, jobFolder, repository, ...)
    */
    static def createNightlyBuildChainBuildAndTestJobForCurrentRepoWithEnv(def script, Folder jobFolder, Map extraEnv = [:], boolean enableNotification = false, String notificationJobName = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createNightlyBuildChainBuildAndTestJobWithEnv(script, jobFolder, Utils.getRepoName(script), extraEnv, enableNotification, notificationJobName, defaultJobParamsGetter)
    }

        /**
    * Create a Build-Chain Build&Test job in the current folder with an extra env.
    *
    * See also createBranchBuildChainJob(script, jobFolder, repository, ...)
    */
    static def createNightlyBuildChainBuildAndTestJobWithEnv(def script, Folder jobFolder, String repository, Map extraEnv = [:], boolean enableNotification = false, String notificationJobName = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createBranchBuildChainJob(script, jobFolder, repository, extraEnv, 'build-and-test', enableNotification, notificationJobName, defaultJobParamsGetter)
    }

    /**
    * Create a Build-Chain Build&Deploy job in the current folder for current repo.
    *
    * See also createBranchBuildChainJob(script, jobFolder, repository, ...)
    */
    static def createNightlyBuildChainBuildAndDeployJobForCurrentRepo(def script, Folder jobFolder, boolean enableNotification = false, String notificationJobName = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createNightlyBuildChainBuildAndDeployJobForCurrentRepoWithEnv(script, jobFolder, [:], enableNotification, notificationJobName, defaultJobParamsGetter)
    }

    /**
    * Create a Build-Chain Build&Deploy job in the current folder with an extra env for current repo.
    *
    * See also createBranchBuildChainJob(script, jobFolder, repository, ...)
    */
    static def createNightlyBuildChainBuildAndDeployJobForCurrentRepoWithEnv(def script, Folder jobFolder, Map extraEnv = [:], boolean enableNotification = false, String notificationJobName = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createNightlyBuildChainBuildAndDeployJobWithEnv(script, jobFolder, Utils.getRepoName(script), extraEnv, enableNotification, notificationJobName, defaultJobParamsGetter)
    }

    /**
    * Create a Build-Chain Build&Deploy job in the current folder with an extra env.
    *
    * See also createBranchBuildChainJob(script, jobFolder, repository, ...)
    */
    static def createNightlyBuildChainBuildAndDeployJobWithEnv(def script, Folder jobFolder, String repository, Map extraEnv = [:], boolean enableNotification = false, String notificationJobName = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        extraEnv.putAll([
            ENABLE_DEPLOY: true,
            MAVEN_DEPLOY_REPOSITORY: Utils.getMavenArtifactsUploadRepositoryUrl(script),
            MAVEN_DEPLOY_REPOSITORY_CREDS_ID: Utils.getMavenArtifactsUploadRepositoryCredentialsId(script),
        ])
        return createBranchBuildChainJob(script, jobFolder, repository, extraEnv, 'build-and-deploy', enableNotification, notificationJobName, defaultJobParamsGetter)
    }

    /**
    * Create a Build-Chain branch job in the given folder.
    *
    * parameters:
    *   - jobFolder: Folder for the job to be created in
    *   - repoName: Will be taken from environment if not given
    *   - enableDeploy: Whether deploy should be done after the build
    *   - enableNotification: Whether notification should be sent in case of unsuccessful pipeline
    *   - notificationJobName: Identifier for the notification stream
    *   - repoName: Will be taken from environment if not given
    *   - defaultJobParamsGetter: (optional) Closure to get the job default params
    */
    static def createBranchBuildChainJob(def script, Folder jobFolder, String repository, Map extraEnv = [:], String jobNameSuffix = '', boolean enableNotification = false, String notificationJobName = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        def jobParams = getSeedJobParams(script, "${repository}${jobNameSuffix ? ".${jobNameSuffix}" : ''}", jobFolder, KogitoConstants.BUILD_CHAIN_JENKINSFILE, "${jobNameSuffix} for ${repository} using the build-chain", defaultJobParamsGetter)
        KogitoJobUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)
        jobParams.triggers = [ cron : '@midnight' ]

        jobParams.parametersClosures.add({
            stringParam('DISPLAY_NAME', '', 'Setup a specific build display name')

            stringParam('GIT_BRANCH_NAME', Utils.getGitBranch(script), 'Set the Git branch to test')

            booleanParam('SKIP_TESTS', false, 'Skip tests')
            booleanParam('SKIP_INTEGRATION_TESTS', false, 'Skip IT tests')
        })
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),
            ENABLE_NOTIFICATION: enableNotification,
            NOTIFICATION_JOB_NAME: notificationJobName,

            GIT_AUTHOR: Utils.getGitAuthor(script),

            BUILDCHAIN_PROJECT: "kiegroup/${repository}",
            BUILDCHAIN_TYPE: 'branch',
            BUILDCHAIN_CONFIG_REPO: Utils.getBuildChainConfigRepo(script) ?: Utils.getSeedRepo(script),
            BUILDCHAIN_CONFIG_AUTHOR: Utils.getBuildChainConfigAuthor(script) ?: Utils.getSeedAuthor(script),
            BUILDCHAIN_CONFIG_BRANCH: Utils.getBuildChainConfigBranch(script) ?: Utils.getSeedBranch(script),
            BUILDCHAIN_CONFIG_FILE_PATH: Utils.getBuildChainConfigFilePath(script),

            MAVEN_SETTINGS_CONFIG_FILE_ID: Utils.getBindingValue(script, 'MAVEN_SETTINGS_FILE_ID'),
        ])

        // Extra overrides default
        jobParams.env.putAll(extraEnv)

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
