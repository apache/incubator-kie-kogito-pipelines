package org.kie.jenkins.jobdsl

import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils

/**
* Seed Job utils
* 
* Common methods to create seed jobs
**/
class SeedJobUtils {

    static def createSeedJobTrigger(def jenkinsScript, String jobName, String repository, String gitAuthor, String gitBranch, List pathsToListen, String jobRelativePathToTrigger) {
        if (pathsToListen.isEmpty()) {
            throw new RuntimeException('pathsToListen cannot be empty, else it would end up in an infinite loop ...');
        }
        def job = jenkinsScript.pipelineJob(jobName) {
            description('This job listens to pipelines repo and launch the seed job if needed. DO NOT USE FOR TESTING !!!! See https://github.com/kiegroup/kogito-pipelines/blob/main/docs/jenkins.md#test-specific-jobs')

            logRotator {
                numToKeep(5)
            }

            throttleConcurrentBuilds {
                maxTotal(1)
            }

            parameters {
                stringParam('SEED_AUTHOR', Utils.getSeedAuthor(jenkinsScript), 'If different from the default')
                stringParam('SEED_BRANCH', Utils.getSeedBranch(jenkinsScript), 'If different from the default')

                booleanParam('FORCE_REBUILD', false, 'Default, the job will scan for modified files and do the update in case some files are modified. In case you want to force the DSL generation')
            }

            environmentVariables {
                env('SEED_REPO', KogitoConstants.KOGITO_PIPELINES_REPOSITORY)
                env('SEED_SCRIPTS_FILEPATH', 'dsl/seed/jobs/scripts/seed_repo_generation.groovy')
                env('JOB_TYPE', 'TRIGGER')

                env('JOB_RELATIVE_PATH_TO_TRIGGER', jobRelativePathToTrigger)
                env('LISTEN_TO_MODIFIED_PATHS', new groovy.json.JsonBuilder(pathsToListen).toString())
            }

            definition {
                cpsScm {
                    scm {
                        git {
                            remote {
                                url("https://github.com/kiegroup/${repository}.git")
                                credentials('kie-ci')
                            }
                            branch(gitBranch)
                            extensions {
                                cleanBeforeCheckout()
                            }
                        }
                    }
                    if (repository == KogitoConstants.KOGITO_PIPELINES_REPOSITORY) {
                        scriptPath('dsl/seed/jobs/Jenkinsfile.seed.trigger')
                    } else {
                        scriptPath('.ci/jenkins/dsl/Jenkinsfile.seed')
                    }
                }
            }

            properties {
                githubProjectUrl("https://github.com/kiegroup/${repository}/")

                // pipelineTriggers {
                //     triggers {
                //         gitHubPushTrigger()
                //     }
                // }
            }
        }
        // Trigger jobs need to be executed once for the hook to work ...
        // jenkinsScript.queue(jobName)
        return job
    }
}
