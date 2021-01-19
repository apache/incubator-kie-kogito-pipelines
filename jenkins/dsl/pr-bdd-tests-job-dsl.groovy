pipelineJob('KIE/kogito/ghprb-webhooks/kogito-runtimes-bdd') {
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        github('kiegroup/kogito-pipelines')
                    }
                    branch('*/master')
                }
            }
            scriptPath('Jenkinsfile.test-pr')
        }
    }
    // TODO: to be removed when can setup separate Maven repository for each PR
    concurrentBuild(false)
    properties {
        githubProjectUrl('https://github.com/kiegroup/kogito-runtimes/')
        buildDiscarder {
            strategy {
                logRotator {
                    daysToKeepStr('')
                    numToKeepStr('5')
                    artifactDaysToKeepStr('')
                    artifactNumToKeepStr('')
                }
            }
        }
    }
    triggers {
        ghprbTrigger {
            adminlist('triceo rsynek winklerm mcimbale mbiarnes pszubiak radtriste')
            whitelist('')
            orgslist('kiegroup')
            // use cron to check for new PR's/comments since webhook can't send payload to RHBA Jenkins on VPN?
            cron('H/15 * * * *')
            triggerPhrase('.*[j|J]enkins,? run BDD tests.*')
            onlyTriggerPhrase(true)
            /* useGitHubHooks(true) */
            useGitHubHooks(false)
            permitAll(false)
            autoCloseFailedPullRequests(false)
            displayBuildErrorsOnDownstreamBuilds(false)
            commentFilePath("")
            skipBuildPhrase('.*\\[skip\\W+ci\\].*')
            blackListCommitAuthor('')
            allowMembersOfWhitelistedOrgsAsAdmin(true)
            msgSuccess('BDD tests succeeded.')
            msgFailure('BDD tests failed.')
            commitStatusContext('Linux')
            gitHubAuthId('kie-ci1-token')
            buildDescTemplate('')
            blackListLabels('')
            whiteListLabels('')
            extensions {
                // unsure of functionality, copied from KIE/kogito/ghprb-webhooks jobs
                ghprbSimpleStatus {
                    showMatrixStatus(true)
                    commitStatusContext('Linux')
                    statusUrl('${BUILD_URL}display/redirect')
                    triggeredStatus('Build triggered.')
                    startedStatus('Build started.')
                    addTestResults(true)
                }
                ghprbBuildStatus {
                    messages {
                        /* need to manually add the "$" before the bracketed BUILD_URL
                        in the final so that the seed build URL is not used */
                        ghprbBuildResultMessage {
                            result('SUCCESS')
                            message("The build has **passed**. You can find the build [here]({BUILD_URL}).")
                        }
                        ghprbBuildResultMessage {
                            result('ERROR')
                            message("The build has **an error**. Please check [the logs]({BUILD_URL}console).")
                        }
                        ghprbBuildResultMessage {
                            result('FAILURE')
                            message("The build has **failed**. Please check [the logs]({BUILD_URL}console).")
                        }
                    }
                }
            }
            includedRegions('')
            excludedRegions('')
        }
    }
}

