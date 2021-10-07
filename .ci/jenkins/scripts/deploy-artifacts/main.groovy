import org.kie.jenkins.MavenCommand
import org.kie.jenkins.MavenStagingHelper

void execute(def pipelinesCommon) {
    echo 'Execute default deploy'
    retry (3) {
        deployArtifacts(pipelinesCommon, getMavenCommand(pipelinesCommon))
    }
}

MavenCommand getMavenCommand(def pipelinesCommon) {
    pipelinesCommon.getDefaultMavenCommand()
                .inDirectory(pipelinesCommon.getRepoName())
                .withProperty('full')
}

void deployArtifacts(def pipelinesCommon, MavenCommand mavenCommand) {
    if (shouldStageArtifacts(pipelinesCommon) || isSpecificArtifactsUpload()) {
        runMavenDeploy(pipelinesCommon, mavenCommand, true)

        if (isSpecificArtifactsUpload()) {
            // Deploy to specific repository with credentials
            // TODO set back
            // maven.uploadLocalArtifacts(env.MAVEN_REPO_CREDS_ID, getLocalDeploymentFolder(pipelinesCommon.getRepoName()), getMavenRepoZipUrl())
            echo 'maven.uploadLocalArtifacts'
        } else {
            // Stage release artifacts
            runMavenStage(pipelinesCommon, mavenCommand)
        }
    } else if (shouldDeployToRepository(pipelinesCommon)) {
        runMavenDeploy(pipelinesCommon, mavenCommand)
    }
}

void runMavenDeploy(def pipelinesCommon, MavenCommand mavenCommand, boolean localDeployment = false) {
    if (localDeployment) {
        mavenCommand.withLocalDeployFolder(getLocalDeploymentFolder(pipelinesCommon.getRepoName()))
    } else if (getMavenDeployRepository()) {
        mavenCommand.withDeployRepository(getMavenDeployRepository())
    }

    // TODO set back
    echo mavenCommand.skipTests(true).withProperty('skipITs').getFullRunCommand('deploy')
// mavenCommand.skipTests(true).run('deploy')
}

void runMavenStage(def pipelinesCommon, MavenCommand mavenCommand) {
    MavenStagingHelper stagingHelper = getStagingHelper(mavenCommand)
    // TODO set back
    echo 'Stage and promote artifacts'
// stagingHelper.stageLocalArtifacts(env.NEXUS_STAGING_PROFILE_ID, getLocalDeploymentFolder(pipelinesCommon.getRepoName())).each { setPipelinePropertyIfNeeded(it.key, it.value) }
// stagingHelper.promoteStagingRepository(env.NEXUS_BUILD_PROMOTION_PROFILE_ID)
}

MavenStagingHelper getStagingHelper(MavenCommand mavenCommand) {
    return new MavenStagingHelper(this, mavenCommand)
        .withNexusReleaseUrl(env.NEXUS_RELEASE_URL)
        .withNexusReleaseRepositoryId(env.NEXUS_RELEASE_REPOSITORY_ID)
}

String getLocalDeploymentFolder(String repoName) {
    return "${mavenDeployLocalDir}/${repoName}"
}

String getMavenRepoZipUrl() {
    return "${getMavenDeployRepository().replaceAll('/content/', '/service/local/').replaceFirst('/*$', '')}/content-compressed"
}

boolean isSpecificArtifactsUpload() {
    return getMavenDeployRepository() && env.MAVEN_REPO_CREDS_ID
}

boolean shouldStageArtifacts(def pipelinesCommon) {
    return !isSpecificArtifactsUpload() && pipelinesCommon.isRelease() && !getMavenDeployRepository()
}

boolean shouldDeployToRepository(def pipelinesCommon) {
    return getMavenDeployRepository() || pipelinesCommon.getGitAuthor() == 'kiegroup'
}

String getMavenDeployRepository() {
    return env.MAVEN_DEPLOY_REPOSITORY
}

List getCommands(String json) {
    return json ? readJSON(text: json) : []
}

return this
