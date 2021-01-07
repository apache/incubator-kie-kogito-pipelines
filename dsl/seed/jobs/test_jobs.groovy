import org.kie.jenkins.jobdsl.model.Folder
import org.kie.jenkins.jobdsl.KogitoJobUtils
import org.kie.jenkins.jobdsl.Utils

Map getMultijobPRConfig(Folder jobFolder) {
    return [
        parallel: true,
        buildchain: true,
        jobs : [
            [
                id: 'kogito-runtimes',
                primary: true,
                env : [
                    // Sonarcloud analysis only on main branch
                    // As we have only Community edition
                    ENABLE_SONARCLOUD: Utils.isMainBranch(this),
                ]
            ], [
                id: 'kogito-apps',
                dependsOn: 'kogito-runtimes',
                repository: 'kogito-apps',
                env : [
                    ADDITIONAL_TIMEOUT: jobFolder.isNative() ? '360' : '210',
                ]
            ], [
                id: 'kogito-examples',
                dependsOn: 'kogito-runtimes',
                repository: 'kogito-examples'
            ]
        ],
    ]
}
KogitoJobUtils.createAllEnvsPerRepoPRJobs(this, { jobFolder -> getMultijobPRConfig(jobFolder) })

KogitoJobUtils.createAllJobsForMavenArtifactsRepository(this)

KogitoJobUtils.createAllEnvsBuildChainBuildAndTestJobs(this)
KogitoJobUtils.createAllEnvsMavenDeployArtifactsJobs(this)