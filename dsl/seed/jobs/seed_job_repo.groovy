import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.SeedJobUtils

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

SeedJobUtils.createSeedJobTrigger(
    this,
    "${JOB_NAME}-trigger",
    "${REPO_NAME}",
    "${GIT_AUTHOR}",
    "${GIT_BRANCH}",
    [ "${GIT_JENKINS_CONFIG_PATH}" ],
    "${JOB_NAME}")

// Configuration of the seed and generated jobs is done via `dsl/seed/config.yaml`
pipelineJob("${JOB_NAME}") {
    description("This job creates all needed Jenkins jobs on branch ${GENERATION_BRANCH} and repository ${REPO_NAME}. DO NOT USE FOR TESTING !!!! See https://github.com/kiegroup/kogito-pipelines/blob/main/docs/jenkins.md#test-specific-jobs")

    logRotator {
        numToKeep(5)
    }

    throttleConcurrentBuilds {
        maxTotal(1)
    }

    parameters {
        booleanParam('DEBUG', false, 'Enable Debug capability')

        booleanParam('SKIP_TESTS', false, 'Skip testing')

        stringParam('SEED_AUTHOR', "${SEED_AUTHOR}", 'If different from the default')
        stringParam('SEED_BRANCH', "${SEED_BRANCH}", 'If different from the default')
    }

    environmentVariables {
        env('SEED_REPO', KogitoConstants.KOGITO_PIPELINES_REPOSITORY)
        env('JOB_TYPE', 'GENERATE')

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
    }
}
