package org.kie.jenkins.jobdsl.utils

import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.model.JenkinsFolderRegistry
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils

class JobParamsUtils {
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
        jobParams.git.repository = Utils.getSeedRepo(script)
        jobParams.git.author = Utils.getSeedAuthor(script)
        jobParams.git.branch = Utils.getSeedBranch(script)
        return jobParams
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
}