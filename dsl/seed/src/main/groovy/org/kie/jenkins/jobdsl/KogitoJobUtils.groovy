package org.kie.jenkins.jobdsl

import groovy.json.JsonOutput

import org.kie.jenkins.jobdsl.model.Folder
import org.kie.jenkins.jobdsl.model.JobId
import org.kie.jenkins.jobdsl.model.Environment
import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils
import org.kie.jenkins.jobdsl.VersionUtils

/**
* Job utils
**/
class KogitoJobUtils {

    static def getDefaultJobParams(def script, String repoName = '') {
        String repository = repoName ?: Utils.getRepoName(script)
        return [
            job: [
                name: repository
            ],
            git: [
                author: Utils.getGitAuthor(script),
                branch: Utils.getGitBranch(script),
                repository: repository,
                credentials: Utils.getGitAuthorCredsId(script),
                token_credentials: Utils.getGitAuthorTokenCredsId(script)
            ],
            env: [:],
            parametersClosures: [],
            pr: [
                excluded_regions: [
                    'LICENSE',
                    '\\.gitignore',
                    '.*\\.md',
                    '.*\\.adoc',
                    '.*\\.txt',
                    // '\\.github/.*',
                    // '\\.ci/jenkins/.*',
                    'docsimg/.*',
                ],
                ignore_for_labels: [ 'skip-ci' ],
            ]
        ]
    }

    static def getBasicJobParams(def script, String jobName, Folder jobFolder, String jenkinsfileName, String jobDescription = '', Closure defaultJobParamsGetter = null) {
        def jobParams = defaultJobParamsGetter ? defaultJobParamsGetter() : getDefaultJobParams(script)
        jobParams.job.name = jobName
        jobParams.job.folder = jobFolder
        jobParams.jenkinsfile = jenkinsfileName
        jobParams.job.description = jobDescription ?: jobParams.job.description
        return jobParams
    }

    static def createVersionUpdateToolsJob(def script, String repository, String dependencyName, def mavenUpdate = [:], def gradleUpdate = [:]) {
        def jobParams = getBasicJobParams(script, "update-${dependencyName.toLowerCase()}-${repository}", Folder.TOOLS,
                                                Utils.getPipelinesJenkinsfilePath(script, 'Jenkinsfile.tools.update-dependency-version'), "Update ${dependencyName} version for ${repository}") {
            return getDefaultJobParams(script, KogitoConstants.KOGITO_PIPELINES_REPOSITORY)
                                                }
        // Setup correct checkout branch for pipelines
        jobParams.git.branch = VersionUtils.getProjectTargetBranch(KogitoConstants.KOGITO_PIPELINES_REPOSITORY, jobParams.git.branch, repository)
        jobParams.env.putAll([
            REPO_NAME: "${repository}",
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),

            DEPENDENCY_NAME: "${dependencyName}",
            NOTIFICATION_JOB_NAME: Utils.getRepoNameCamelCase(repository),

            PR_PREFIX_BRANCH: Utils.getGenerationBranch(script),

            GIT_BRANCH_NAME: Utils.getGitBranch(script),
            GIT_AUTHOR:  Utils.getGitAuthor(script),
            GIT_AUTHOR_CREDS_ID: Utils.getGitAuthorCredsId(script),

            PIPELINES_COMMON_SCRIPT_PATH: Utils.getPipelinesJenkinsHelperScript(script),
        ])
        if (mavenUpdate) {
            mavenUpdate.modules ? jobParams.env.put('MAVEN_MODULES',  JsonOutput.toJson(mavenUpdate.modules)) : null
            mavenUpdate.compare_deps_remote_poms ? jobParams.env.put('MAVEN_COMPARE_DEPS_REMOTE_POMS', JsonOutput.toJson(mavenUpdate.compare_deps_remote_poms)) : null
            mavenUpdate.properties ? jobParams.env.put('MAVEN_PROPERTIES', JsonOutput.toJson(mavenUpdate.properties)) : null
        }
        if (gradleUpdate) {
            gradleUpdate.regex ? jobParams.env.put('GRADLE_REGEX', JsonOutput.toJson(gradleUpdate.regex)) : null
        }
        def job = KogitoJobTemplate.createPipelineJob(script, jobParams)
        return job.with {
            parameters {
                stringParam('NEW_VERSION', '', 'Which version to set ?')
            }
        }
        return job
    }

    static def createQuarkusUpdateToolsJob(def script, String repository, def mavenUpdate = [:], def gradleUpdate = [:]) {
        return createVersionUpdateToolsJob(script, repository, 'Quarkus', mavenUpdate, gradleUpdate)
    }

    static List createAllEnvsPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, Environment.getActiveEnvironments(script), jobsRepoConfigGetter, defaultParamsGetter)
    }

    static List createPerEnvPerRepoPRJobs(def script, List<Environment> environments, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = null) {
        List allJobs = []

        applyInAllPullRequestFolders(script, environments) { folder ->
            if (folder.environment.isOptional()) {
                allJobs.addAll(KogitoJobTemplate.createPerRepoPRJobs(script, folder, getOptionalJobsRepoConfigClosure(jobsRepoConfigGetter), defaultJobParamsGetter))
            } else {
                allJobs.addAll(KogitoJobTemplate.createPerRepoPRJobs(script, folder, jobsRepoConfigGetter, defaultJobParamsGetter))
            }
        }

        return allJobs
    }

    static List createDefaultPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.DEFAULT ], jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    static def createNativePerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.NATIVE ], jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    static def createMandrelPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.MANDREL ], jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    static def createQuarkusMainPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.QUARKUS_MAIN ], jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    static def createQuarkusBranchPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.QUARKUS_BRANCH ], jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    /**
    *   Create Build Chain Build&Test jobs for all needed environments (nightlies & release)
    */
    static List createAllEnvsBuildChainBuildAndTestJobs(def script) {
        return createPerEnvBuildChainBuildAndTestJobs(script, Environment.getActiveEnvironments(script))
    }

    /**
    *   Create Build Chain Build&Test jobs for given environments (nightlies & release)
    */
    static List createPerEnvBuildChainBuildAndTestJobs(def script, List<Environment> environments) {
        List allJobs = []

        // Nightlies
        applyInAllNightlyFolders(script, environments) { folder ->
            allJobs.add(createBuildChainBuildAndTestJob(script, folder))
        }

        // TODO release
        // if (!Utils.isMainBranch(this)) {

        return allJobs
        }

    static def createBuildChainBuildAndTestJob(def script, Folder jobFolder) {
        String repository = Utils.getRepoName(script)

        def jobParams = getBasicJobParams(script, "${repository}.${JobId.BUILD_AND_TEST.toId()}", jobFolder,
                                                Utils.getPipelinesJenkinsfilePath(script, KogitoConstants.BUILDCHAIN_JENKINSFILE),
                                                "Build & Test for ${repository} using the build-chain") {
            return getDefaultJobParams(script, KogitoConstants.KOGITO_PIPELINES_REPOSITORY)
                                                }
        // Setup correct checkout branch for pipelines
        jobParams.git.branch = VersionUtils.getProjectTargetBranch(KogitoConstants.KOGITO_PIPELINES_REPOSITORY, jobParams.git.branch, jobParams.git.repository)

        jobParams.parametersClosures.add({
            stringParam('DISPLAY_NAME', '', 'Setup a specific build display name')

            stringParam('GIT_BRANCH_NAME', Utils.getGitBranch(script), 'Set the Git branch to test')

            booleanParam('SKIP_TESTS', false, 'Skip tests')
            booleanParam('SKIP_IT_TESTS', false, 'Skip IT tests') // TODO Optaweb skip integration testing ?
        })
        jobParams.env.putAll([
            REPO_NAME:  "${repository}",

            GIT_AUTHOR:   Utils.getGitAuthor(script),

            BUILDCHAIN_PROJECT:  "kiegroup/${repository}",
            BUILDCHAIN_TYPE:  'branch',
            BUILDCHAIN_CONFIG_BRANCH:  jobParams.git.branch,

            NOTIFICATION_JOB_NAME:  "${jobFolder.environment.toName()}",

            MAVEN_SETTINGS_CONFIG_FILE_ID:  Utils.getBindingValue(script, 'MAVEN_SETTINGS_FILE_ID'),
        ])
        jobParams.env.put('MAVEN_DEPENDENCIES_REPOSITORY',  
                    Utils.getBindingValue(script, jobFolder.isPullRequest() ? 'MAVEN_PR_CHECKS_REPOSITORY_URL' : 'MAVEN_ARTIFACTS_REPOSITORY'))

        return KogitoJobTemplate.createPipelineJob(script, jobParams)
    }

    /**
    *   Create Deploy artifacts jobs for all needed environments (nightlies, update-version & release)
    */
    static List createAllEnvsDeployArtifactsJobs(def script, Closure defaultJobParamsGetter = null) {
        return createPerEnvDeployArtifactsJobs(script, Environment.getActiveEnvironments(script), defaultJobParamsGetter)
    }

    /**
    *   Create Deploy artifacts jobs for given environments (nightlies, update-version & release)
    */
    static List createPerEnvDeployArtifactsJobs(def script, List<Environment> environments, Closure defaultJobParamsGetter = null) {
        List allJobs = []

        // Nightlies
        applyInAllNightlyFolders(script, environments) { folder ->
            allJobs.add(createDeployArtifactsJob(script, folder, defaultJobParamsGetter))
        }

        // Update-version
        allJobs.add(createDeployArtifactsJob(script, Folder.UPDATE_VERSION, defaultJobParamsGetter))

        // TODO release
        // if (!Utils.isMainBranch(this)) {

        return allJobs
        }

    static def createDeployArtifactsJob(def script, Folder jobFolder, Closure defaultJobParamsGetter = null) {
        def jobParams = getCommonRunSimpleJobParams(script, jobFolder, JobId.DEPLOY_ARTIFACTS, defaultJobParamsGetter)

        jobParams.parametersClosures.add({
            booleanParam('SKIP_TESTS', true, 'Skip tests when deploying')
        })

        jobParams.env.putAll([ MAVEN_SETTINGS_CONFIG_FILE_ID: Utils.getBindingValue(script, 'MAVEN_SETTINGS_FILE_ID') ])
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
    *   Create Update version jobs for all needed environments (update-version & release)
    */
    static List createAllEnvsUpdateVersionJobs(def script, List neededProjectVersions, Closure defaultJobParamsGetter = null) {
        return createPerEnvUpdateVersionJobs(script, Environment.getActiveEnvironments(script), neededProjectVersions, defaultJobParamsGetter)
    }

    /**
    *   Create Update version jobs for given environments (update-version & release)
    */
    static List createPerEnvUpdateVersionJobs(def script, List<Environment> environments, List neededProjectVersions, Closure defaultJobParamsGetter = null) {
        List allJobs = []

        // Update-version
        allJobs.add(createUpdateVersionJob(script, Folder.UPDATE_VERSION, neededProjectVersions, defaultJobParamsGetter))

        // TODO release
        // if (!Utils.isMainBranch(this)) {

        return allJobs
        }

    static def createUpdateVersionJob(def script, Folder jobFolder, List neededProjectVersions, Closure defaultJobParamsGetter = null) {
        def jobParams = getCommonRunWithGitJobParams(script, jobFolder, JobId.UPDATE_VERSION, defaultJobParamsGetter)
        jobParams.parametersClosures.add({
            neededProjectVersions.each { vs ->
                stringParam("${vs}_VERSION".toUpperCase(), '', "Set the ${vs} Version")
            }
        })
        return KogitoJobTemplate.createPipelineJob(script, jobParams)
    }

    /**
    *   Create Maven update version jobs for all needed environments (update-version & release)
    */
    static List createAllEnvsMavenUpdateVersionJobs(def script, List neededProjectVersions, Closure defaultJobParamsGetter = null) {
        return createPerEnvMavenUpdateVersionJobs(script, Environment.getActiveEnvironments(script), neededProjectVersions, defaultJobParamsGetter)
    }

    /**
    *   Create Maven update version jobs for given environments (update-version & release)
    */
    static List createPerEnvMavenUpdateVersionJobs(def script, List<Environment> environments, List neededProjectVersions, Closure defaultJobParamsGetter = null) {
        List allJobs = []

        // Update-version
        allJobs.add(createUpdateVersionJob(script, Folder.UPDATE_VERSION, neededProjectVersions, defaultJobParamsGetter))

        // TODO release
        // if (!Utils.isMainBranch(this)) {

        return allJobs
        }

    static def createMavenUpdateVersionJob(def script, Folder jobFolder, List neededProjectVersions, Closure defaultJobParamsGetter = null) {
        def job = createUpdateVersionJob(script, jobFolder, neededProjectVersions) {
            def jobParams = defaultJobParamsGetter()
            jobParams.env.putAll([
                MAVEN_SETTINGS_CONFIG_FILE_ID:  Utils.getBindingValue(script, 'MAVEN_SETTINGS_FILE_ID'),
                MAVEN_DEPENDENCIES_REPOSITORY:  Utils.getBindingValue(script, 'MAVEN_ARTIFACTS_REPOSITORY'),
            ])
            return jobParams
        }
        return job
    }

    static def createAllJobsForArtifactsRepository(def script, List neededProjectVersions, Map jobDisabled = [:], Closure defaultJobParamsGetter = null) {
        if (!jobDisabled.build) {
            createAllEnvsBuildChainBuildAndTestJobs(script)
        }

        if (!jobDisabled.deploy) {
            createAllEnvsDeployArtifactsJobs(script, defaultJobParamsGetter)
        }

        if (!jobDisabled.update_version) {
            createAllEnvsMavenUpdateVersionJobs(script, neededProjectVersions, defaultJobParamsGetter)
        }
    }

    // static def createAllJobsForDefaultRepository(def script, List neededProjectVersions, Map jobDisabled = [:], Closure defaultJobParamsGetter = null) {
    //     if (!jobDisabled.build) {
    //         // TODO createAllDefaultBuildAndTestJobs
    //         createAllEnvsDefaultBuildAndTestJobs(script, defaultJobParamsGetter)
    //     }

    //     if (!jobDisabled.deploy) {
    //         // TODO createAllDefaultDeployJobs
    //         createAllEnvsDefaultDeployJobs(script, defaultJobParamsGetter)
    //     }

    //     if (!jobDisabled.update_version) {
    //         createAllEnvsMavenUpdateVersionJobs(script, neededProjectVersions, defaultJobParamsGetter)
    //     }
    // }

    /*
    * Add optional information to per repo config
    */
    private static Closure getOptionalJobsRepoConfigClosure(Closure jobsRepoConfigGetter) {
        return { jobFolder ->
            Map jobsRepoConfig = jobsRepoConfigGetter(jobFolder)
            jobsRepoConfig.optional = true
            jobsRepoConfig.jobs.each { job ->
                job.env = job.env ?: [:]
                // For sonarcloud to disable for optional
                job.env.ENABLE_SONARCLOUD = false
            }
            return jobsRepoConfig
        }
    }

    /*
    *  Get Job Params for a simple script run (see /.ci/jenkins/Jenkinsfile.repo.run-simple-script)
    */
    static Map getCommonRunSimpleJobParams(def script, Folder jobFolder, JobId jobId, Closure defaultJobParamsGetter = null) {
        String stageName = Utils.allFirstLetterUpperCase(jobId.toId())
        String repository = Utils.getRepoName(script)

        def jobParams = getBasicJobParams(script,
                                                "${repository}.${jobId.toId()}",
                                                jobFolder,
                                                Utils.getPipelinesJenkinsfilePath(script, 'Jenkinsfile.repo.run-simple-script'),
                                                "${stageName} for ${repository}",
                                                defaultJobParamsGetter)
        // Setup correct checkout branch for pipelines
        jobParams.git.branch = VersionUtils.getProjectTargetBranch(KogitoConstants.KOGITO_PIPELINES_REPOSITORY, jobParams.git.branch, repository)

        jobParams.env.putAll([
            REPO_NAME : repository,
            SCRIPT_ID: jobId.toId(),
            MAIN_STAGE_NAME: stageName,
            GIT_AUTHOR: Utils.getGitAuthor(script),
            PIPELINES_COMMON_SCRIPT_PATH: Utils.getPipelinesJenkinsHelperScript(script)
        ])

        jobParams.parametersClosures.add({
            stringParam('DISPLAY_NAME', '', 'Setup a specific build display name')
            stringParam('GIT_BRANCH_NAME', Utils.getGitBranch(script), 'Which branch to update ?')
        })

        return jobParams
    }

    /*
    *  Get Job Params for a run with git script run (see /.ci/jenkins/Jenkinsfile.repo.run-script-with-git)
    */
    static Map getCommonRunWithGitJobParams(def script, Folder jobFolder, JobId jobId, Closure defaultJobParamsGetter = null) {
        def jobParams = getCommonRunSimpleJobParams(script, jobFolder, jobId, defaultJobParamsGetter)

        jobParams.jenkinsfile = Utils.getPipelinesJenkinsfilePath(script, 'Jenkinsfile.repo.run-script-with-git')
        jobParams.env.putAll([
            GIT_AUTHOR_CREDS_ID: Utils.getGitAuthorCredsId(script),
        ])

        jobParams.parametersClosures.add({
            booleanParam('CREATE_PR', false, 'Should we create a PR with the changes ?')
            booleanParam('MERGE_PR_AUTOMATICALLY', false, 'Should the created PR be merged automatically ?')
            stringParam('GIT_TAG', '', 'If you also need to tag the repository. Empty will not do anything.')
        })

        return jobParams
    }

    static void applyInAllNightlyFolders(def script, Closure applyInFolderClosure) {
        applyInAllNightlyFolders(script, Environment.getActiveEnvironments(script), applyInFolderClosure)
    }

    static void applyInAllNightlyFolders(def script, List<Environment> environments, Closure applyInFolderClosure) {
        applyInAllFolders(script, JobType.NIGHTLY, environments, applyInFolderClosure)
    }

    static void applyInAllPullRequestFolders(def script, Closure applyInFolderClosure) {
        applyInAllPullRequestFolders(script, Environment.getActiveEnvironments(script), applyInFolderClosure)
    }

    static void applyInAllPullRequestFolders(def script, List<Environment> environments, Closure applyInFolderClosure) {
        applyInAllFolders(script, JobType.PULLREQUEST, environments, applyInFolderClosure)
    }

    static void applyInAllFolders(def script, JobType jobType, List<Environment> environments, Closure applyInFolderClosure) {
        Folder.getAllFoldersByJobTypeAndEnvironments(script, jobType, environments)
            .findAll { folder -> folder.shouldAutoGenerateJobs() }
            .each { folder -> applyInFolderClosure(folder) }
    }

    }
