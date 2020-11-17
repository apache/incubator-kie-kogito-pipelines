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
    }


    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/kiegroup/kogito-pipelines.git')
                        credentials('kie-ci')
                    }
                    branch('master')
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath('dsl/seed/Jenkinsfile.seed')
        }
    }
}