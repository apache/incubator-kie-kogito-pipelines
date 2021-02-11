package org.kie.jenkins.jobdsl.templates

import org.kie.jenkins.jobdsl.Utils

/**
* PR job template
**/
class KogitoJobTemplate {

    static def createPipelineJob(def steps, Map jobParams = [:]) {
        return steps.pipelineJob("${jobParams.job.folder}/${jobParams.job.name}") {
            description("""
                        ${jobParams.job.description ?: jobParams.job.name} on branch ${jobParams.git.branch}\n
                        Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.\n
                        Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.\n
                    """.stripMargin())

            logRotator {
                numToKeep(10)
            }

            if (jobParams.triggers && jobParams.triggers.cron) {
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

    static def createPRJob(def steps, Map jobParams = [:]) {
        jobParams.triggers = [:] // Reset triggers
        jobParams.pr = jobParams.pr ?: [:] // Setup default config for pr to avoid NullPointerException

        return createPipelineJob(steps, jobParams).with {
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
                                url(jobParams.git.repo_url ?: Utils.createRepositoryUrl('${ghprbPullAuthorLogin}', jobParams.git.repository))
                                credentials(jobParams.git.credentials)
                            }
                            branch(jobParams.pr.checkout_branch ?: '${ghprbSourceBranch}')
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

            // Add ghprbTrigger
            triggers {
                ghprbTrigger {
                    // Ordered by appearence in Jenkins UI
                    gitHubAuthId(jobParams.git.credentials)
                    adminlist('')
                    useGitHubHooks(true)
                    triggerPhrase(jobParams.pr.trigger_phrase ?: '.*[j|J]enkins,?.*(retest|test) this.*')
                    onlyTriggerPhrase(false)
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
                            commitStatusContext(jobParams.pr.commitContext ?: 'Build&Test')
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
                                    message("The ${jobParams.pr.commitContext ?: 'Build&Test'} check has **an error**. Please check [the logs](" + '${BUILD_URL}' + 'console).')
                                }
                                ghprbBuildResultMessage {
                                    result('FAILURE')
                                    message("The ${jobParams.pr.commitContext ?: 'Build&Test'} check has **failed**. Please check [the logs](" + '${BUILD_URL}' + 'console).')
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

}
