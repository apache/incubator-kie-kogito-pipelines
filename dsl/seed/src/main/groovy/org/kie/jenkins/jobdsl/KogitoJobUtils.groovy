/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kie.jenkins.jobdsl

import groovy.json.JsonOutput

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
        def jobParams = JobParamsUtils.getSeedJobParams(script, "update-${dependencyName.toLowerCase()}-${Utils.getRepositoryJobDisplayName(script, repository)}", JobType.TOOLS, 'Jenkinsfile.update-dependency-version', "Update ${dependencyName} version for ${repository}")
        JobParamsUtils.setupJobParamsAgentDockerBuilderImageConfiguration(script, jobParams)
        // Setup correct checkout branch for pipelines
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),

            DEPENDENCY_NAME: "${dependencyName}",
            PROPERTIES_FILENAME: KogitoConstants.PIPELINE_PROPERTIES_FILENAME,

            PR_PREFIX_BRANCH: Utils.getGenerationBranch(script),

            BUILD_BRANCH_NAME: Utils.getGitBranch(script),
            GIT_AUTHOR:  Utils.getGitAuthor(script),
            GIT_AUTHOR_CREDS_ID: Utils.getGitAuthorCredsId(script),
            GIT_AUTHOR_PUSH_CREDS_ID: Utils.getGitAuthorPushCredsId(script),
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
                stringParam('JIRA_NUMBER', '', '(Optional) Is there any issues.redhat.com JIRA associated to that change ? .')
            }
        }
        return job
    }

    /**
    * Create a Quarkus update job which allow to update the quarkus version into current repository, via Maven or Gradle
    */
    static def createQuarkusVersionUpdateToolsJobForCurrentRepo(def script, def mavenUpdate = [:], def gradleUpdate = [:], def filepathReplaceRegex = [:]) {
        return createQuarkusUpdateToolsJob(script, Utils.getRepoName(script), mavenUpdate, gradleUpdate, filepathReplaceRegex)
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
        def jobParams = JobParamsUtils.getSeedJobParams(script, 'update-quarkus-all', JobType.TOOLS, 'Jenkinsfile.update-quarkus-version', 'Update Quarkus version for the whole ecosystem')
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),

            BUILD_BRANCH_NAME: Utils.getGitBranch(script),

            PROJECTS_TO_UPDATE: projectsToUpdate.join(','),
            REVIEWERS: reviewers.join(','),

            PROPERTIES_FILENAME: KogitoConstants.PIPELINE_PROPERTIES_FILENAME,

            GIT_AUTHOR_TOKEN_CREDS_ID: Utils.getGitAuthorTokenCredsId(script),

            SEED_CONFIG_FILE_GIT_REPOSITORY: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_REPOSITORY'),
            SEED_CONFIG_FILE_GIT_AUTHOR_NAME: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_AUTHOR_NAME'),
            SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID'),
            SEED_CONFIG_FILE_GIT_AUTHOR_PUSH_CREDS_ID: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_AUTHOR_PUSH_CREDS_ID'),
            SEED_CONFIG_FILE_GIT_BRANCH: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_GIT_BRANCH'),
            SEED_CONFIG_FILE_PATH: Utils.getBindingValue(script, 'SEED_CONFIG_FILE_PATH'),
        ])
        JobParamsUtils.setupJobParamsSeedRepoEnv(script, jobParams)
        def job = KogitoJobTemplate.createPipelineJob(script, jobParams)
        job?.with {
            parameters {
                stringParam('NEW_VERSION', '', 'Which version to set ?')
                stringParam('PR_BRANCH', '', '(Optional) Which PR branch name to use ? If none given, a name will be generated automatically.')
                stringParam('JIRA_NUMBER', '', '(Optional) Is there any issues.redhat.com JIRA associated to that change ? .')
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
        JobParamsUtils.setupJobParamsAgentDockerBuilderImageConfiguration(script, jobParams)
        jobParams.env.putAll([
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),
            BUILD_BRANCH_NAME: Utils.getGitBranch(script),

            GIT_QUARKUS_AUTHOR: Utils.getGitQuarkusAuthor(script),
            GIT_QUARKUS_AUTHOR_CREDS_ID: Utils.getGitQuarkusAuthorCredsId(script),
            PROJECT_NAME: project,

            GIT_FORK_AUTHOR: Utils.getGitForkAuthorName(script),
            GIT_FORK_AUTHOR_CREDS_ID: Utils.getGitForkAuthorCredsId(script),
            GIT_FORK_AUTHOR_PUSH_CREDS_ID: Utils.getGitForkAuthorPushCredsId(script),
        ])
        def job = KogitoJobTemplate.createPipelineJob(script, jobParams)
        job?.with {
            parameters {
                stringParam('NEW_VERSION', '', 'Which version to set ?')

                stringParam('PR_BRANCH', '', '(Optional) Which PR branch name to use ? If none given, a name will be generated automatically.')

                choiceParam('COMMAND', ['stage', 'finalize'], 'Choose if you want to use staged artifacts or released artifacts.')

                stringParam('QUARKUS_PLATFORM_BRANCH', Utils.getGitQuarkusBranch(script), 'Which branch to target on Quarkus platform ?')
            }
        }
        return job
    }

    /**
    * Create a environment integration branch nightly job
    *
    * parameters:
    *   - envName: Environment name for which the job will be created
    *   - scriptCalls: Any command to call before performing the integration branch update
    *
    */
    static def createEnvironmentIntegrationBranchNightlyJob(def script, String envName, def scriptCalls = [], Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        def jobParams = JobParamsUtils.getSeedJobParamsWithEnv(script, "${Utils.getJobDisplayName(script)}.integration", JobType.NIGHTLY, envName, 'Jenkinsfile.environment.integration-branch', "Generic Integration branch job for specific environment", defaultJobParamsGetter)
        jobParams.triggers = jobParams.triggers ?: [ cron : '@midnight' ] // To remove once environment nightlies are managed by main nightly pipeline
        if (!envName) {
            throw new RuntimeException('Please provide a non-empty environment to generate an integration branch job...')
        }
        JobParamsUtils.setupJobParamsIntegrationBranchConfiguration(script, jobParams, envName)
        jobParams.env.putAll( [
            JENKINS_EMAIL_CREDS_ID: Utils.getJenkinsEmailCredsId(script),

            BUILD_BRANCH_NAME: Utils.getGitBranch(script),
            GIT_AUTHOR:  Utils.getGitAuthor(script),
            GIT_AUTHOR_CREDS_ID: Utils.getGitAuthorCredsId(script),
            GIT_AUTHOR_TOKEN_CREDENTIALS_ID: Utils.getGitAuthorTokenCredsId(script),

            SCRIPTS_CALLS: JsonOutput.toJson(scriptCalls),
        ])

        return KogitoJobTemplate.createPipelineJob(script, jobParams)
    }

    static List createAllEnvironmentsPerRepoPRJobs(def script, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        PrintUtils.debug(script, 'createAllEnvironmentsPerRepoPRJobs')
        return createPerEnvironmentPerRepoPRJobs(script, EnvUtils.getAllAutoGeneratedEnvironments(script), jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    static List createPerEnvironmentPerRepoPRJobs(def script, List<String> environments, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        PrintUtils.debug(script, "createPerEnvironmentPerRepoPRJobs for envs ${environments}")
        List allJobs = []

        // Generate default env
        allJobs.addAll(KogitoJobTemplate.createPerRepoPRJobs(script, '', jobsRepoConfigGetter, defaultJobParamsGetter))

        // Generate environments one
        environments.each { envName ->
            Closure envJobsRepoConfigGetter = jobsRepoConfigGetter
            if (!EnvUtils.isEnvironmentPullRequestDefaultCheck(script, envName)) {
                envJobsRepoConfigGetter = getOptionalJobsRepoConfigClosure(script, envJobsRepoConfigGetter)
            }
            allJobs.addAll(KogitoJobTemplate.createPerRepoPRJobs(script, envName, envJobsRepoConfigGetter, defaultJobParamsGetter))
        }

        return allJobs
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
    * Create a Nightly Build-Chain Build&Test job for the given env.
    *
    * See also `createBranchBuildChainJob` method
    */
    static def createNightlyBuildChainBuildAndTestJob(def script, String envName = '', String repository, Map extraEnv = [:], boolean enableNotification = false, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        def jobParams = JobParamsUtils.getSeedJobParamsWithEnv(script, "${Utils.getRepositoryJobDisplayName(script, repository)}.build-and-test", JobType.NIGHTLY, envName, KogitoConstants.BUILD_CHAIN_JENKINSFILE, "Build & Test for ${repository} using the build-chain", defaultJobParamsGetter)
        jobParams.env.putAll(extraEnv)
        jobParams.triggers = jobParams.triggers ?: [ cron : '@midnight' ] // To remove once environment nightlies are managed by main nightly pipeline
        return createBranchBuildChainJob(script, jobParams, repository, enableNotification, envName)
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
    * Create a Nightly Build-Chain Build&Deploy job for the given env.
    *
    * See also `createBranchBuildChainJob` method
    */
    static def createNightlyBuildChainBuildAndDeployJob(def script, String envName = '', String repository, Map extraEnv = [:], boolean enableNotification = false, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        def jobParams = JobParamsUtils.getSeedJobParamsWithEnv(script, "${Utils.getRepositoryJobDisplayName(script, repository)}.build-and-deploy", JobType.NIGHTLY, envName, KogitoConstants.BUILD_CHAIN_JENKINSFILE, "Build & Deploy for ${repository} using the build-chain", defaultJobParamsGetter)
        JobParamsUtils.setupJobParamsDeployConfiguration(script, jobParams)
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
        return createBuildChainIntegrationJob(script, envName, repository, enableNotification) { jenkinsScript ->
            def jobParams = defaultJobParamsGetter(jenkinsScript)
            jobParams.triggers = jobParams.triggers ?: [ cron : '@midnight' ] // To remove once environment nightlies are managed by main nightly pipeline
            return jobParams
        }
    }

    /**
    * Create a job creating an integration branch when performing the build
    *
    * This job will call the build-chain with extra environment variables to allow for the creation of an integration branch
    *
    */
    static def createBuildChainIntegrationJob(def script, String envName, String repository, boolean enableNotification = false, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        def jobParams = JobParamsUtils.getSeedJobParamsWithEnv(script, "${Utils.getRepositoryJobDisplayName(script, repository)}.integration", JobType.NIGHTLY, envName, KogitoConstants.BUILD_CHAIN_JENKINSFILE, "Integration with Quarkus for ${repository} using the build-chain", defaultJobParamsGetter)
        if (!envName) {
            throw new RuntimeException('Please provide a non-empty environment to generate an integration branch job...')
        }
        JobParamsUtils.setupJobParamsIntegrationBranchConfiguration(script, jobParams, envName)
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
        JobParamsUtils.setupJobParamsBuildChainConfiguration(script, jobParams, repository, 'branch', notificationJobName)

        jobParams.parametersClosures.add({
            stringParam('DISPLAY_NAME', jobParams.parametersValues?.DISPLAY_NAME ?: '', 'Setup a specific build display name')

            stringParam('GIT_BRANCH_NAME', Utils.getGitBranch(script), 'Set the Git branch to test')

            booleanParam('SKIP_TESTS', jobParams.parametersValues?.SKIP_TESTS ?: false, 'Skip tests')
            booleanParam('SKIP_INTEGRATION_TESTS', jobParams.parametersValues?.SKIP_INTEGRATION_TESTS ?: false, 'Skip IT tests')
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
    private static Closure getOptionalJobsRepoConfigClosure(def script, Closure jobsRepoConfigGetter) {
        return { jobFolder ->
            Map jobsRepoConfig = jobsRepoConfigGetter(jobFolder)
            jobsRepoConfig.optional = true
            return jobsRepoConfig
        }
    }

}
