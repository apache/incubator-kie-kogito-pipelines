// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

import org.kie.jenkins.jobdsl.utils.FolderUtils
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.utils.SeedJobUtils
import org.kie.jenkins.jobdsl.Utils

// Create all folders
folder("${GENERATION_BRANCH}")
if (Utils.isOldFolderStructure(this)) {
    // For old branches
    FolderUtils.getAllNeededFolders().each { folder("${GENERATION_BRANCH}/${it}") }
}

SeedJobUtils.createSeedJobTrigger(
    this,
    "${GENERATION_BRANCH}/z-seed-trigger-job",
    Utils.getSeedRepo(this),
    Utils.getSeedAuthor(this),
    Utils.getSeedAuthorCredsId(this),
    Utils.getSeedBranch(this),
    [
        'dsl/seed/gradle',
        'dsl/seed/jenkinsfiles/scripts',
        'dsl/seed/jenkinsfiles/Jenkinsfile.seed.branch',
        'dsl/seed/jobs/seed_job_branch.groovy',
        'dsl/seed/src',
        'dsl/seed/build.gradle',
        'dsl/seed/gradle.properties',
    ],
    "${JOB_NAME}")

SeedJobUtils.createSeedJobTrigger(
    this,
    "${GENERATION_BRANCH}/z-seed-config-trigger-job",
    "${SEED_CONFIG_FILE_GIT_REPOSITORY}",
    "${SEED_CONFIG_FILE_GIT_AUTHOR_NAME}",
    "${SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID}",
    "${SEED_CONFIG_FILE_GIT_BRANCH}",
    [
        "${SEED_CONFIG_FILE_PATH}",
    ],
    "${JOB_NAME}"
)

// Configuration of the seed and generated jobs is done via `dsl/seed/config.yaml`
pipelineJob("${GENERATION_BRANCH}/${JOB_NAME}") {
    description("This job creates all needed Jenkins jobs on branch ${GENERATION_BRANCH}. DO NOT USE FOR TESTING !!!! See https://github.com/apache/incubator-kie-kogito-pipelines/blob/main/docs/jenkins.md#test-specific-jobs")

    logRotator {
        numToKeep(5)
    }

    throttleConcurrentBuilds {
        maxTotal(1)
    }

    parameters {
        booleanParam('DEBUG', false, 'Enable Debug capability')

        booleanParam('SKIP_TESTS', false, 'Skip testing')
    }

    environmentVariables {
        env('GENERATION_BRANCH', "${GENERATION_BRANCH}")
        env('MAIN_BRANCHES', "${MAIN_BRANCHES}")

        env('SEED_CONFIG_FILE_GIT_REPOSITORY', "${SEED_CONFIG_FILE_GIT_REPOSITORY}")
        env('SEED_CONFIG_FILE_GIT_AUTHOR_NAME', "${SEED_CONFIG_FILE_GIT_AUTHOR_NAME}")
        env('SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID', "${SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID}")
        env('SEED_CONFIG_FILE_GIT_BRANCH', "${SEED_CONFIG_FILE_GIT_BRANCH}")
        env('SEED_CONFIG_FILE_PATH', "${SEED_CONFIG_FILE_PATH}")

        env('SEED_REPO', Utils.getSeedRepo(this))
        env('SEED_AUTHOR', Utils.getSeedAuthor(this))
        env('SEED_AUTHOR_CREDS_ID', Utils.getSeedAuthorCredsId(this))
        env('SEED_BRANCH', Utils.getSeedBranch(this))

        env('AGENT_LABEL', 'ubuntu')
        env('JENKINS_EMAIL_CREDS_ID', Utils.getJenkinsEmailCredsId(this))
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/${SEED_AUTHOR}/${SEED_REPO}.git')
                        credentials('${SEED_AUTHOR_CREDS_ID}')
                    }
                    branch('${SEED_BRANCH}')
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath("${SEED_JENKINSFILE}")
        }
    }

    properties {
        githubProjectUrl("https://github.com/${Utils.getSeedAuthor(this)}/${Utils.getSeedRepo(this)}/")
    }
}

// Toggle triggers job
folder("${GENERATION_BRANCH}/tools")
pipelineJob("${GENERATION_BRANCH}/tools/toggle-dsl-triggers") {
    description('Toggle DSL triggers')

    logRotator {
        numToKeep(5)
    }

    throttleConcurrentBuilds {
        maxTotal(1)
    }

    parameters {
        booleanParam('DISABLE_TRIGGERS', false, 'If selected the triggers will be disabled.')
    }

    environmentVariables {
        env('SEED_CONFIG_FILE_GIT_REPOSITORY', "${SEED_CONFIG_FILE_GIT_REPOSITORY}")
        env('SEED_CONFIG_FILE_GIT_AUTHOR_NAME', "${SEED_CONFIG_FILE_GIT_AUTHOR_NAME}")
        env('SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID', "${SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID}")
        env('SEED_CONFIG_FILE_GIT_BRANCH', "${SEED_CONFIG_FILE_GIT_BRANCH}")
        env('SEED_CONFIG_FILE_PATH', "${SEED_CONFIG_FILE_PATH}")

        env('JENKINS_EMAIL_CREDS_ID', Utils.getJenkinsEmailCredsId(this))
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url("https://github.com/${Utils.getSeedAuthor(this)}/${Utils.getSeedRepo(this)}.git")
                        credentials(Utils.getSeedAuthorCredsId(this))
                    }
                    branch(Utils.getSeedBranch(this))
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath("dsl/seed/jenkinsfiles/Jenkinsfile.tools.toggle-triggers")
        }
    }

    properties {
        githubProjectUrl("https://github.com/${Utils.getSeedAuthor(this)}/${Utils.getSeedRepo(this)}/")
    }
}
