package org.kie.jenkins.jobdsl

import org.kie.jenkins.jobdsl.model.Folder
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.RegexUtils
import org.kie.jenkins.jobdsl.Utils
import org.kie.jenkins.jobdsl.VersionUtils

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
    *
    */
    static def createPipelineJob(def script, Map jobParams = [:]) {
        String jobFolderName = ''
        Map jobFolderEnv = [:]
        if(jobParams.job.folder) {
            if (!jobParams.job.folder instanceof Folder) {
                throw new RuntimeException('Folder is not of type org.kie.jenkins.jobdsl.model.Folder')
            }
            // Expect org.kie.jenkins.jobdsl.model.Folder structure
            jobFolderName = jobParams.job.folder.getFolderName()
            jobFolderEnv = jobParams.job.folder.getDefaultEnvVars(script)
            if (!jobParams.job.folder.isActive(script)) {
                // Do no create the job if the folder is not active
                println "Cannot create job name ${jobParams.job.name} in jobFolder ${jobFolderName} as folder is NOT active"
                return
            }

            script.folder(jobFolderName)
        }

        println "Create job name ${jobParams.job.name} in jobFolder ${jobFolderName} with folder env${jobFolderEnv}"

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

                if (!Utils.areTriggersDisabled(script) && jobParams.triggers && jobParams.triggers.cron) {
                    pipelineTriggers {
                        triggers {
                            cron {
                                spec(jobParams.triggers.cron)
                            }
                        }
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

            if (jobParams.env || jobFolderEnv) {
                environmentVariables {
                    jobFolderEnv.each {
                        env(it.key, it.value)
                    }
                    jobParams.env.each {
                        env(it.key, it.value)
                    }
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
            jobParams.job.folder = Folder.PULLREQUEST
        }

        def job = createPipelineJob(script, jobParams)
        job.with {
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
                            def authorized_labels = jobParams.pr.run_only_for_labels ? jobParams.pr.run_only_for_labels : []
                            if (jobParams.pr.use_gha_label_triggers) {
                                // No whitelist here, we rely on a GHA/manual which will set the jenkins trigger label
                                // if the user is authorized
                                authorized_labels += KogitoConstants.GH_JENKINS_TRIGGER_LABEL
                                whitelist('')
                                orgslist('')
                                allowMembersOfWhitelistedOrgsAsAdmin(false)
                            } else {
                                whitelist(jobParams.pr.authorized_users ? jobParams.pr.authorized_users.join('\n') : jobParams.git.author)
                                orgslist(jobParams.pr.authorized_groups ? jobParams.pr.authorized_groups.join('\n') : jobParams.git.author)
                                allowMembersOfWhitelistedOrgsAsAdmin(true)
                            }
                            whiteListLabels(authorized_labels.join('\n'))
                            blackListLabels(jobParams.pr.ignore_for_labels ? jobParams.pr.ignore_for_labels.join('\n') : '')                            
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
    * Set per repo PR jobs for a repository.
    *
    * Jobs are defined into the `jobsRepoConfig`. Default job params are retrieved from `defaultParamsGetter`.
    * If no `defaultParamsGetter` is given (aka null), then the method will call the `KogitoJobUtils.getDefaultJobParams` method to retrieve those.
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
    static def createPerRepoPRJobs(def script, Folder prFolder, Closure jobsRepoConfigGetter, Closure defaultParamsGetter = null) {
        String testTypeId = prFolder.getConfigValues()?.typeId ?: prFolder.environment.toId()
        String testTypeName = prFolder.getConfigValues()?.typeName ?: prFolder.environment.toId()
        String triggerPhraseTestType = RegexUtils.getRegexMultipleCase(testTypeId)

        Map jobsRepoConfig = jobsRepoConfigGetter(prFolder)
        boolean parallel = jobsRepoConfig.parallel
        boolean useBuildChain = jobsRepoConfig.buildchain

        return jobsRepoConfig.jobs.collect { jobCfg ->
            def jobParams = defaultParamsGetter ? defaultParamsGetter() : KogitoJobUtils.getDefaultJobParams(script)
            jobParams.job.folder = prFolder
            jobParams.env = jobParams.env ?: [:]
            jobParams.pr = jobParams.pr ?: [:]
            KogitoJobUtils.setupJobParamsDefaultMavenConfiguration(script, jobParams)

            // Kept for backward compatibility
            jobParams.job.name += testTypeId ? ".${testTypeId}" : ''

            // Update jenkinsfile path
            if (jobCfg.jenkinsfile) {
                jobParams.jenkinsfile = jobCfg.jenkinsfile
            }

            jobParams.git.project_url = "https://github.com/${jobParams.git.author}/${jobParams.git.repository}/"
            if (Utils.isTestEnvironment(script)) {
                jobParams.pr.putAll([
                    run_only_for_labels: [KogitoConstants.LABEL_DSL_TEST],
                    run_only_for_branches: [ jobParams.git.repository == 'optaplanner-quickstarts' ? 'development' : 'main' ],
                    authorized_users: [ 'kiegroup' ],
                    authorized_groups: [ 'kiegroup' ],
                ])
                // Enable PR test only if main branch
                if (Utils.isMainBranch(script)) {
                    jobParams.git.project_url = "https://github.com/kiegroup/${jobParams.git.repository}/"
                }
            }

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
            String defaultJenkinsConfigPath = Utils.getJenkinsConfigPath(script, jobParams.git.repository)
            if (jobCfg.jenkinsfile) {
                jobParams.jenkinsfile = jobCfg.jenkinsfile
            } else if (defaultJenkinsConfigPath) {
                jobParams.jenkinsfile = "${defaultJenkinsConfigPath}/Jenkinsfile"
            }

            if (useBuildChain) {
                // Buildchain uses centralized configuration for Jenkinsfile.buildchain to checkout
                // Overrides configuration already done
                String buildChainCheckoutBranch = Utils.getSeedBranch(script)
                // TODO Test -> to change back
                jobParams.pr.checkout_branch = buildChainCheckoutBranch
                jobParams.git.author = Utils.getSeedAuthor(script)
                jobParams.env.put('BUILDCHAIN_PROJECT', "kiegroup/${jobCfg.repository ?: jobParams.git.repository}")
                jobParams.env.put('BUILDCHAIN_PR_TYPE', 'pr')
                jobParams.env.put('BUILDCHAIN_CONFIG_REPO', Utils.getSeedRepo(script))
                jobParams.env.put('BUILDCHAIN_CONFIG_AUTHOR', Utils.getSeedAuthor(script))
                jobParams.env.put('BUILDCHAIN_CONFIG_BRANCH', buildChainCheckoutBranch)
                jobParams.env.put('NOTIFICATION_JOB_NAME', "(${testTypeId}) - ${jobCfg.repository ?: jobParams.git.repository}")
                jobParams.git.repository = Utils.getSeedRepo(script)
                jobParams.jenkinsfile = Utils.getSeedJenkinsfilePath(script, 'Jenkinsfile.buildchain')

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
