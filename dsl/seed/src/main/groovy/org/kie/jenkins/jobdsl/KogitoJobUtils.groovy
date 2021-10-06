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

}
