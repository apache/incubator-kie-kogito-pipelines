package org.kie.jenkins

import org.junit.Test
import org.junit.Before
import org.junit.Rule
import org.junit.rules.ExpectedException

class TestJenkinsfileBuildchain extends SingleFileDeclarativePipelineTest {

    def buildChainLibMock = [:]
    def mailerLibMock = [:]
    def mavenLibMock = [:]
    def utilLibMock = [:]

    @Rule
    public ExpectedException thrown = ExpectedException.none()

    TestJenkinsfileBuildchain() {
        super('../Jenkinsfile.buildchain')
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        mockSharedLibraries('jenkins-pipeline-shared-libraries')
        setUpSharedLibrariesMocks()

        // Other mocks
        mockConfigFilePlugin('MAVEN_SETTINGS_FILE')
        mockJUnitPlugin()

        // Global variables
        addEnvVar('ghprbPullLink', 'ghprbPullLink')
        addEnvVar('GITHUB_TOKEN', 'GITHUB_TOKEN')
    }

    void setUpSharedLibrariesMocks() {
        mockSharedLibVarsCall('buildChain', 'getBuildChainVersionFromCompositeActionFile', { return 'VERSION' })

        mockSharedLibVarsCall('mailer', 'buildLogScriptPR')
        mockSharedLibVarsCall('mailer', 'sendEmail_failedPR')
        mockSharedLibVarsCall('mailer', 'sendEmail_unstablePR')
        mockSharedLibVarsCall('mailer', 'sendEmail_fixedPR')

        mockSharedLibVarsCall('maven', 'runMavenWithSettingsSonar', { settingsXmlId, command, token, logFile -> registerTestCallstack('maven.runMavenWithSettingsSonar', "${command}") })

        mockSharedLibVarsCall('pullrequest', 'postComment', { body -> registerTestCallstack('pullrequest.postComment') } )

        mockSharedLibVarsCall('util', 'spaceLeft')
        mockSharedLibVarsCall('util', 'cleanNode', { containerEngine -> registerTestCallstack('util.cleanNode', containerEngine) })
        mockSharedLibVarsCall('util', 'getProjectTriggeringJob', { [ 'group', 'project-name'] })
        mockSharedLibVarsCall('util', 'archiveConsoleLog', { id, lines -> registerTestCallstack('util.archiveConsoleLog') })
        mockSharedLibVarsCall('util', 'getMarkdownTestSummary', { id, addInfo, buildUrl, type -> registerTestCallstack('util.getMarkdownTestSummary') })
    }

    @Test
    void default_execution() throws Exception {
        runJenkinsfileAndAssertSuccess()

        assertEchoCall('label:kie-rhel7 && kie-mem16g && !master')
        assertTimeoutCall('180', 'MINUTES')
        assertShCall('npm install -g @kie/build-chain-action@VERSION')
        assertEnvironmentVariableEqual('BUILD_MVN_OPTS_CURRENT', '-Prun-code-coverage')
        assertConfigFileCall('kogito_pr_settings', 'MAVEN_SETTINGS_FILE')
        assertWithCredentialsStringCall('kie-ci1-token', 'GITHUB_TOKEN')
        assertShCall("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build pr -url=ghprbPullLink --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'")
        assertJunitCall('**/target/surefire-reports/**/*.xml,**/target/failsafe-reports/**/*.xml')
        assertArchiveArtifactsCall('**/cypress/screenshots/**,**/cypress/videos/**')
        assertTestCallstackDoesNotContain('util.archiveConsoleLog')
        assertStageCall('Sonar analysis')
        assertDirCall('bc/kiegroup_project_name/project-name')
        assertTestCallstackContains('maven.runMavenWithSettingsSonar')
        assertNoNotification()
        assertTestCallstackContains('util.cleanNode', 'docker')
    }

    @Test
    void with_buildchain_project() throws Exception {
        binding.getVariable('env').BUILDCHAIN_PROJECT = 'newgroup/new-project-name'
        mockSharedLibVarsCall('util', 'getProjectGroupName', { [ 'newgroup', 'new-project-name'] })

        runJenkinsfileAndAssertSuccess()

        assertShCall("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build pr -sp=newgroup/new-project-name -url=ghprbPullLink --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'")
        assertDirCall('bc/kiegroup_new_project_name/new-project-name')
    }

    @Test
    void change_label_env() throws Exception {
        binding.getVariable('env').ADDITIONAL_LABEL = 'anylabel'

        runJenkinsfileAndAssertSuccess()

        assertEchoCall('label:anylabel && !master')
    }

    @Test
    void change_timeout_env() throws Exception {
        binding.getVariable('env').ADDITIONAL_TIMEOUT = '360'

        runJenkinsfileAndAssertSuccess()

        assertTimeoutCall('360', 'MINUTES')
    }

    @Test
    void sonar_analysis_disabled() throws Exception {
        binding.getVariable('env').DISABLE_SONARCLOUD = 'true'

        runJenkinsfileAndAssertSuccess()

        assertSonarCloudDisabled()
    }

    @Test
    void downstream_build() throws Exception {
        binding.getVariable('env').DOWNSTREAM_BUILD = 'true'

        runJenkinsfileAndAssertSuccess()

        assertSonarCloudDisabled()
    }

    @Test
    void pr_build_chain_type() throws Exception {
        binding.getVariable('env').BUILDCHAIN_TYPE = 'pr'

        runJenkinsfileAndAssertSuccess()

        assertConfigFileCall('kogito_pr_settings', 'MAVEN_SETTINGS_FILE')
        assertShCall("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build pr -url=ghprbPullLink --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'")
    }

    @Test
    void pr_build_chain_type_failure() throws Exception {
        binding.getVariable('env').BUILDCHAIN_TYPE = 'pr'
        helper.addShMock("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build pr -url=ghprbPullLink --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'", '', 1)
        thrown.expect(Exception)

        runJenkinsfileAndAssertFailure()

        assertNotification()
    }

    @Test
    void branch_build_chain_type() throws Exception {
        binding.getVariable('env').BUILDCHAIN_TYPE = 'branch'
        binding.getVariable('env').GIT_BRANCH_NAME = 'branch_to_test'
        binding.getVariable('env').GIT_AUTHOR = 'owner'
        binding.getVariable('env').BUILDCHAIN_PROJECT = 'newgroup/new-project-name'
        mockSharedLibVarsCall('util', 'getProjectGroupName', { [ 'newgroup', 'new-project-name'] })

        runJenkinsfileAndAssertSuccess()

        assertConfigFileCall('kogito_release_settings', 'MAVEN_SETTINGS_FILE')
        assertShCall("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build branch -sp=newgroup/new-project-name -p=newgroup/new-project-name -b=branch_to_test -g=owner --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'")
        assertSonarCloudDisabled()
        assertNoNotification()
    }

    @Test
    void branch_build_chain_type_failure() throws Exception {
        binding.getVariable('env').BUILDCHAIN_TYPE = 'branch'
        binding.getVariable('env').GIT_BRANCH_NAME = 'branch_to_test'
        binding.getVariable('env').GIT_AUTHOR = 'owner'
        binding.getVariable('env').BUILDCHAIN_PROJECT = 'newgroup/new-project-name'
        mockSharedLibVarsCall('util', 'getProjectGroupName', { [ 'newgroup', 'new-project-name'] })
        helper.addShMock("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build branch -sp=newgroup/new-project-name -p=newgroup/new-project-name -b=branch_to_test -g=owner --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'", '', 1)
        thrown.expect(Exception)

        runJenkinsfileAndAssertFailure()

        assertNoNotification()
    }

    @Test
    void fdb_build_chain_type() throws Exception {
        binding.getVariable('env').BUILDCHAIN_TYPE = 'fdb'

        runJenkinsfileAndAssertSuccess()

        assertConfigFileCall('kogito_pr_settings', 'MAVEN_SETTINGS_FILE')
        assertShCall("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build fdb -url=ghprbPullLink --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'")
        assertSonarCloudDisabled()
    }

    @Test
    void fdb_build_chain_type_failure() throws Exception {
        binding.getVariable('env').BUILDCHAIN_TYPE = 'fdb'
        helper.addShMock("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build fdb -url=ghprbPullLink --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'", '', 1)
        thrown.expect(Exception)

        runJenkinsfileAndAssertFailure()

        assertNoNotification()
    }

    @Test
    void quarkus_execution() throws Exception {
        binding.getVariable('env').QUARKUS_BRANCH = '999.9'

        runJenkinsfileAndAssertSuccess()
        assertShCall("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-quarkus-config.yaml' -folder='bc' build pr -url=ghprbPullLink --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'")
    }

    @Test
    void native_env_execution() throws Exception {
        binding.getVariable('env').NATIVE = 'true'

        runJenkinsfileAndAssertSuccess()

        assertShCall("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build pr -url=ghprbPullLink --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'")
        assertEnvironmentVariableEqual('BUILD_MVN_OPTS_CURRENT', '-Prun-code-coverage -Dquarkus.native.container-build=true -Dnative -Pnative')
    }

    @Test
    void native_build_image_env_execution() throws Exception {
        binding.getVariable('env').NATIVE = 'true'
        binding.getVariable('env').NATIVE_BUILDER_IMAGE = 'builder_image_env'

        runJenkinsfileAndAssertSuccess()

        assertShCall("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build pr -url=ghprbPullLink --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'")
        assertEnvironmentVariableEqual('BUILD_MVN_OPTS_CURRENT', '-Prun-code-coverage -Dquarkus.native.container-build=true -Dnative -Pnative -Dquarkus.native.builder-image=builder_image_env')
    }

    @Test
    void skipTests() throws Exception {
        binding.getVariable('params').SKIP_TESTS = 'true'

        runJenkinsfileAndAssertSuccess()

        assertShCall("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build pr -url=ghprbPullLink --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'")
        assertEnvironmentVariableEqual('BUILD_MVN_OPTS_CURRENT', '-Prun-code-coverage -DskipTests')
    }

    @Test
    void skipIntegrationTests() throws Exception {
        binding.getVariable('params').SKIP_IT_TESTS = 'true'

        runJenkinsfileAndAssertSuccess()

        assertShCall("build-chain-action -token=GITHUB_TOKEN -df='https://raw.githubusercontent.com/radtriste/kogito-pipelines/\${BRANCH:main}/.ci/pull-request-config.yaml' -folder='bc' build pr -url=ghprbPullLink --skipParallelCheckout -cct '(^mvn .*)||\$1 -s MAVEN_SETTINGS_FILE -Dmaven.wagon.http.ssl.insecure=true -Dmaven.test.failure.ignore=true'")
        assertEnvironmentVariableEqual('BUILD_MVN_OPTS_CURRENT', '-Prun-code-coverage -DskipITs')
    }

    void assertNotification() {
        assertTestCallstackContains('util.getMarkdownTestSummary')
        assertTestCallstackContains('pullrequest.postComment')
    }

    void assertNoNotification() {
        assertTestCallstackDoesNotContain('util.getMarkdownTestSummary')
        assertTestCallstackDoesNotContain('pullrequest.postComment')
    }

    void assertSonarCloudDisabled() {
        assertEnvironmentVariableDoesNotContain('BUILD_MVN_OPTS_CURRENT', '-Prun-code-coverage')
        assertNoStageCall('Sonar analysis')
        assertTestCallstackDoesNotContain('maven.runMavenWithSettingsSonar')
    }

}
