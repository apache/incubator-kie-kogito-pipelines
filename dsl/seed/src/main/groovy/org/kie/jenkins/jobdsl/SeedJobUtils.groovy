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
                booleanParam('FORCE_REBUILD', false, 'Default, the job will scan for modified files and do the update in case some files are modified. In case you want to force the DSL generation')
            }

            environmentVariables {
                env('REPO_NAME', repository)
                env('GIT_AUTHOR', gitAuthor)
                env('GIT_BRANCH_NAME', gitBranch)

                env('JOB_RELATIVE_PATH_TO_TRIGGER', jobRelativePathToTrigger)
                env('LISTEN_TO_MODIFIED_PATHS', new groovy.json.JsonBuilder(pathsToListen).toString())
            }

            definition {
                cps {
                    script(jenkinsScript.readFileFromWorkspace('jenkinsfiles/Jenkinsfile.seed.trigger'))
                }
            }

            properties {
                githubProjectUrl("https://github.com/${gitAuthor}/${repository}")

                pipelineTriggers {
                    triggers {
                        githubPush()
                    }
                }
            }
        }
        // Trigger jobs need to be executed once for the hook to work ...
        jenkinsScript.queue(jobName)
        return job
    }
}
