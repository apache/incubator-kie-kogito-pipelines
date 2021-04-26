package org.kie.jenkins.jobdsl.templates

import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils
import org.kie.jenkins.jobdsl.RegexUtils

/**
* PR job template
**/
class KogitoJobTemplate {

    static def createPipelineJob(def script, Map jobParams = [:]) {
        return script.pipelineJob("${jobParams.job.folder}/${jobParams.job.name}") {
            description("""
                        ${jobParams.job.description ?: jobParams.job.name} on branch ${jobParams.git.branch}\n
                        Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.\n
                        Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.\n
                    """.stripMargin())

            logRotator {
                numToKeep(10)
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
        }
    }

    static def createPRJob(def script, Map jobParams = [:]) {
        jobParams.triggers = [:] // Reset triggers
        jobParams.pr = jobParams.pr ?: [:] // Setup default config for pr to avoid NullPointerException

        if (!jobParams.job.folder) {
            script.folder(KogitoConstants.KOGITO_DSL_PULLREQUEST_FOLDER)
            jobParams.job.folder = KogitoConstants.KOGITO_DSL_PULLREQUEST_FOLDER
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
                            whitelist(jobParams.git.author)
                            orgslist(jobParams.git.author)
                            blackListLabels(jobParams.pr.ignore_for_labels ? jobParams.pr.ignore_for_labels.join('\n') : '')
                            whiteListLabels(jobParams.pr.run_only_for_labels ? jobParams.pr.run_only_for_labels.join('\n') : '')
                            allowMembersOfWhitelistedOrgsAsAdmin(true)
                            buildDescTemplate('')
                            blackListCommitAuthor('')
                            whiteListTargetBranches {
                                (jobParams.pr.whiteListTargetBranches ?: []).each { br ->
                                    ghprbBranch {
                                        branch(br)
                                    }
                                }
                            }
                            blackListTargetBranches {
                                (jobParams.pr.blackListTargetBranches ?: []).each { br ->
                                    ghprbBranch {
                                        branch(br)
                                    }
                                }
                            }
                            includedRegions('')
                            excludedRegions('')
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
                                        ghprbBuildResultMessage {
                                            result('ERROR')
                                            message("The ${jobParams.pr.commitContext ?: 'Linux'} check has **an error**. Please check [the logs](\${BUILD_URL}display/redirect).")
                                        }
                                        ghprbBuildResultMessage {
                                            result('FAILURE')
                                            message("The ${jobParams.pr.commitContext ?: 'Linux'} check has **failed**. Please check [the logs](\${BUILD_URL}display/redirect).")
                                        }
                                        ghprbBuildResultMessage {
                                            result('SUCCESS')
                                            message("The ${jobParams.pr.commitContext ?: 'Linux'} check is **successful**.")
                                        }
                                    }
                                }
                            }
                            permitAll(false)
                            commentFilePath('')
                            msgSuccess('Success')
                            msgFailure('Failure')
                            commitStatusContext('')
                        }
                        ghprbCancelBuildsOnUpdate {
                            overrideGlobal(true)
                        }
                    }
                }
            }
        }
    }

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

        return createPRJob(script, jobParams)
    }

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

    static def createMultijobPRJobs(def script, Map multijobConfig, Closure defaultParamsGetter) {
        String testTypeId = multijobConfig.testType ? multijobConfig.testType.toLowerCase() : 'tests'
        String testTypeName = multijobConfig.testType ?: 'build'
        String triggerPhraseTestType = RegexUtils.getRegexMultipleCase(testTypeId)

        boolean parallel = multijobConfig.parallel

        multijobConfig.jobs.each { jobCfg ->
            def jobParams = defaultParamsGetter()
            jobParams.job.folder = KogitoConstants.KOGITO_DSL_PULLREQUEST_FOLDER
            jobParams.env = jobParams.env ?: [:]
            jobParams.pr = jobParams.pr ?: [:]

            jobParams.job.name += testTypeId ? ".${testTypeId}" : ''
            if (jobCfg.repository) { // Downstream job
                jobParams.env.put('DOWNSTREAM_BUILD', true)
                jobParams.env.put('UPSTREAM_TRIGGER_PROJECT', jobParams.git.repository)
                jobParams.job.description = "Run ${testTypeName} tests of ${jobCfg.repository} due to changes in ${jobParams.git.repository} repository"
                jobParams.job.name += '.downstream'
                jobParams.git.project_url = "https://github.com/${jobParams.git.author}/${jobParams.git.repository}/"
                jobParams.git.repository = jobCfg.repository
                // If downstream repo, checkout target branch for Jenkinsfile
                // For now there is a problem of matching when putting both branch
                // Sometimes it takes the source, sometimes the target ...
                jobParams.pr.checkout_branch = '${ghprbTargetBranch}'
            } else {
                jobParams.job.description = "Run tests from ${jobParams.git.repository} repository"
            }
            jobParams.job.name += ".${jobCfg.id.toLowerCase()}"

            jobParams.pr.putAll([
                commitContext: getTypedId(testTypeName, jobCfg.id),
                run_only_for_labels: [ KogitoConstants.KOGITO_PR_MULTIJOB_LABEL ]
            ])

            // Setup PR triggers
            if (parallel || jobCfg.primary) {
                jobParams.pr.trigger_phrase_only = multijobConfig.optional

                jobParams.pr.trigger_phrase = "(${multijobConfig.primaryTriggerPhrase ?: KogitoConstants.KOGITO_DEFAULT_PR_TRIGGER_PHRASE})"
                jobParams.pr.trigger_phrase += '|' + generateMultiJobTriggerPhrasePattern(triggerPhraseTestType, parallel ? RegexUtils.getRegexMultipleCase(jobCfg.id) : '')
            } else if (jobCfg.dependsOn) {
                // Sequential and need to wait for another job to complete
                jobParams.pr.trigger_phrase_only = true
                jobParams.pr.trigger_phrase = "(.*${getTypedId(testTypeName, jobCfg.dependsOn, true)}.*successful.*)"
                jobParams.pr.trigger_phrase += '|' + generateMultiJobTriggerPhrasePattern(triggerPhraseTestType, RegexUtils.getRegexMultipleCase(jobCfg.id))
            } else {
                error 'You need to define `primary` or `dependsOn`. Else your job will never be launched...'
            }

            // Update env
            if (multijobConfig.extraEnv) {
                jobParams.env.putAll(multijobConfig.extraEnv)
            }

            createPRJob(script, jobParams)
        }
    }

    static def createMultijobLTSPRJobs(def script, Map multijobConfig, Closure defaultParamsGetter) {
        multijobConfig.testType = 'LTS'
        multijobConfig.extraEnv = [ QUARKUS_BRANCH: Utils.getQuarkusLTSVersion(script) ]
        multijobConfig.optional = true
        multijobConfig.primaryTriggerPhrase = KogitoConstants.KOGITO_LTS_PR_TRIGGER_PHRASE
        createMultijobPRJobs(script, multijobConfig, defaultParamsGetter)
    }

    static def createMultijobNativePRJobs(def script, Map multijobConfig, Closure defaultParamsGetter) {
        multijobConfig.testType = 'native'
        multijobConfig.extraEnv = [ NATIVE: true ]
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

}
