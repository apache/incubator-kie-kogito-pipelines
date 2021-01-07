// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

String getSeedAuthor() {
    return SEED_AUTHOR ?: 'kiegroup'
}

String getSeedBranch() {
    return SEED_BRANCH ?: 'main'
}

// Configuration of the seed and generated jobs is done via `dsl/seed/config.yaml`
pipelineJob('0-seed-job') {
    description('This job creates all needed Jenkins jobs. DO NOT USE FOR TESTING !!!! See https://github.com/kiegroup/kogito-pipelines/blob/main/docs/jenkins.md#test-specific-jobs')

    logRotator {
        numToKeep(5)
    }

    throttleConcurrentBuilds {
        maxTotal(1)
    }

    // triggers {
    //     scm('H/15 * * * *')
    // }

    // wrappers {
    //     timestamps()
    //     colorizeOutput()
    //     preBuildCleanup()
    // }

    parameters {
        booleanParam('DEBUG', false, 'Enable Debug capability')

        booleanParam('SKIP_TESTS', false, 'Skip testing')

        stringParam('CUSTOM_BRANCH_KEY', '', 'To generate only some custom repos... Branch key to use for job generation. This is useful if you use')
        stringParam('CUSTOM_REPOSITORIES', '', 'To generate only some custom repos... Comma list of `repo[:branch]`. Example: `kogito-pipelines:any_change`. If no branch is given, then `main` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')
        stringParam('CUSTOM_AUTHOR', '', 'To generate only some custom repos... Define from from which author the custom repositories are checked out. If none given, then `SEED_AUTHOR` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')
        stringParam('CUSTOM_MAIN_BRANCH', '', 'To generate only some custom repos... If no main_branch is given, then DSL config `git.main_branch` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')

        stringParam('SEED_AUTHOR', 'kiegroup', 'If different from the default')
        stringParam('SEED_BRANCH', 'radtriste-patch-2', 'If different from the default')

        booleanParam('FORCE_REBUILD', false, 'Default, the job will scan for modified files and do the update in case some files are modified. In case you want to force the DSL generation')
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url("https://github.com/${getSeedAuthor()}/kogito-pipelines.git")
                        credentials('kie-ci')
                    }
                    branch(getSeedBranch())
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath('dsl/seed/jobs/Jenkinsfile.seed.main')
        }
    }

    properties {
        githubProjectUrl("https://github.com/${getSeedAuthor()}/kogito-pipelines/")

        pipelineTriggers {
            triggers {
                gitHubPushTrigger()
            }
        }
    }
}
