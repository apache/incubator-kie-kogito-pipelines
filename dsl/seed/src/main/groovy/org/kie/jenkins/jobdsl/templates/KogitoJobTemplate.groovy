package org.kie.jenkins.jobdsl.templates

import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.RegexUtils
import org.kie.jenkins.jobdsl.FolderUtils
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
    *     folder: In which folder should the job be created ?
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
        return script.pipelineJob("${jobParams.job.folder}/${jobParams.job.name}") {
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

            if (jobParams.env) {
                environmentVariables {
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
            jobParams.job.folder = FolderUtils.getPullRequestFolder(script)
        }

        return createPipelineJob(script, jobParams).with {
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
    }

    /**
    * Create a Quarkus LTS PR job
    *
    * See also createPRJob(script, jobParams)
    *
    * jobParams which are overriden by this method:
    *   description => Setup specific one
    *   name => append `.quarkus-lts`
    *   pr:
    *     trigger_phrase => '.*[j|J]enkins,? run LTS[ tests]?.*'
    *     trigger_phrase_only => true
    *     commitContext => 'LTS' commit context
    *   env:
    *     QUARKUS_BRANCH => LTS quarkus branch
    *     LTS => true
    **/
    static def createQuarkusLTSPRJob(def script, Map jobParams = [:]) {
        def quarkusLtsVersion = Utils.getQuarkusLTSVersion(script)

        jobParams.job.description = "Run on demand tests from ${jobParams.job.name} repository against quarkus LTS"
        jobParams.job.name += '.quarkus-lts'

        jobParams.pr = jobParams.pr ?: [:]
        jobParams.pr.putAll([
            trigger_phrase : KogitoConstants.KOGITO_LTS_PR_TRIGGER_PHRASE,
            trigger_phrase_only: true,
            commitContext: "LTS (${quarkusLtsVersion})"
        ])

        jobParams.env = jobParams.env ?: [:]
        jobParams.env.put('QUARKUS_BRANCH', quarkusLtsVersion)
        jobParams.env.put('LTS', true)

        return createPRJob(script, jobParams)
    }

    /**
    * Create a Native PR job
    *
    * See also createPRJob(script, jobParams)
    *
    * jobParams which are overriden by this method:
    *   description => Setup specific one
    *   name => append `.native`
    *   pr:
    *     trigger_phrase => '.*[j|J]enkins,? run native[ tests]?.*'
    *     trigger_phrase_only => true
    *     commitContext => 'Native'
    *   env:
    *     NATIVE => true
    **/
    static def createNativePRJob(def script, Map jobParams = [:]) {
        jobParams.job.description = "Run on demand native tests from ${jobParams.job.name} repository"
        jobParams.job.name += '.native'

        jobParams.pr = jobParams.pr ?: [:]
        jobParams.pr.putAll([
            trigger_phrase : KogitoConstants.KOGITO_NATIVE_PR_TRIGGER_PHRASE,
            trigger_phrase_only: true,
            commitContext: 'Native'
        ])

        jobParams.env = jobParams.env ?: [:]
        jobParams.env.put('NATIVE', true)

        return createPRJob(script, jobParams)
    }

    /**
    * Set multijob PR jobs for a repository.
    *
    * Jobs are defined into the `multijobConfig`. Default job params are retrieved from `defaultParamsGetter`.
    * The method will configure the different PR jobs with the given `multijobConfig` and default params from `defaultParamsGetter`
    *
    * See createPRJob(script, jobParams) for the default job params structure
    *
    * multijobConfig structure:
    *   parallel: (optional) should the different jobs executed in parallel ?
    *   optional: (optional) In case even the PR jobs should not be executed automatically.
    *   jobs: list of jobs
    *     id: Id of the job. Will be used as commit context
    *     repository: (optional) Repository to checkout for the job ?
    *     primary: (optional) should the job be launched first (only considered for non-parallel jobs)
    *     dependsOn: (optional) which job should be executed before that one ? Ignored if `primary` is set (only considered for non-parallel jobs)
    *     jenkinsfile: (optional) where to lookup the jenkinsfile. else it will take the default one
    *   testType: (optional) Name of the tests. Used for the trigger phrase and commitContext. Default is `tests`.
    *   primaryTriggerPhrase: Redefined default primary trigger phrase
    */
    static def createMultijobPRJobs(def script, Map multijobConfig, Closure defaultParamsGetter) {
        String testTypeId = multijobConfig.testType ? multijobConfig.testType.toLowerCase() : 'tests'
        String testTypeName = multijobConfig.testType ?: 'build'
        String triggerPhraseTestType = RegexUtils.getRegexMultipleCase(testTypeId)

        boolean parallel = multijobConfig.parallel
        boolean useBuildChain = multijobConfig.buildchain

        multijobConfig.jobs.each { jobCfg ->
            def jobParams = defaultParamsGetter()
            jobParams.env = jobParams.env ?: [:]
            jobParams.env.put('BUILD_MVN_OPTS', '-Dproductized')
            jobParams.pr = jobParams.pr ?: [:]

            jobParams.job.name += testTypeId ? ".${testTypeId}" : ''

            // Update jenkinsfile path
            if (jobCfg.jenkinsfile) {
                jobParams.jenkinsfile = jobCfg.jenkinsfile
            }

            jobParams.git.project_url = "https://github.com/${jobParams.git.author}/${jobParams.git.repository}/"

            if (jobCfg.repository && jobCfg.repository != jobParams.git.repository ) { // Downstream job
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
                String buildChainCheckoutBranch = VersionUtils.getProjectTargetBranch(KogitoConstants.BUILDCHAIN_REPOSITORY, jobParams.git.branch, jobParams.git.repository)
                jobParams.pr.checkout_branch = buildChainCheckoutBranch
                jobParams.env.put('BUILDCHAIN_PROJECT', "${jobParams.git.author}/${jobCfg.repository ?: jobParams.git.repository}")
                jobParams.env.put('BUILDCHAIN_PR_TYPE', 'cross_pr')
                jobParams.env.put('BUILDCHAIN_CONFIG_BRANCH', buildChainCheckoutBranch)
                jobParams.env.put('NOTIFICATION_JOB_NAME', "(${testTypeId}) - ${jobCfg.repository ?: jobParams.git.repository}")
                jobParams.git.repository = KogitoConstants.BUILDCHAIN_REPOSITORY
                jobParams.jenkinsfile = KogitoConstants.BUILDCHAIN_JENKINSFILE_PATH

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
            if (parallel || jobCfg.primary) {
                jobParams.pr.trigger_phrase_only = multijobConfig.optional

                jobParams.pr.trigger_phrase = "(${multijobConfig.primaryTriggerPhrase ?: KogitoConstants.KOGITO_DEFAULT_PR_TRIGGER_PHRASE})"
                jobParams.pr.trigger_phrase += '|' + generateMultiJobTriggerPhrasePattern(triggerPhraseTestType, parallel ? RegexUtils.getRegexMultipleCase(jobCfg.id) : '')
            } else if (jobCfg.dependsOn) {
                // Sequential and need to wait for another job to complete`
                jobParams.pr.trigger_phrase_only = true
                jobParams.pr.trigger_phrase = "(.*${getTypedId(testTypeName, jobCfg.dependsOn, true)}.*successful.*)"
                jobParams.pr.trigger_phrase += '|' + generateMultiJobTriggerPhrasePattern(triggerPhraseTestType, RegexUtils.getRegexMultipleCase(jobCfg.id))
            } else {
                error 'You need to define `primary` or `dependsOn`. Else your job will never be launched...'
            }
            // Add for all cases the `Jenkins run downstream` trigger comment if job dependsOn
            if (jobCfg.dependsOn) {
                jobParams.pr.trigger_phrase += '|' + generateMultiJobTriggerPhrasePattern(triggerPhraseTestType, 'downstream')
            }

            // Update env
            // Job env overrides always any value
            jobCfg.env.each { key, value ->
                jobParams.env.put(key, value)
            }

            createPRJob(script, jobParams)
        }
    }

    /**
    * Set multijob LTS PR jobs for a repository.
    *
    * See also createMultijobPRJobs(script, multijobConfig, defaultParamsGetter)
    *
    * Overriden config:
    *   testType => 'LTS'
    *   jobs.env => added `QUARKUS_BRANCH`, `LTS` and `DISABLE_SONARCLOUD`
    *   optional => true
    *   primaryTriggerPhrase => '.*[j|J]enkins,? run LTS[ tests]?.*'
    */
    static def createMultijobLTSPRJobs(def script, Map multijobConfig, Closure defaultParamsGetter) {
        multijobConfig.testType = 'LTS'
        multijobConfig.jobs.each { job ->
            job.env = job.env ?: [:]
            job.env.QUARKUS_BRANCH = Utils.getQuarkusLTSVersion(script)
            job.env.LTS = true
            job.env.DISABLE_SONARCLOUD = true
        }
        multijobConfig.optional = true
        multijobConfig.primaryTriggerPhrase = KogitoConstants.KOGITO_LTS_PR_TRIGGER_PHRASE
        createMultijobPRJobs(script, multijobConfig, defaultParamsGetter)
    }

    /**
    * Set multijob Native PR jobs for a repository.
    *
    * See also createMultijobPRJobs(script, multijobConfig, defaultParamsGetter)
    *
    * Overriden config:
    *   testType => 'native'
    *   jobs.env => added `DISABLE_SONARCLOUD` and then `BUILD_MVN_OPTS_CURRENT` and `NATIVE_PROFILE` if not set already
    *   optional => true
    *   primaryTriggerPhrase => '.*[j|J]enkins,? run native[ tests]?.*'
    */
    static def createMultijobNativePRJobs(def script, Map multijobConfig, Closure defaultParamsGetter) {
        multijobConfig.testType = 'native'
        multijobConfig.jobs.each { job ->
            job.env = job.env ?: [:]
            job.env.DISABLE_SONARCLOUD = true
            job.env.BUILD_MVN_OPTS_CURRENT = job.env.BUILD_MVN_OPTS_CURRENT ?: "-Pnative ${KogitoConstants.DEFAULT_NATIVE_CONTAINER_PARAMS}"
            job.env.ADDITIONAL_TIMEOUT = job.env.ADDITIONAL_TIMEOUT ?: '720'
        }
        multijobConfig.optional = true
        multijobConfig.primaryTriggerPhrase = KogitoConstants.KOGITO_NATIVE_PR_TRIGGER_PHRASE
        createMultijobPRJobs(script, multijobConfig, defaultParamsGetter)
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

    static def getDefaultJobParams(def script, String repository) {
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
                ],
                ignore_for_labels: [ 'skip-ci', 'dsl-test' ],
            ]
        ]
    }

    static def getCompletedJobParams(def script, String repository, String jobName, String jobFolder, String jenkinsfileName, String jobDescription = '') {
        def jobParams = getDefaultJobParams(script, repository)
        jobParams.job.name = jobName
        jobParams.job.folder = jobFolder
        jobParams.jenkinsfile = jenkinsfileName
        jobParams.job.description = jobDescription ?: jobParams.job.description
        return jobParams
    }

}
