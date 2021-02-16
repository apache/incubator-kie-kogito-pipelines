// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// Configuration of the seed and generated jobs is done via `dsl/seed/config.yaml`
pipelineJob("0-seed-job") {

    description("This job creates all needed Jenkins jobs")

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

        stringParam('CUSTOM_BRANCH_KEY', '', 'To generate only some custom repos... Branch key to use for job generation. This is useful if you use')
        stringParam('CUSTOM_REPOSITORIES', '', 'To generate only some custom repos... Comma list of `repo[:branch]`. Example: `kogito-pipelines:any_change`. If no branch is given, then `master` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')
        stringParam('CUSTOM_AUTHOR', '', 'To generate only some custom repos... Define from from which author the custom repositories are checked out. If none given, then `SEED_AUTHOR` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')
        stringParam('CUSTOM_MAIN_BRANCH', '', 'To generate only some custom repos... If no main_branch is given, then DSL config `git.main_branch` is taken. Ignored if `CUSTOM_BRANCH_KEY` is not set.')
        
        stringParam('SEED_AUTHOR', 'kiegroup', 'If different from the default')
        stringParam('SEED_BRANCH', 'master', 'If different from the default')
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
            scriptPath('dsl/seed/Jenkinsfile.seed')
        }
    }
}