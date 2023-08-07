// +++++++++++++++++++++++++++++++++++++++++++ root jobs ++++++++++++++++++++++++++++++++++++++++++++++++++++

import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.KogitoJobTemplate
import org.kie.jenkins.jobdsl.Utils

def jobParams = [
    job: [
        name: '0-prepare-release-branch',
        description: 'Prepare env for a release',
    ],
    git: [
        repository: Utils.getSeedRepo(this),
        author: Utils.getSeedAuthor(this),
        credentials: Utils.getSeedAuthorCredsId(this),
        branch: Utils.getSeedBranch(this),
    ],
    env: [:],
    jenkinsfile: 'dsl/seed/jenkinsfiles/Jenkinsfile.release.prepare',
]

KogitoJobTemplate.createPipelineJob(this, jobParams)?.with {
    parameters {
        RELEASE_PROJECTS.split(',').each { projectName ->
            stringParam("${projectName}_VERSION".toUpperCase(), '', "${Utils.getRepoNameCamelCase(projectName)} version to release as Major.minor.micro")
        }

        if (DEPENDENCY_PROJECTS) {
            DEPENDENCY_PROJECTS.split(',').each { projectName ->
                stringParam("${projectName}_VERSION".toUpperCase(), '', "${Utils.getRepoNameCamelCase(projectName)} dependency version which this will depend on")
            }
        }

        booleanParam('PRODUCTIZED_BRANCH', false, 'Is the created branch a productized one ?')
    }

    environmentVariables {
        env('JENKINS_EMAIL_CREDS_ID', Utils.getJenkinsEmailCredsId(this))

        env('SEED_CONFIG_FILE_GIT_REPOSITORY', "${SEED_CONFIG_FILE_GIT_REPOSITORY}")
        env('SEED_CONFIG_FILE_GIT_AUTHOR_NAME', "${SEED_CONFIG_FILE_GIT_AUTHOR_NAME}")
        env('SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID', "${SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID}")
        env('SEED_CONFIG_FILE_GIT_BRANCH', "${SEED_CONFIG_FILE_GIT_BRANCH}")
        env('SEED_CONFIG_FILE_PATH', "${SEED_CONFIG_FILE_PATH}")

        env('SEED_REPO', Utils.getSeedRepo(this))
        env('SEED_AUTHOR', Utils.getSeedAuthor(this))
        env('SEED_BRANCH', Utils.getSeedBranch(this))
        env('SEED_CREDENTIALS_ID', Utils.getSeedAuthorCredsId(this))
    }
}

def jobParamsRemove = [
    job: [
        name: '0-remove-branch',
        description: 'Remove branches',
    ],
    git: [
        repository: Utils.getSeedRepo(this),
        author: Utils.getSeedAuthor(this),
        credentials: Utils.getSeedAuthorCredsId(this),
        branch: Utils.getSeedBranch(this),
    ],
    env: [:],
    jenkinsfile: 'dsl/seed/jenkinsfiles/Jenkinsfile.remove.branches',
]

List nonMainBranches = ALL_BRANCHES.split(',').findAll { it != MAIN_BRANCH_NAME }
if (nonMainBranches) {
    KogitoJobTemplate.createPipelineJob(this, jobParamsRemove)?.with {
        parameters {
            choiceParam('BRANCH_TO_REMOVE', nonMainBranches, 'Which release branch to remove ?') 
        }

        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', Utils.getJenkinsEmailCredsId(this))

            env('GIT_REPOSITORY', "${SEED_CONFIG_FILE_GIT_REPOSITORY}")
            env('GIT_AUTHOR', "${SEED_CONFIG_FILE_GIT_AUTHOR_NAME}")
            env('GIT_AUTHOR_CREDENTIALS_ID', "${SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID}")
            env('GIT_BRANCH_TO_BUILD', "${SEED_CONFIG_FILE_GIT_BRANCH}")
            env('CONFIG_FILE_PATH', "${SEED_CONFIG_FILE_PATH}")
        }
    }
} else {
    println 'No branches to remove ...'
}

def jobParamsProd = [
    job: [
        name: '0-prepare-productized-branch',
        description: 'Prepare productized branch',
    ],
    git: [
        repository: Utils.getSeedRepo(this),
        author: Utils.getSeedAuthor(this),
        credentials: Utils.getSeedAuthorCredsId(this),
        branch: Utils.getSeedBranch(this),
    ],
    env: [:],
    jenkinsfile: 'dsl/seed/jenkinsfiles/Jenkinsfile.prod.prepare',
]

List communityReleaseBranches = ALL_BRANCHES.split(',').findAll { it != MAIN_BRANCH_NAME && !it.endsWith('-prod') }
if (communityReleaseBranches) {
    KogitoJobTemplate.createPipelineJob(this, jobParamsProd)?.with {
        parameters {
            PRODUCTIZED_PROJECTS.split(',').each { projectName ->
                choiceParam("${projectName}_RELEASE_BRANCH".toUpperCase(), communityReleaseBranches, "${Utils.getRepoNameCamelCase(projectName)} community branch to which to create the productized branch from")
                stringParam("${projectName}_PROD_BRANCH_SUFFIX".toUpperCase(), 'prod', "${Utils.getRepoNameCamelCase(projectName)} productized branch suffix")
                stringParam("${projectName}_UPDATE_VERSION".toUpperCase(), '', "${Utils.getRepoNameCamelCase(projectName)} dependency version which this will depend on")
            }

            if (DEPENDENCY_PROJECTS) {
                DEPENDENCY_PROJECTS.split(',').each { projectName ->
                    stringParam("${projectName}_UPDATE_VERSION".toUpperCase(), '', "${Utils.getRepoNameCamelCase(projectName)} dependency version which this will depend on")
                }
            }

            stringParam('QUARKUS_VERSION', '', 'Quarkus version to which to update all productized branches, usually latest LTS version')
            stringParam('DOWNGRADE_QUARKUS_PR_BRANCH', '', 'Which PR branch name to use for Quarkus downgrade? If none given, a name will be generated automatically.')
            stringParam('ADDITIONAL_BUILD_MAVEN_OPTS', '', 'Additional default maven opts for jenkins jobs, e.g., -Ddata-index-ephemeral.image=quay.io/kiegroup/kogito-data-index-ephemeral')
            stringParam('PROJECTS_TO_REMOVE_FROM_PR_CHECKS', '', 'Comma-separated list of projects (<owner>/<repo>) to be removed/disabled from build chain pull request config')

            booleanParam('DRY_RUN', false, 'If enabled no changes will be applied to remote branches')
        }

        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', Utils.getJenkinsEmailCredsId(this))

            env('SEED_CONFIG_FILE_GIT_REPOSITORY', "${SEED_CONFIG_FILE_GIT_REPOSITORY}")
            env('SEED_CONFIG_FILE_GIT_AUTHOR_NAME', "${SEED_CONFIG_FILE_GIT_AUTHOR_NAME}")
            env('SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID', "${SEED_CONFIG_FILE_GIT_AUTHOR_CREDS_ID}")
            env('SEED_CONFIG_FILE_GIT_BRANCH', "${SEED_CONFIG_FILE_GIT_BRANCH}")
            env('SEED_CONFIG_FILE_PATH', "${SEED_CONFIG_FILE_PATH}")

            env('SEED_REPO', Utils.getSeedRepo(this))
            env('SEED_AUTHOR', Utils.getSeedAuthor(this))
            env('SEED_BRANCH', Utils.getSeedBranch(this))
            env('SEED_CREDENTIALS_ID', Utils.getSeedAuthorCredsId(this))
        }
    }
} else {
    println 'No branches to productize ...'
}