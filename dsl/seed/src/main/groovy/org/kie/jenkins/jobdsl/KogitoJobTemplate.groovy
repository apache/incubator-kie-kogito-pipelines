package org.kie.jenkins.jobdsl

import org.kie.jenkins.jobdsl.model.JenkinsFolder
import org.kie.jenkins.jobdsl.model.JenkinsFolderRegistry
import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.utils.EnvUtils
import org.kie.jenkins.jobdsl.utils.JobParamsUtils
import org.kie.jenkins.jobdsl.utils.PrintUtils
import org.kie.jenkins.jobdsl.utils.RegexUtils
import org.kie.jenkins.jobdsl.utils.VersionUtils
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils

/**
* PR job template
**/
class KogitoJobTemplate {

    /**
    * Create a pipeline job
    *
    * jobParams structure:
    *   job:
    *     folder: In which folder should the job be created ? it has to be a org.kie.jenkins.jobdsl.model.Folder struct
    *     name: Name of the job
    *     description: (optional) Description of the job
    *   git:
    *     branch: Branch to checkout
    *     author: Author to checkout
    *     repository: Repository to checkout
    *     credentials: Credentials to use for checkout
    *     repo_url: (optional) Full repository url to checkout. If not defined, will be set from author/repository
    *     project_url: (optional) Which GH project URL ? If not defined, will be set from git author/repository
    *     useRelativeTargetDirectory: (optional) Use a different repository for the pipeline checkout
    *   disable_concurrent: (optional) if set to true, will avoid concurrent build of the job
    *   triggers:
    *     cron: (optional) For recurring jobs
    *   jenkinsfile: (optional) Which Jenkinsfile to get ? Default is `Jenkinsfile`
    *   env: key/value pairs to set as environement variables
    * parametersClosures: Array of closure to add some parameters to the job. In the closure, you can use the different `*Param` methods, like `booleanParam`, `stringParam`, ...
    *
    */
    static def createPipelineJob(def script, Map jobParams = [:]) {
        String jobFolderName = ''
        def jobFolder = jobParams.job.folder
        if (jobFolder) {
            if (![JenkinsFolder].any { it.isAssignableFrom(jobFolder.getClass()) }) {
                throw new RuntimeException('Folder is not of type org.kie.jenkins.jobdsl.model.JenkinsFolder')
            }

            // Expect org.kie.jenkins.jobdsl.model.Folder structure
            jobFolderName = jobFolder.getName()

            if (!EnvUtils.isEnvironmentDefined(script, jobFolder.getEnvironmentName())) {
                String output = "Cannot create job name ${jobParams.job.name} in jobFolder ${jobFolderName} as environment ${jobFolder.getEnvironmentName()} is NOT configured"
                if (Utils.isGenerationFailOnMissingEnvironment(script)) {
                    throw new RuntimeException(output)
                } else if (Utils.isGenerationIgnoreOnMissingEnvironment(script)) {
                    // Do no create the job if the folder is not active
                    PrintUtils.warn(script, output)
                    return
                }
            } else if (!jobFolder.isActive(script)) {
                // Do no create the job if the folder is not active
                PrintUtils.warn(script, "Cannot create job name ${jobParams.job.name} in jobFolder ${jobFolderName} as folder is NOT active")
                return
            }

            script.folder(jobFolderName)
        }

        // Setup job full environment
        Map fullEnv = [:]
        jobParams.env ? fullEnv.putAll(jobParams.env) : null
        // Add folder default env vars only if not existing already ...
        jobFolder?.getDefaultEnvVars().each { key, value ->
            if (!fullEnv.find { "${key}" == "${it.key}" }) { // Use `.find{}` method due to gstring vs string comparison issues...
                fullEnv.put(key, value)
            }
        }

        PrintUtils.info(script, "Create job name ${jobParams.job.name} in jobFolder ${jobFolderName} with folder env ${fullEnv}")

        return script.pipelineJob("${jobFolderName ? "${jobFolderName}/" : ''}${jobParams.job.name}") {
            description("""
                        ${jobParams.job.description ?: jobParams.job.name} on branch ${jobParams.git.branch}\n
                        Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.\n
                        Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.\n
                    """.stripMargin())

            logRotator {
                numToKeep(5)
            }

            if (jobParams.disable_concurrent) {
                throttleConcurrentBuilds {
                    maxTotal(1)
                }
            }

            properties {
                githubProjectUrl(jobParams.git.project_url ?: Utils.createProjectUrl(jobParams.git.author, jobParams.git.repository))

                if (!Utils.areTriggersDisabled(script) && jobParams.triggers) {
                    if (jobParams.triggers.cron) {
                        pipelineTriggers {
                            triggers {
                                cron {
                                    spec(jobParams.triggers.cron)
                                }
                            }
                        }
                    } else if (jobParams.triggers.push) {
                        pipelineTriggers {
                            triggers {
                                githubPush()
                            }
                        }
                    } else {
                        throw new RuntimeException('Unknown `jobParams.triggers`')
                    }
                }
            }

            definition {
                cpsScm {
                    scm {
                        git {
                            remote {
                                url(jobParams.git.repo_url ?: Utils.createRepositoryUrl(jobParams.git.author, jobParams.git.repository))
                                credentials(jobParams.git.credentials)
                            }
                            branch(jobParams.git.branch)
                            extensions {
                                cleanBeforeCheckout()
                                if (jobParams.git.useRelativeTargetDirectory) {
                                    relativeTargetDirectory(repository)
                                }
                            }
                        }
                    }
                    scriptPath("${jobParams.jenkinsfile ?: 'Jenkinsfile'}")
                }
            }

            jobParams.parametersClosures.each { paramsClosure ->
                parameters(paramsClosure)
            }

            environmentVariables {
                fullEnv.each {
                    env(it.key, it.value)
                }
            }
}
}

    /**
    * Create a PR job
    *
    * jobParams structure
    *   # See also createPipelineJob(script, jobParams)
    *   pr:
    *     checkout_branch: (optional) Which specific branch to checkout for the PR pipeline ? will override git branch.
    *     trigger_phrase: (optional) Trigger phrase for the PR job. Default is '.*[j|J]enkins,?.*(retest|test) this.*'
    *     trigger_phrase_only: (optional) Set to true if the PR job should not be launched automatically but with the trigger phrase
    *     ignore_for_labels: (optional) Labels list to ignore
    *     run_only_for_labels: (optional) Labels list to run only when set
    *     run_only_for_branches: (optional) Branches for which the job should be executed
    *     ignore_for_branches: (optional) Branches for which the job should NOT be executed
    *     commitContext: (optional) Name of the commit context to appear in GH. Default is `Linux`.
    **/
    static def createPRJob(def script, Map jobParams = [:]) {
        jobParams.triggers = [:] // Reset triggers
        jobParams.pr = jobParams.pr ?: [:] // Setup default config for pr to avoid NullPointerException

        if (!jobParams.job.folder) {
            jobParams.job.folder = JenkinsFolderRegistry.getOrRegisterFolder(script, JobType.PULL_REQUEST)
        }

        if (Utils.isTestEnvironment(script)) {
            jobParams.pr = jobParams.pr ?: [:]
            jobParams.pr.putAll ([
                run_only_for_labels: (jobParams.pr.run_only_for_labels ?: []) + [KogitoConstants.LABEL_DSL_TEST],
                run_only_for_branches: [ jobParams.git.repository == 'optaplanner-quickstarts' ? 'development' : 'main' ],
                authorized_users: [ 'kiegroup' ],
                authorized_groups: [ 'kiegroup' ],
            ])

            // Enable PR test only if main branch
            if (Utils.isMainBranch(script)) {
                jobParams.git.project_url = "https://github.com/kiegroup/${jobParams.pr.target_repository ?: jobParams.git.repository}/"
            }
        }

        def job = createPipelineJob(script, jobParams)
        job?.with {
            // Redefine to keep days instead of number of builds
            logRotator {
                daysToKeep(10)
            }

            // Redefine author and branch in git
            definition {
                cpsScm {
                    scm {
                        git {
                            remote {
                                url(jobParams.git.repo_url ?: Utils.createRepositoryUrl(jobParams.git.author, jobParams.git.repository))
                                credentials(jobParams.git.credentials)
                                if (!jobParams.pr.checkout_branch) {
                                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                                }
                            }

                            branch(jobParams.pr.checkout_branch ?: '${sha1}')

                            extensions {
                                cleanBeforeCheckout()
                                if (jobParams.git.useRelativeTargetDirectory) {
                                    relativeTargetDirectory(repository)
                                }
                            }
                        }
                    }
                    scriptPath("${jobParams.jenkinsfile ?: '.ci/jenkins/Jenkinsfile'}")
                }
            }

            // Add ghprbTrigger
            properties {
                pipelineTriggers {
                    triggers {
                        ghprbTrigger {
                            // Ordered by appearence in Jenkins UI
                            gitHubAuthId(jobParams.git.credentials)
                            adminlist('')
                            useGitHubHooks(true)
                            triggerPhrase(jobParams.pr.trigger_phrase ?: KogitoConstants.KOGITO_DEFAULT_PR_TRIGGER_PHRASE)
                            onlyTriggerPhrase(jobParams.pr.trigger_phrase_only ?: false)
                            autoCloseFailedPullRequests(false)
                            skipBuildPhrase(".*\\[skip\\W+ci\\].*")
                            displayBuildErrorsOnDownstreamBuilds(false)
                            cron('')
                            whitelist(jobParams.pr.authorized_users ? jobParams.pr.authorized_users.join('\n') : jobParams.git.author)
                            orgslist(jobParams.pr.authorized_groups ? jobParams.pr.authorized_groups.join('\n') : jobParams.git.author)
                            blackListLabels(jobParams.pr.ignore_for_labels ? jobParams.pr.ignore_for_labels.join('\n') : '')
                            whiteListLabels(jobParams.pr.run_only_for_labels ? jobParams.pr.run_only_for_labels.join('\n') : '')
                            allowMembersOfWhitelistedOrgsAsAdmin(true)
                            buildDescTemplate('')
                            blackListCommitAuthor('')
                            whiteListTargetBranches {
                                (jobParams.pr.run_only_for_branches ?: []).each { br ->
                                    ghprbBranch {
                                        branch(br)
                                    }
                                }
                            }
                            blackListTargetBranches {
                                (jobParams.pr.ignore_for_branches ?: []).each { br ->
                                    ghprbBranch {
                                        branch(br)
                                    }
                                }
                            }
                            includedRegions('')
                            excludedRegions(jobParams.pr.excluded_regions ? jobParams.pr.excluded_regions.join('\n') : '')
                            extensions {
                                ghprbSimpleStatus {
                                    commitStatusContext(jobParams.pr.commitContext ?: 'Linux')
                                    addTestResults(true)
                                    showMatrixStatus(false)
                                    statusUrl('${BUILD_URL}display/redirect')
                                    triggeredStatus('Build triggered.')
                                    startedStatus('Build started.')
                                }
                                ghprbBuildStatus {
                                    messages {
                                        if (!jobParams.pr.disable_status_message_error) {
                                            ghprbBuildResultMessage {
                                                result('ERROR')
                                                message("The ${jobParams.pr.commitContext ?: 'Linux'} check has **an error**. Please check [the logs](\${BUILD_URL}display/redirect).")
                                            }
                                        }
                                        if (!jobParams.pr.disable_status_message_failure) {
                                            ghprbBuildResultMessage {
                                                result('FAILURE')
                                                message("The ${jobParams.pr.commitContext ?: 'Linux'} check has **failed**. Please check [the logs](\${BUILD_URL}display/redirect).")
                                            }
                                        }
                                    }
                                }
                                ghprbCancelBuildsOnUpdate {
                                    overrideGlobal(true)
                                }
                            }
                            permitAll(false)
                            commentFilePath('')
                            msgSuccess('Success')
                            msgFailure('Failure')
                            commitStatusContext('')
                        }
                    }
                }
            }
        }
        return job
    }

    /**
    * Should be merged with `createPerRepoPRJobs(def script, def prFolder, Closure jobsRepoConfigGetter, Closure defaultParamsGetter)`
    * once https://issues.redhat.com/browse/PLANNER-2870 is implemented
    */
    static def createPerRepoPRJobs(def script, String envName, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        PrintUtils.debug(script, "createPerRepoPRJobs for env ${envName}")
        JenkinsFolder prFolder = JenkinsFolderRegistry.getOrRegisterFolder(script, JobType.PULL_REQUEST, envName)
        return createPerRepoPRJobsWithFolder(script, prFolder, jobsRepoConfigGetter, defaultJobParamsGetter)
    }

    /**
    * Set per repo PR jobs for a repository.
    *
    * Jobs are defined into the `jobsRepoConfig`. Default job params are retrieved from `defaultJobParamsGetter`.
    * If no `defaultJobParamsGetter` is given, then the default one is used, aka `JobParamsUtils.getDefaultJobParams` method to retrieve those.
    * The method will configure the different PR jobs with the given `jobsRepoConfig` and the default params retrieved.
    *
    * See createPRJob(script, jobParams) for the default job params structure
    *
    * jobsRepoConfig structure:
    *   parallel: (optional) should the different jobs executed in parallel ?
    *   optional: (optional) In case even the PR jobs should not be executed automatically.
    *   jobs: list of jobs
    *     id: Id of the job. Will be used as commit context
    *     repository: (optional) Repository to checkout for the job ?
    *     primary: (optional) should the job be launched first (only considered for non-parallel jobs)
    *     dependsOn: (optional) which job should be executed before that one ? Ignored if `primary` is set (only considered for non-parallel jobs)
    *     jenkinsfile: (optional) where to lookup the jenkinsfile. else it will take the default one
    *   testType: (optional) Name of the tests. Used for the trigger phrase and commitContext. Default is `tests`.
    */
    static def createPerRepoPRJobsWithFolder(def script, def prFolder, Closure jobsRepoConfigGetter, Closure defaultJobParamsGetter = JobParamsUtils.DEFAULT_PARAMS_GETTER) {
        PrintUtils.info(script, "createPerRepoPRJobsWithFolder in folder ${prFolder.getName()}")
        String testTypeId
        String testTypeName
        if (prFolder instanceof JenkinsFolder) {
            String envName = prFolder.getEnvironmentName()
            testTypeId = envName ?: 'tests' // Define harcoded value for no env
            testTypeName = envName ?: 'build' // Define harcoded value for no env
        } else {
            throw new RuntimeException('Folder is not of type org.kie.jenkins.jobdsl.model.JenkinsFolder')
        }
        PrintUtils.debug(script, "createPerRepoPRJobsWithFolder with testTypeId=${testTypeId} and  testTypeName=${testTypeName}")
        String triggerPhraseTestType = RegexUtils.getRegexMultipleCase(testTypeId)

        Map jobsRepoConfig = jobsRepoConfigGetter(prFolder)
        boolean parallel = jobsRepoConfig.parallel
        boolean useBuildChain = jobsRepoConfig.buildchain

        return jobsRepoConfig.jobs.collect { jobCfg ->
            def jobParams = defaultJobParamsGetter(script)
            jobParams.job.folder = prFolder
            jobParams.env = jobParams.env ?: [:]
            jobParams.pr = jobParams.pr ?: [:]
            JobParamsUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)

            // Kept for backward compatibility
            jobParams.job.name += testTypeId ? ".${testTypeId}" : ''

            // Update jenkinsfile path
            if (jobCfg.jenkinsfile) {
                jobParams.jenkinsfile = jobCfg.jenkinsfile
            }

            jobParams.git.project_url = "https://github.com/${jobParams.git.author}/${jobParams.pr.target_repository ?: jobParams.git.repository}/"

            boolean downstream = jobCfg.repository && jobCfg.repository != jobParams.git.repository

            if (downstream) { // Downstream job
                jobParams.env.put('DOWNSTREAM_BUILD', true)
                jobParams.env.put('UPSTREAM_TRIGGER_PROJECT', jobParams.git.repository)
                jobParams.job.description = "Run ${testTypeName} tests of ${jobCfg.repository} due to changes in ${jobParams.git.repository} repository"
                jobParams.job.name += '.downstream'

                // Checkout targeted repo and build it
                // Buildchain will use other settings and we should not interfere here
                if (!useBuildChain) {
                    jobParams.pr.checkout_branch = VersionUtils.getProjectTargetBranch(jobCfg.repository, jobParams.git.branch, jobParams.git.repository)
                    jobParams.git.repository = jobCfg.repository
                }
            } else {
                jobParams.job.description = "Run tests from ${jobParams.git.repository} repository"
            }

            // Update jenkinsfile path
            String defaultJenkinsConfigPath = Utils.getRepositoryJenkinsConfigPath(script, jobParams.git.repository)
            if (jobCfg.jenkinsfile) {
                jobParams.jenkinsfile = jobCfg.jenkinsfile
            } else if (defaultJenkinsConfigPath) {
                jobParams.jenkinsfile = "${defaultJenkinsConfigPath}/Jenkinsfile"
            }

            if (useBuildChain) {
                // Buildchain uses centralized configuration for Jenkinsfile.buildchain to checkout
                // Overrides configuration already done
                JobParamsUtils.setupJobParamsBuildChainConfiguration(script, jobParams, jobCfg.repository ?: jobParams.git.repository, 'cross_pr', "(${testTypeId}) - ${jobCfg.id}")

                // Should target the GIT_BRANCH, if not defined
                if (!jobParams.pr.run_only_for_branches) {
                    jobParams.pr.run_only_for_branches = [ Utils.getGitBranch(script) ]
                }

                // Status messages are sent directly by the pipeline as comments
                jobParams.pr.putAll([
                    disable_status_message_error: true,
                    disable_status_message_failure: true,
                ])
            }
            jobParams.job.name += ".${jobCfg.id.toLowerCase()}"

            jobParams.pr.putAll([
                commitContext: getTypedId(testTypeName, jobCfg.id),
            ])

            if (!jobParams.pr.run_only_for_branches) {
                jobParams.pr.run_only_for_branches = [ jobParams.git.branch ]
            }

            // Setup PR triggers
            jobParams.pr.trigger_phrase = generateMultiJobTriggerPhrasePattern(triggerPhraseTestType, '')
            jobParams.pr.trigger_phrase += '|' + generateMultiJobTriggerPhrasePattern(triggerPhraseTestType, RegexUtils.getRegexMultipleCase(jobCfg.id))
            if (downstream) {
                jobParams.pr.trigger_phrase += '|' + generateMultiJobTriggerPhrasePattern(triggerPhraseTestType, 'downstream')
            }
            if (parallel || jobCfg.primary) {
                jobParams.pr.trigger_phrase_only = jobsRepoConfig.optional

                if (!jobsRepoConfig.optional) {
                    jobParams.pr.trigger_phrase = "(${KogitoConstants.KOGITO_DEFAULT_PR_TRIGGER_PHRASE})|${jobParams.pr.trigger_phrase}"
                }
            } else if (jobCfg.dependsOn) {
                // Sequential and need to wait for another job to complete`
                jobParams.pr.trigger_phrase_only = true
                jobParams.pr.trigger_phrase += "| (.*${getTypedId(testTypeName, jobCfg.dependsOn, true)}.*successful.*)"
            } else {
                error 'You need to define `primary` or `dependsOn`. Else your job will never be launched...'
            }

            // Update env
            // Job env overrides always any value
            jobCfg.env.each { key, value ->
                jobParams.env.put(key, value)
            }

            return createPRJob(script, jobParams)
        }
    }

    static String getTypedId(String prefix, String id, boolean regex = false) {
        if (regex) {
            return "\\(${prefix}\\) ${id}"
        }
        return "(${prefix}) ${id}"
    }

    static String generateMultiJobTriggerPhrasePattern(String testType, String id = '') {
        String idStr = id ? id + ' ' : ''
        return "(.*${RegexUtils.getRegexFirstLetterCase('jenkins')},?.*(rerun|run) ${idStr}${testType}.*)"
    }

}
