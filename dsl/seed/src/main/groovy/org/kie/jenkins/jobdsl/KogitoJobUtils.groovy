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

    static def getDefaultJobParams(def script, String repoName = '') {
        String repository = repoName ?: Utils.getRepoName(script)
        def jobParams = [
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
                ignore_for_labels: [ 'skip-ci' ],
            ]
        ]
        if (Utils.isProdEnvironment(script)) {
            jobParams.pr.ignore_for_labels.add(KogitoConstants.LABEL_DSL_TEST)
        }
        return jobParams
    }

    static def getBasicJobParams(def script, String jobName, Folder jobFolder, String jenkinsfilePath, String jobDescription = '', Closure defaultJobParamsGetter = null) {
        def jobParams = defaultJobParamsGetter ? defaultJobParamsGetter() : getDefaultJobParams(script)
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

    /**
    * Seed job params are used for `common` jenkinsfiles which are taken from the seed
    **/
    static def getSeedJobParams(def script, String jobName, Folder jobFolder, String jenkinsfileName, String jobDescription = '') {
        def jobParams = getBasicJobParams(script, jobName, jobFolder, Utils.getSeedJenkinsfilePath(script, jenkinsfileName), jobDescription) {
            return getDefaultJobParams(script, Utils.getSeedRepo(script))
        }
        jobParams.git.author = Utils.getSeedAuthor(script)
        jobParams.git.branch = Utils.getSeedBranch(script)
        return jobParams
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
            REPO_NAME: "${repository}",
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

            SEED_REPO: Utils.getSeedRepo(script),
            SEED_AUTHOR_NAME: Utils.getSeedAuthor(script),
            SEED_BRANCH: Utils.getSeedBranch(script),
            SEED_AUTHOR_CREDS_ID: Utils.getSeedAuthorCredsId(script)
        ])
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
        def jobParams = getSeedJobParams(script, 'update-quarkus-platform', Folder.TOOLS, 'Jenkinsfile.update-quarkus-platform', "Update Quarkus platform with new version of ${project}")
        KogitoJobUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)
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

    static def createMandrelLTSPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = null) {
        return createPerEnvPerRepoPRJobs(script, [ Environment.MANDREL_LTS ], jobsRepoConfigGetter, defaultParamsGetter)
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
