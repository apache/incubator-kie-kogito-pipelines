package org.kie.jenkins.jobdsl

import groovy.json.JsonOutput

import org.kie.jenkins.jobdsl.model.Folder
import org.kie.jenkins.jobdsl.model.Environment
import org.kie.jenkins.jobdsl.model.JobId
import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils
import org.kie.jenkins.jobdsl.VersionUtils

/**
* Job utils
**/
class KogitoJobUtils {

    static final Closure DEFAULT_PARAMS_GETTER = { script ->
        return getDefaultJobParams(script)
    }

    static final Closure CLOUD_ENABLED_FOLDER_FILTER = { folder ->
        return folder.environment?.isCloudEnabled()
    }

    /**
    * This create a default structure for job params with information taken from the DSL environment
    */
    static def getDefaultJobParams(def script) {
        return [
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
                ignore_for_labels: [ 'skip-ci', 'dsl-test' ],
            ]
        ]
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

    static def setupJobParamsSeedRepoEnv(def script, def jobParams) {
        jobParams.env = jobParams.env ?: [:]
        jobParams.env.putAll([
            SEED_REPO: Utils.getSeedRepo(script),
            SEED_AUTHOR: Utils.getSeedAuthor(script),
            SEED_BRANCH: Utils.getSeedBranch(script),
            SEED_AUTHOR_CREDS_ID: Utils.getSeedAuthorCredsId(script)
        ])
    }

    static def getCloudContainerEnv(String containerEngine, String containerTlsOptions, int maxRegistryRetries) {
        return [
            CONTAINER_ENGINE: containerEngine,
            CONTAINER_TLS_OPTIONS: containerTlsOptions,
            MAX_REGISTRY_RETRIES: maxRegistryRetries,
        ]
    }

    static def setupJobParamsCloudDockerContainerEnv(def script, def jobParams, int maxRegistryRetries = 3) {
        jobParams.env = jobParams.env ?: [:]
        jobParams.env.putAll(getCloudContainerEnv('docker', '', maxRegistryRetries))
    }

    static def setupJobParamsCloudPodmanContainerEnv(def script, def jobParams, int maxRegistryRetries = 3) {
        jobParams.env = jobParams.env ?: [:]
        jobParams.env.putAll(getCloudContainerEnv('podman', '--tls-verify=false', maxRegistryRetries))
    }

    static def setupJobParamsImageInfoParams(def script, def jobParams, String paramsPrefix = '') {
        jobParams.parametersClosures = jobParams.parametersClosures ?: []
        def jobFolder = jobParams.job.folder
        if (!jobFolder) {
            throw new RuntimeException('Folder is not defined. Cannot set properly the Image parameters...')
        }

        String prefix = paramsPrefix ? "${paramsPrefix.toUpperCase()}_" : ''
        String cloudImageRegistry = Utils.getCloudImageRegistry(script)
        String cloudImageNamespace = Utils.getCloudImageNamespace(script)
        String cloudImageRegistryCredentials = jobFolder.isRelease() ? Utils.getCloudImageRegistryCredentialsRelease(script) : Utils.getCloudImageRegistryCredentialsNightly(script)
        jobParams.parametersClosures.add({
            stringParam("${prefix}IMAGE_OPENSHIFT_API", '', "Setup the Openshift API if ${paramsPrefix} images need the openshift as registry/namespace")
            stringParam("${prefix}IMAGE_OPENSHIFT_CREDS_KEY", '', "Setup the Openshift API if ${paramsPrefix} images need to access the openshift API")
            stringParam("${prefix}IMAGE_REGISTRY_CREDENTIALS", "${cloudImageRegistryCredentials}", "Image registry credentials to use for ${paramsPrefix} images. Will be ignored if no IMAGE_REGISTRY is given")
            stringParam("${prefix}IMAGE_REGISTRY", "${cloudImageRegistry}", "Image registry to use for ${paramsPrefix} images")
            stringParam("${prefix}IMAGE_NAMESPACE", "${cloudImageNamespace}", "Image namespace to use for ${paramsPrefix} images")
            stringParam("${prefix}IMAGE_NAME_SUFFIX", '', "Image name suffix to use for ${paramsPrefix} images. In case you need to change the final image name, you can add a suffix to it.")
            stringParam("${prefix}IMAGE_TAG", '', "Image tag to use for ${paramsPrefix} images")
        })
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
    * Create an update version tools job for Maven/Gradle projects
    *
    * This will create a job which is accepting one parameter, aka NEW_VERSION, and will update the different Maven/Gradle information given as parameters.
    *
    * parameters:
    *   - repository
    *   - dependencyName: Which dependency are you updating ? Quarkus ? Other ?
    *   - mavenUpdate
    *       - modules: Maven modules to update
    *       - properties: Which properties to update
    *       - compare_deps_remote_poms: Which pom to compare to, to update the dependencies version
    *   - gradleUpdate
    *       - regex: Regex to use on `build.gradle` files to update the version
    *   - filepathReplaceRegex array of filepath/regex to perform using sed command
    */
    static def createVersionUpdateToolsJob(def script, String dependencyName, def mavenUpdate = [:], def gradleUpdate = [:], def filepathReplaceRegex = [:]) {
        String repository = Utils.getRepoName(script)
        def jobParams = getSeedJobParams(script, "update-${dependencyName.toLowerCase()}-${repository}", Folder.TOOLS, 'Jenkinsfile.tools.update-dependency-version', "Update ${dependencyName} version for ${repository}")
        KogitoJobUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)
        // Setup correct checkout branch for pipelines
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),

            DEPENDENCY_NAME: "${dependencyName}",

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
        def job = KogitoJobTemplate.createPipelineJob(script, jobParams)
        return job.with {
            parameters {
                stringParam('NEW_VERSION', '', 'Which version to set ?')
            }
        }
    }

    /**
    * Create a Quarkus version update tools job which allow to update the quarkus version into a repository, via Maven or Gradle
    * See also createVersionUpdateToolsJob
    */
    static def createQuarkusVersionUpdateToolsJob(def script, def mavenUpdate = [:], def gradleUpdate = [:], def filepathReplaceRegex = [:]) {
        return createVersionUpdateToolsJob(script, 'Quarkus', mavenUpdate, gradleUpdate, filepathReplaceRegex)
    }

    /**
    * Create main quarkus update tools job which will update the quarkus version for the global ecosystem project
    * and will call the different projects `update-quarkus-{project}` jobs. Those should be best created with method `createQuarkusUpdateToolsJob`.
    */
    static def createMainQuarkusUpdateToolsJob(def script, String notificationJobName, List projectsToUpdate) {
        def jobParams = getSeedJobParams(script, 'update-quarkus-all', Folder.TOOLS, 'Jenkinsfile.ecosystem.update-quarkus-all', 'Update Quarkus version for the whole ecosystem')
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),

            BUILD_BRANCH_NAME: Utils.getGitBranch(script),

            PROJECTS_TO_UPDATE: projectsToUpdate.join(','),

            SEED_BRANCH_CONFIG_FILE_GIT_REPOSITORY: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_REPOSITORY'),
            SEED_BRANCH_CONFIG_FILE_GIT_AUTHOR_NAME: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_AUTHOR_NAME'),
            SEED_BRANCH_CONFIG_FILE_GIT_AUTHOR_CREDS_ID: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID'),
            SEED_BRANCH_CONFIG_FILE_GIT_BRANCH: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_BRANCH'),
            SEED_BRANCH_CONFIG_FILE_PATH: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_PATH'),
        ])
        setupJobParamsSeedRepoEnv(script, jobParams)
        KogitoJobTemplate.createPipelineJob(script, jobParams)?.with {
            parameters {
                stringParam('NEW_VERSION', '', 'Which version to set ?')
            }
        }
    }

    /**
    * Create a per Repo PR jobs for all existing environments
    * See also `createPerEnvPerRepoPRJobs`
    */
    static List createAllEnvsPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, Environment.getActiveEnvironments(script), jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    /**
    * Create PR jobs per repository for the given environments
    *
    * parameters:
    *   - environments: List of environment for which the PR jobs must be created
    *   - jobsRepoConfigGetter: Closure to retrieve the repository config. The closure must accept the `Folder jobFolder` as parameter
    *   - defaultJobParamsGetter: (optional) Closure to get the job default params
    */
    static List createPerEnvPerRepoPRJobs(def script, List<Environment> environments, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        List allJobs = []

        applyInAllPullRequestFolders(script, environments) { folder ->
            allJobs.addAll(KogitoJobTemplate.createPerRepoPRJobs(script, folder, overrideJobsRepoConfigGetter(jobsRepoConfigGetter, folder), defaultJobParamsGetter))
        }

        return allJobs
    }

    /**
    * Add optional information to a per repo config
    */
    private static Closure overrideJobsRepoConfigGetter(Closure jobsRepoConfigGetter, Folder folder) {
        if (!folder.environment.isOptional()) {
            return jobsRepoConfigGetter
        }
        return { jobFolder ->
            Map jobsRepoConfig = jobsRepoConfigGetter(jobFolder)
            jobsRepoConfig.optional = true
            jobsRepoConfig.jobs.each { job ->
                job.env = job.env ?: [:]
                job.env.ENABLE_SONARCLOUD = false
            }
            return jobsRepoConfig
        }
    }

    /**
    * Create a per Repo PR jobs for the default environment
    * See also `createPerEnvPerRepoPRJobs`
    */
    static List createDefaultPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.DEFAULT ], jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    /**
    * Create a per Repo PR jobs for the native environment
    * See also `createPerEnvPerRepoPRJobs`
    */
    static def createNativePerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.NATIVE ], jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    /**
    * Create a per Repo PR jobs for the mandrel environment
    * See also `createPerEnvPerRepoPRJobs`
    */
    static def createMandrelPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.MANDREL ], jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    /**
    * Create a per Repo PR jobs for the quarkus main environment
    * See also `createPerEnvPerRepoPRJobs`
    */
    static def createQuarkusMainPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.QUARKUS_MAIN ], jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    /**
    * Create a per Repo PR jobs for the quarkus branch environment
    * See also `createPerEnvPerRepoPRJobs`
    */
    static def createQuarkusBranchPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.QUARKUS_BRANCH ], jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    /**
    * Create Build Chain Build&Test jobs for all needed environments (nightlies)
    * See also `createBuildChainBuildAndTestJob`
    */
    static List createAllEnvsBuildChainBuildAndTestJobs(def script, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvBuildChainBuildAndTestJobs(script, Environment.getActiveEnvironments(script), defaultJobParamsGetter)
    }

    /**
    * Create Build Chain Build&Test jobs for given environments (nightlies)
    * See also `createBuildChainBuildAndTestJob`
    */
    static List createPerEnvBuildChainBuildAndTestJobs(def script, List<Environment> environments, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        List allJobs = []

        // Nightlies
        applyInAllNightlyFolders(script, environments) { folder ->
            allJobs.add(createBuildChainBuildAndTestJob(script, folder, defaultJobParamsGetter))
        }

        // TODO release
        // if (!Utils.isMainBranch(this)) {

        return allJobs
        }

    /**
    * Create a Build-Chain Build&Test job in the given folder.
    *
    * parameters:
    *   - jobFolder: Folder for the job to be created in
    *   - repoName: Will be taken from environment if not given
    *   - defaultJobParamsGetter: (optional) Closure to get the job default params
    */
    static def createBuildChainBuildAndTestJob(def script, Folder jobFolder, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        String repository = Utils.getRepoName(script)

        def jobParams = getSeedJobParams(script, "${repository}.${JobId.BUILD_AND_TEST.toId()}", jobFolder, KogitoConstants.BUILD_CHAIN_JENKINSFILE, "Build & Test for ${repository} using the build-chain", defaultJobParamsGetter)
        KogitoJobUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)

        jobParams.parametersClosures.add({
            stringParam('DISPLAY_NAME', '', 'Setup a specific build display name')

            stringParam('GIT_BRANCH_NAME', Utils.getGitBranch(script), 'Set the Git branch to test')

            booleanParam('SKIP_TESTS', false, 'Skip tests')
            booleanParam('SKIP_INTEGRATION_TESTS', false, 'Skip IT tests') // TODO Optaweb skip integration testing ?
        })
        jobParams.env.putAll([
            GIT_AUTHOR: Utils.getGitAuthor(script),

            BUILDCHAIN_PROJECT: "kiegroup/${repository}",
            BUILDCHAIN_TYPE: 'branch',
            BUILDCHAIN_CONFIG_REPO: Utils.getSeedRepo(script),
            BUILDCHAIN_CONFIG_AUTHOR: Utils.getSeedAuthor(script),
            BUILDCHAIN_CONFIG_BRANCH: jobParams.git.branch,

            MAVEN_SETTINGS_CONFIG_FILE_ID: Utils.getBindingValue(script, 'MAVEN_SETTINGS_FILE_ID'),
        ])
        jobParams.env.put('MAVEN_DEPENDENCIES_REPOSITORY',
                    Utils.getBindingValue(script, jobFolder.isPullRequest() ? 'MAVEN_PR_CHECKS_REPOSITORY_URL' : 'MAVEN_ARTIFACTS_REPOSITORY'))

        return KogitoJobTemplate.createPipelineJob(script, jobParams)
    }

    /**
    * Create Deploy artifacts jobs for all needed environments (nightlies)
    * See also `createDeployArtifactsJob`
    */
    static List createAllEnvsMavenDeployArtifactsJobs(def script, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvMavenDeployArtifactsJobs(script, Environment.getActiveEnvironments(script), defaultJobParamsGetter)
    }

    /**
    * Create Deploy artifacts jobs for given environments (nightlies)
    * See also `createDeployArtifactsJob`
    */
    static List createPerEnvMavenDeployArtifactsJobs(def script, List<Environment> environments, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        List allJobs = []

        // Nightlies
        applyInAllNightlyFolders(script, environments) { folder ->
            allJobs.add(createMavenDeployArtifactsJob(script, folder, defaultJobParamsGetter))
        }

        return allJobs
    }

    /**
    * Create a deploy artifacts job
    *
    * parameters:
    *   - jobFolder: Folder for the job to be created in
    *   - defaultJobParamsGetter: (optional) Closure to get the job default params
    */
    static def createMavenDeployArtifactsJob(def script, Folder jobFolder, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        JobId jobId = JobId.DEPLOY_ARTIFACTS
        String repository = Utils.getRepoName(script)

        def jobParams = getSeedJobParams(script, "${repository}.${jobId.toId()}", jobFolder, 'Jenkinsfile.deploy-artifacts', "${Utils.allFirstLetterUpperCase(jobId.toId())} for ${repository}", defaultJobParamsGetter)
        KogitoJobUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)

        jobParams.env.putAll([
            GIT_AUTHOR: Utils.getGitAuthor(script),
            MAVEN_SETTINGS_CONFIG_FILE_ID: Utils.getBindingValue(script, 'MAVEN_SETTINGS_FILE_ID')
        ])

        jobParams.parametersClosures.add({
            stringParam('DISPLAY_NAME', '', 'Setup a specific build display name')
            stringParam('GIT_BRANCH_NAME', Utils.getGitBranch(script), 'Which branch to update ?')
            booleanParam('SKIP_TESTS', true, 'Skip tests when deploying')
        })

        if (jobFolder.isPullRequest()) {
            jobParams.env.putAll([
                MAVEN_DEPENDENCIES_REPOSITORY: Utils.getBindingValue(script, 'MAVEN_PR_CHECKS_REPOSITORY_URL'),
                MAVEN_DEPLOY_REPOSITORY: Utils.getBindingValue(script, 'MAVEN_PR_CHECKS_REPOSITORY_URL'),
                MAVEN_REPO_CREDS_ID: Utils.getBindingValue(script, 'MAVEN_PR_CHECKS_REPOSITORY_CREDS_ID'),
            ])
        } else {
            jobParams.env.putAll([
                MAVEN_DEPENDENCIES_REPOSITORY: Utils.getBindingValue(script, 'MAVEN_ARTIFACTS_REPOSITORY'),
                MAVEN_DEPLOY_REPOSITORY: Utils.getBindingValue(script, 'MAVEN_ARTIFACTS_REPOSITORY'),
            ])
            if (jobFolder.isRelease()) {
                jobParams.env.putAll([
                    NEXUS_RELEASE_URL: Utils.getBindingValue(script, 'MAVEN_NEXUS_RELEASE_URL'),
                    NEXUS_RELEASE_REPOSITORY_ID: Utils.getBindingValue(script, 'MAVEN_NEXUS_RELEASE_REPOSITORY'),
                    NEXUS_STAGING_PROFILE_ID: Utils.getBindingValue(script, 'MAVEN_NEXUS_STAGING_PROFILE_ID'),
                    NEXUS_BUILD_PROMOTION_PROFILE_ID: Utils.getBindingValue(script, 'MAVEN_NEXUS_BUILD_PROMOTION_PROFILE_ID'),
                ])
            }
        }

        return KogitoJobTemplate.createPipelineJob(script, jobParams)
    }

    /**
    * Create all needed jobs for a Maven artifacts repository
    *
    * parameters:
    *   - jobConfig: Allow to give some information to the created job Config
    *       - disabledEnvs: Array of Environment enums where the job should not be existing for all jobs
    *       - build: Build job configuration
    *           - disabled: Set to true if you want to disable it
    *           - envVars: Environment variables to add to the job
    *           - disabledEnvs: Array of Environment enums where the job should not be existing for build job
    *       - deploy: Deploy job configuration
    *           - disabled: Set to true if you want to disable it
    *           - envVars: Environment variables to add to the job
    *           - disabledEnvs: Array of Environment enums where the job should not be existing for deploy job
    *   - defaultJobParamsGetter: (optional) Closure to get the job default params
    */
    static def createAllJobsForMavenArtifactsRepository(def script, Map jobsConfig = [:], Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        if (!jobsConfig.build?.disabled) {
            def envs = filterOutFromActiveEnvironments(script, jobsConfig.disabledEnvs)
            createPerEnvBuildChainBuildAndTestJobs(script, filterOutFromEnvironments(script, envs, jobsConfig.build?.disabledEnvs), overrideDefaultParamsGetter(defaultJobParamsGetter, jobsConfig.build))
        }

        if (!jobsConfig.deploy?.disabled) {
            def envs = filterOutFromEnvironments(script, Environment.getMandatoryActiveEnvironments(script), jobsConfig.disabledEnvs)
            createPerEnvMavenDeployArtifactsJobs(script, filterOutFromEnvironments(script, envs, jobsConfig.deploy?.disabledEnvs), overrideDefaultParamsGetter(defaultJobParamsGetter, jobsConfig.deploy))
        }
    }

    /**
    * Create Promote images jobs for all needed environments (nightlies)
    * See also `createCloudDeployImagesJob`
    */
    static List createAllEnvsCloudPromoteImagesJobs(def script, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return createPerEnvCloudPromoteImagesJobs(script, Environment.getActiveEnvironments(script), defaultJobParamsGetter)
    }

    /**
    * Create Promote images jobs for given environments (nightlies)
    * See also `createCloudDeployImagesJob`
    */
    static List createPerEnvCloudPromoteImagesJobs(def script, List<Environment> environments, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        List allJobs = []

        // Nightlies
        applyInAllNightlyFolders(script, environments, { folder ->
                allJobs.add(createCloudPromoteImagesJob(script, folder, defaultJobParamsGetter))
            }, CLOUD_ENABLED_FOLDER_FILTER)

        return allJobs
    }

    /**
    * Create a promote images job
    *
    * parameters:
    *   - jobFolder: Folder for the job to be created in
    *   - defaultJobParamsGetter: (optional) Closure to get the job default params
    */
    static def createCloudPromoteImagesJob(def script, Folder jobFolder, Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        JobId jobId = JobId.PROMOTE_IMAGES
        String repository = Utils.getRepoName(script)

        def jobParams = getSeedJobParams(script, "${repository}.${jobId.toId()}", jobFolder, 'Jenkinsfile.promote-images', "${Utils.allFirstLetterUpperCase(jobId.toId())} for ${repository}", defaultJobParamsGetter)
        setupJobParamsCloudPodmanContainerEnv(script, jobParams)

        jobParams.env.putAll([
            CI: true,
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),

            GIT_AUTHOR: Utils.getGitAuthor(script),

        // Documentation part, should not uncomment
        // DEFAULT_IMAGE_NAMES: '', // Hardcoded default image names
        // DEFAULT_IMAGE_NAMES_SHELL_SCRIPT: '', // Shell script to retrieve the image names
        ])

        setupJobParamsImageInfoParams(script, jobParams, KogitoConstants.CLOUD_IMAGE_BASE_PARAMS_PREFIX)
        setupJobParamsImageInfoParams(script, jobParams, KogitoConstants.CLOUD_IMAGE_PROMOTE_PARAMS_PREFIX)
        jobParams.parametersClosures.add({
            stringParam('DISPLAY_NAME', '', 'Setup a specific build display name')
            stringParam('GIT_BRANCH_NAME', Utils.getGitBranch(script), 'Set the Git branch to checkout')

            stringParam('BUILD_IMAGES_JOB_URL', '', 'Url to a job in which job properties contains the image names.')
            stringParam('IMAGE_NAMES', '', 'Comma-separated image names to promote.')
        })

        return KogitoJobTemplate.createPipelineJob(script, jobParams)
    }

    private static Closure overrideDefaultParamsGetter(Closure defaultJobParamsGetter, Map jobConfig) {
        if (!jobConfig) {
            return defaultJobParamsGetter
        }
        return { script ->
            def jobParams = defaultJobParamsGetter(script)
            if (jobConfig.envVars) {
                jobParams.env.putAll(jobConfig.envVars)
            }
            return jobParams
        }
    }

    /**
    * Apply the given closure (accepting a folder as parameter) to all nightly folders
    */
    static void applyInAllNightlyFolders(def script, Closure applyInFolderClosure, Closure folderFilterClosure = null) {
        applyInAllNightlyFolders(script, Environment.getActiveEnvironments(script), applyInFolderClosure, folderFilterClosure)
    }

    /**
    * Apply the given closure (accepting a folder as parameter) to all nightly folders
    */
    static void applyInAllCloudNightlyFolders(def script, Closure applyInFolderClosure) {
        applyInAllNightlyFolders(script, Environment.getActiveEnvironments(script), applyInFolderClosure, CLOUD_ENABLED_FOLDER_FILTER)
    }

    /**
    * Apply the given closure (accepting a folder as parameter) to nightly folders from given enviroments
    */
    static void applyInAllNightlyFolders(def script, List<Environment> environments, Closure applyInFolderClosure, Closure folderFilterClosure = null) {
        applyInAllFolders(script, JobType.NIGHTLY, environments, applyInFolderClosure, folderFilterClosure)
    }

    /**
    * Apply the given closure (accepting a folder as parameter) to all pull request folders
    */
    static void applyInAllPullRequestFolders(def script, Closure applyInFolderClosure) {
        applyInAllPullRequestFolders(script, Environment.getActiveEnvironments(script), applyInFolderClosure)
    }

    /**
    * Apply the given closure (accepting a folder as parameter) to all nightly folders
    */
    static void applyInAllCloudPullRequestFolders(def script, Closure applyInFolderClosure) {
        applyInAllPullRequestFolders(script, Environment.getActiveEnvironments(script), applyInFolderClosure, CLOUD_ENABLED_FOLDER_FILTER)
    }

    /**
    * Apply the given closure (accepting a folder as parameter) to pull request folders from given enviroments
    */
    static void applyInAllPullRequestFolders(def script, List<Environment> environments, Closure applyInFolderClosure, Closure folderFilterClosure = null) {
        applyInAllFolders(script, JobType.PULLREQUEST, environments, applyInFolderClosure, folderFilterClosure)
    }

    /**
    * Apply the given closure (accepting a folder as parameter) to all folder from the given job type and with the given enviroments
    */
    static void applyInAllFolders(def script, JobType jobType, List<Environment> environments, Closure applyInFolderClosure, Closure folderFilterClosure = null) {
        def folders = Folder.getAllFoldersByJobTypeAndEnvironments(script, jobType, environments)
            .findAll { folder -> folder.shouldAutoGenerateJobs() }

        if (folderFilterClosure != null) {
            folders = folders.findAll(folderFilterClosure)
        }

        folders.each { folder -> applyInFolderClosure(folder) }
    }

    static List filterOutFromActiveEnvironments(def script, List disabledEnvs = null) {
        return filterOutFromEnvironments(script, Environment.getActiveEnvironments(script), disabledEnvs)
    }

    static List filterOutFromEnvironments(def script, List environments, List disabledEnvs = null) {
        List envs = environments
        if (disabledEnvs) {
            envs.retainAll { !disabledEnvs.contains(it) }
        }
        return envs
    }

    }
