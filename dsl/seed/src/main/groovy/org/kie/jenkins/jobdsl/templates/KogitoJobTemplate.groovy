package org.kie.jenkins.jobdsl.templates

import org.kie.jenkins.jobdsl.Utils

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

            if (!Utils.areTriggersDisabled(script) && jobParams.triggers && jobParams.triggers.cron) {
                triggers {
                    cron (jobParams.triggers.cron)
                }
            }

            if (jobParams.disable_concurrent) {
                throttleConcurrentBuilds {
                    maxTotal(1)
                }
            }

            properties {
                githubProjectUrl(jobParams.git.project_url ?: Utils.createProjectUrl(jobParams.git.author, jobParams.git.repository))
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
                                url(jobParams.git.repo_url ?: Utils.createRepositoryUrl('kiegroup', jobParams.git.repository))
                                credentials(jobParams.git.credentials)
                                refspec('+refs/pull/*:refs/remotes/origin/pr/*')
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
            triggers {
                ghprbTrigger {
                    // Ordered by appearence in Jenkins UI
                    gitHubAuthId(jobParams.git.credentials)
                    adminlist('')
                    useGitHubHooks(true)
                    triggerPhrase(jobParams.pr.trigger_phrase ?: '.*[j|J]enkins,?.*(retest|test) this.*')
                    onlyTriggerPhrase(jobParams.pr.trigger_phrase_only ?: false)
                    autoCloseFailedPullRequests(false)
                    skipBuildPhrase(".*\\[skip\\W+ci\\].*")
                    displayBuildErrorsOnDownstreamBuilds(false)
                    cron('')
                    whitelist(jobParams.git.author)
                    orgslist(jobParams.git.author)
                    blackListLabels('')
                    whiteListLabels('')
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
                                    message("The ${jobParams.pr.commitContext ?: 'Linux'} check has **an error**. Please check [the logs](" + '${BUILD_URL}' + 'console).')
                                }
                                ghprbBuildResultMessage {
                                    result('FAILURE')
                                    message("The ${jobParams.pr.commitContext ?: 'Linux'} check has **failed**. Please check [the logs](" + '${BUILD_URL}' + 'console).')
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
            }
        }
    }

    static def createQuarkusLTSPRJob(def script, Map jobParams = [:]) {
        def quarkusLtsVersion = Utils.getQuarkusLTSVersion(script)

        jobParams.job.description = "Run on demand tests from ${jobParams.job.name} repository against quarkus LTS"
        jobParams.job.name += '.quarkus-lts'
        jobParams.pr = [
            trigger_phrase : '.*[j|J]enkins,? run LTS[ tests]?.*',
            trigger_phrase_only: true,
            commitContext: "LTS (${quarkusLtsVersion})"
        ]
        jobParams.env = jobParams.env ?: [:]
        jobParams.env.put('QUARKUS_BRANCH', quarkusLtsVersion)

        return createPRJob(script, jobParams)
    }

    static def createNativePRJob(def script, Map jobParams = [:]) {
        jobParams.job.description = "Run on demand native tests from ${jobParams.job.name} repository"
        jobParams.job.name += '.native'
        jobParams.pr = [
            trigger_phrase : '.*[j|J]enkins,? run native[ tests]?.*',
            trigger_phrase_only: true,
            commitContext: 'Native'
        ]
        jobParams.env = jobParams.env ?: [:]
        jobParams.env.put('NATIVE', true)

        return createPRJob(script, jobParams)
    }

}
