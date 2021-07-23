import org.kie.jenkins.jobdsl.Utils

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// Configuration of the seed and generated jobs is done via `dsl/seed/config.yaml`
pipelineJob("${JOB_NAME}") {
    description("This job creates all needed Jenkins jobs on branch ${GENERATION_BRANCH} and repository ${REPO_NAME}. DO NOT USE FOR TESTING !!!! See https://github.com/kiegroup/kogito-pipelines/blob/master/docs/jenkins.md#test-specific-jobs")

    logRotator {
        numToKeep(5)
    }

    throttleConcurrentBuilds {
        maxTotal(1)
    }

    parameters {
        booleanParam('DEBUG', false, 'Enable Debug capability')

        stringParam('SEED_AUTHOR', "${SEED_AUTHOR}", 'If different from the default')
        stringParam('SEED_BRANCH', "${SEED_BRANCH}", 'If different from the default')

        booleanParam('FORCE_REBUILD', false, 'Default, the job will scan for modified files and do the update in case some files are modified. In case you want to force the DSL generation')
    }

    environmentVariables {
        env('SEED_REPO', 'kogito-pipelines')

        env('REPO_NAME', "${REPO_NAME}")
        env('GIT_BRANCH', "${GIT_BRANCH}")
        env('GIT_AUTHOR', "${GIT_AUTHOR}")

        env('GENERATION_BRANCH', "${GENERATION_BRANCH}")
        env('GIT_MAIN_BRANCH', "${GIT_MAIN_BRANCH}")

        env('SEED_SCRIPTS_FILEPATH', 'dsl/seed/jobs/scripts/seed_repo_generation.groovy')
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url("https://github.com/${GIT_AUTHOR}/${REPO_NAME}.git")
                        credentials('kie-ci')
                    }
                    branch("${GIT_BRANCH}")
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath("${GIT_JENKINS_CONFIG_PATH}/dsl/Jenkinsfile.seed")
        }
    }

    properties {
        githubProjectUrl("https://github.com/${GIT_AUTHOR}/${REPO_NAME}/")
        
        pipelineTriggers {
            triggers {
                gitHubPushTrigger()
            }
        }
    }
}
