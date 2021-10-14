package org.kie.jenkins.jobdsl

import groovy.json.JsonOutput

import org.kie.jenkins.jobdsl.templates.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils
import org.kie.jenkins.jobdsl.VersionUtils

/**
* Job utils
**/
class KogitoJobUtils {

    static def createVersionUpdateToolsJob(def script, String repository, String dependencyName, String notificationProject, def mavenUpdate = [:], def gradleUpdate = [:]) {
        def jobParams = KogitoJobTemplate.getCompletedJobParams(script,
                                                KogitoConstants.KOGITO_PIPELINES_REPOSITORY,
                                                "${repository}-update-${dependencyName.toLowerCase()}",
                                                FolderUtils.getToolsFolder(script),
                                                "${Utils.getJenkinsConfigPath(script, KogitoConstants.KOGITO_PIPELINES_REPOSITORY)}/Jenkinsfile.tools.update-dependency-version",
                                                "Update KIE version for ${notificationProject}")
        // Setup correct checkout branch for pipelines
        jobParams.git.branch = VersionUtils.getProjectTargetBranch(KogitoConstants.KOGITO_PIPELINES_REPOSITORY, jobParams.git.branch, repository)

        return KogitoJobTemplate.createPipelineJob(script, jobParams).with {
            parameters {
                stringParam('NEW_VERSION', '', 'Which version to set ?')
            }

            environmentVariables {
                env('JENKINS_EMAIL_CREDS_ID', Utils.getJenkinsEmailCredsId(script))

                env('DEPENDENCY_NAME', "${dependencyName}")
                env('REPO_NAME', "${repository}")
                env('NOTIFICATION_JOB_NAME', "${notificationProject}")

                env('PR_PREFIX_BRANCH', Utils.getGenerationBranch(script))

                env('BUILD_BRANCH_NAME', Utils.getGitBranch(script))
                env('GIT_AUTHOR',  Utils.getGitAuthor(script))
                env('AUTHOR_CREDS_ID', Utils.getGitAuthorCredsId(script))
                env('GITHUB_TOKEN_CREDS_ID', Utils.getGitAuthorTokenCredsId(script))

                if (mavenUpdate) {
                    mavenUpdate.modules ? env('MAVEN_MODULES', JsonOutput.toJson(mavenUpdate.modules)) : null
                    mavenUpdate.compare_deps_remote_poms ? env('MAVEN_COMPARE_DEPS_REMOTE_POMS', JsonOutput.toJson(mavenUpdate.compare_deps_remote_poms)) : null
                    mavenUpdate.properties ? env('MAVEN_PROPERTIES', JsonOutput.toJson(mavenUpdate.properties)) : null
                }
                if (gradleUpdate) {
                    gradleUpdate.regex ? env('GRADLE_REGEX', JsonOutput.toJson(gradleUpdate.regex)) : null
                }
            }
        }
    }

    static def createKie7UpdateToolsJob(def script, String repository, String notificationProject, def mavenUpdate = [:], def gradleUpdate = [:]) {
        return createVersionUpdateToolsJob(script, repository, 'KIE7', notificationProject, mavenUpdate, gradleUpdate)
    }

    static def createQuarkusUpdateToolsJob(def script, String repository, String notificationProject, def mavenUpdate = [:], def gradleUpdate = [:]) {
        return createVersionUpdateToolsJob(script, repository, 'Quarkus', notificationProject, mavenUpdate, gradleUpdate)
    }

    static def createBuildChainBranchJob(def script, def jobParams) {
        String checkedRepository = jobParams.git.repository
        
        // Setup correct values for build-chain checkout
        jobParams.git.repository = KogitoConstants.KOGITO_PIPELINES_REPOSITORY
        jobParams.git.branch = VersionUtils.getProjectTargetBranch(KogitoConstants.KOGITO_PIPELINES_REPOSITORY, jobParams.git.branch, checkedRepository)
        jobParams.jenkinsfile = Utils.getPipelinesJenkinsfilePath(script, KogitoConstants.BUILDCHAIN_JENKINSFILE)

        return KogitoJobTemplate.createPipelineJob(script, jobParams).with {
            parameters {
                stringParam('DISPLAY_NAME', '', 'Setup a specific build display name')

                stringParam('BUILDCHAIN_BRANCH', Utils.getGitBranch(script), 'Set the Git branch to test')
                stringParam('BUILDCHAIN_AUTHOR', Utils.getGitAuthor(script), 'Set the Git author to test')
            }

            environmentVariables {
                env('REPO_NAME', "${checkedRepository}")

                env('BUILDCHAIN_PROJECT', "kiegroup/${checkedRepository}")
                env('BUILDCHAIN_PR_TYPE', 'branch')
                env('BUILDCHAIN_CONFIG_BRANCH', jobParams.git.branch)

                env('MAVEN_SETTINGS_CONFIG_FILE_ID', Utils.getBindingValue(script, 'MAVEN_SETTINGS_FILE_ID'))
                env('MAVEN_DEPENDENCIES_REPOSITORY', Utils.getBindingValue(script, 'MAVEN_ARTIFACTS_REPOSITORY'))
            }
        }
    }
}
