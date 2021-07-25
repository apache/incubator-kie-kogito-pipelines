// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// Configuration of the seed and generated jobs is done via `dsl/seed/config.yaml`
pipelineJob("${JOB_NAME}") {
    description("This job creates all needed Jenkins jobs on branch ${GENERATION_BRANCH}. DO NOT USE FOR TESTING !!!! See https://github.com/kiegroup/kogito-pipelines/blob/main/docs/jenkins.md#test-specific-jobs")

    logRotator {
        numToKeep(5)
    }

    throttleConcurrentBuilds {
        maxTotal(1)
    }

    parameters {
        booleanParam('DEBUG', false, 'Enable Debug capability')

        stringParam('CUSTOM_REPOSITORIES', "${CUSTOM_REPOSITORIES}", 'To generate only some custom repos... Comma list of `repo[:branch]`. Example: `kogito-pipelines:any_change`. If no branch is given, then `main` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')
        stringParam('CUSTOM_AUTHOR', "${CUSTOM_AUTHOR}", 'To generate only some custom repos... Define from from which author the custom repositories are checked out. If none given, then `SEED_AUTHOR` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')

        stringParam('SEED_AUTHOR', "${SEED_AUTHOR}", 'If different from the default')
        stringParam('SEED_BRANCH', "${SEED_BRANCH}", 'If different from the default')

        booleanParam('FORCE_REBUILD', false, 'Default, the job will scan for modified files and do the update in case some files are modified. In case you want to force the DSL generation')
    }

    environmentVariables {
        env('GENERATION_BRANCH', "${GENERATION_BRANCH}")
        env('MAIN_BRANCHES', "${MAIN_BRANCHES}")
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/${SEED_AUTHOR}/kogito-pipelines.git')
                        credentials('kie-ci')
                    }
                    branch('${SEED_BRANCH}')
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath('dsl/seed/jobs/Jenkinsfile.seed.branch')
        }
    }

    properties {
        githubProjectUrl("https://github.com/${SEED_AUTHOR}/kogito-pipelines/")

        pipelineTriggers {
            triggers {
                gitHubPushTrigger()
            }
        }
    }
}
