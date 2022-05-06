package org.kie.jenkins.jobdsl

import groovy.json.JsonOutput

import org.kie.jenkins.jobdsl.model.Folder
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
        def job = KogitoJobTemplate.createPipelineJob(script, jobParams)
        return job.with {
            parameters {
                stringParam('NEW_VERSION', '', 'Which version to set ?')
            }
        }
    }

    static def createQuarkusUpdateToolsJob(def script, String repository, def mavenUpdate = [:], def gradleUpdate = [:]) {
        return createVersionUpdateToolsJob(script, repository, 'Quarkus', mavenUpdate, gradleUpdate)
    }

    static List createAllEnvsPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, Environment.getActiveEnvironments(script), jobsRepoConfigGetter, defaultParamsGetter)
    }

    static List createPerEnvPerRepoPRJobs(def script, List<Environment> environments, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = null) {
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

    static List createDefaultPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.DEFAULT ], jobsRepoConfigGetter, defaultParamsGetter)
    }

    static def createNativePerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.NATIVE ], jobsRepoConfigGetter, defaultParamsGetter)
    }

    static def createMandrelPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.MANDREL ], jobsRepoConfigGetter, defaultParamsGetter)
    }

    static def createQuarkusMainPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.QUARKUS_MAIN ], jobsRepoConfigGetter, defaultParamsGetter)
    }

    static def createQuarkusBranchPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.QUARKUS_BRANCH ], jobsRepoConfigGetter, defaultParamsGetter)
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
