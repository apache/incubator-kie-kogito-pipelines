package org.kie.jenkins.jobdsl.utils

import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.model.JenkinsFolderRegistry
import org.kie.jenkins.jobdsl.utils.PrintUtils
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils

class JobParamsUtils {

    static final Closure DEFAULT_PARAMS_GETTER = { script ->
        return getDefaultJobParams(script)
    }

    static def getDefaultJobParams(def script, String repository = '') {
        repository = repository ?: Utils.getRepoName(script)
        def jobParams = [
            job: [
                name: repository,
            ],
            git: [
                author: Utils.getGitAuthor(script),
                branch: Utils.getGitBranch(script),
                repository: repository,
                credentials: Utils.getGitAuthorCredsId(script),
                token_credentials: Utils.getGitAuthorTokenCredsId(script)
            ],
            parametersClosures: [],
            env: [
                REPO_NAME: repository,
            ],
            pr: [
                target_repository: repository,
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
    *   - jobType: Job Type for the job to be created from
    *   - jenkinsfilePath: Path to the jenkinsfile on defined repository
    *   - jobDescription: (optional) Update the job description, if given
    *   - defaultJobParamsGetter: (optional) Closure to get the job default params
    */
    static def getBasicJobParams(def script, String jobName, JobType jobType, String jenkinsfilePath, String jobDescription = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return getBasicJobParamsWithEnv(script, jobName, jobType, '', jenkinsfilePath, jobDescription, defaultJobParamsGetter)
    }

    /**
    * Agrement default job params with some more information given as parameters of the method
    *
    * parameters:
    *   - jobName: Name of the job
    *   - jobType: Job Type for the job to be created from
    *   - envName: Job Type for the job to be created in
    *   - jenkinsfilePath: Path to the jenkinsfile on defined repository
    *   - jobDescription: (optional) Update the job description, if given
    *   - defaultJobParamsGetter: (optional) Closure to get the job default params
    */
    static def getBasicJobParamsWithEnv(def script, String jobName, JobType jobType, String envName = '', String jenkinsfilePath, String jobDescription = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        def jobParams = defaultJobParamsGetter ? defaultJobParamsGetter(script) : getDefaultJobParams(script)
        jobParams.job.name = jobName
        jobParams.job.folder = JenkinsFolderRegistry.getOrRegisterFolder(script, jobType, envName)
        jobParams.jenkinsfile = jenkinsfilePath
        jobParams.job.description = jobDescription ?: jobParams.job.description
        return jobParams
    }

    /**
    * Seed job params are used for `common` jenkinsfiles which are taken from the seed
    **/
    static def getSeedJobParams(def script, String jobName, JobType jobType, String jenkinsfileName, String jobDescription = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        return getSeedJobParamsWithEnv(script, jobName, jobType, '', jenkinsfileName, jobDescription, defaultJobParamsGetter)
    }

    static def getSeedJobParamsWithEnv(def script, String jobName, JobType jobType, String envName, String jenkinsfileName, String jobDescription = '', Closure defaultJobParamsGetter = DEFAULT_PARAMS_GETTER) {
        def jobParams = getBasicJobParamsWithEnv(script, jobName, jobType, envName, Utils.getSeedJenkinsfilePath(script, jenkinsfileName), jobDescription, defaultJobParamsGetter)
        setupJobParamsSeedPipelineConfiguration(script, jobParams, jenkinsfileName)
        return jobParams
    }

    static def addJobParamsEnvIfNotExisting(def script, def jobParams, String key, String value) {
        jobParams.env = jobParams.env ?: [:]
        if (!jobParams.job?.folder?.getDefaultEnvVars().find { it.key == key }
            && !jobParams.env.keySet().find { it == key }) {
            PrintUtils.debug(script, "Adding `${key}` env variable as it is not present yet")
            jobParams.env.put(key, value)
    }
}

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    // Setup job params methods

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
        addJobParamsEnvIfNotExisting(script, jobParams, 'BUILD_JDK_TOOL', Utils.getJenkinsDefaultJDKTools(script))
    }

    static def setupJobParamsDefaultMavenConfiguration(def script, def jobParams) {
        jobParams.env = jobParams.env ?: [:]
        setupJobParamsDefaultJDKConfiguration(script, jobParams)
        addJobParamsEnvIfNotExisting(script, jobParams, 'BUILD_MAVEN_TOOL', Utils.getJenkinsDefaultMavenTools(script))
    }

    static def setupJobParamsBuildChainConfiguration(def script, def jobParams, String repository, String buildchainType, String notificationJobName) {
        setupJobParamsSeedPipelineConfiguration(script, jobParams, KogitoConstants.BUILD_CHAIN_JENKINSFILE)
        jobParams.env = jobParams.env ?: [:]
        jobParams.env.putAll([
            BUILDCHAIN_PROJECT: "kiegroup/${repository}",
            BUILDCHAIN_TYPE: buildchainType,
            BUILDCHAIN_CONFIG_REPO: Utils.getBuildChainConfigRepo(script) ?: Utils.getSeedRepo(script),
            BUILDCHAIN_CONFIG_AUTHOR: Utils.getBuildChainConfigAuthor(script) ?: Utils.getSeedAuthor(script),
            BUILDCHAIN_CONFIG_BRANCH: Utils.getBuildChainConfigBranch(script) ?: Utils.getSeedBranch(script),
            BUILDCHAIN_CONFIG_FILE_PATH: Utils.getBuildChainConfigFilePath(script),
            NOTIFICATION_JOB_NAME: notificationJobName,
            GIT_AUTHOR_TOKEN_CREDENTIALS_ID: Utils.getGitAuthorTokenCredsId(script),
        ])
        addJobParamsEnvIfNotExisting(script, jobParams, 'BUILD_ENVIRONMENT', jobParams.job.folder.getEnvironmentName())
    }

    static def setupJobParamsSeedPipelineConfiguration(def script, def jobParams, String jenkinsfile) {
        jobParams.git.repository = Utils.getSeedRepo(script)
        jobParams.git.author = Utils.getSeedAuthor(script)
        jobParams.git.branch = Utils.getSeedBranch(script)
        jobParams.pr.checkout_branch = Utils.getSeedBranch(script)
        jobParams.jenkinsfile = Utils.getSeedJenkinsfilePath(script, jenkinsfile)
    }

    static def setupJobParamsIntegrationBranchConfiguration(def script, def jobParams, String envName) {
        jobParams.env = jobParams.env ?: [:]
        jobParams.env.putAll([
            COMMIT_MESSAGE: "Update for ${envName} environment",
            GITHUB_USER: 'kie-ci',
        ])
        addJobParamsEnvIfNotExisting(script, jobParams, 'INTEGRATION_BRANCH_CURRENT', "${Utils.getGenerationBranch(script)}-integration-${envName}")
    }

    static def setupJobParamsDeployConfiguration(def script, def jobParams) {
        jobParams.env = jobParams.env ?: [:]
        jobParams.env.put('ENABLE_DEPLOY', 'true')
        addJobParamsEnvIfNotExisting(script, jobParams, 'MAVEN_DEPLOY_REPOSITORY', Utils.getMavenArtifactsUploadRepositoryUrl(script))
        addJobParamsEnvIfNotExisting(script, jobParams, 'MAVEN_DEPLOY_REPOSITORY_CREDS_ID', Utils.getMavenArtifactsUploadRepositoryCredentialsId(script))
    }
}
