package org.kie.jenkins

import org.junit.Test
import org.junit.Before
import org.junit.Rule
import org.junit.rules.ExpectedException

class TestJenkinsfileNightlyArtifacts extends SingleFileDeclarativePipelineTest {

    def buildChainLibMock = [:]
    def mailerLibMock = [:]
    def mavenLibMock = [:]
    def utilLibMock = [:]

    @Rule
    public ExpectedException thrown = ExpectedException.none()

    TestJenkinsfileNightlyArtifacts() {
        super('../Jenkinsfile.nightly.artifacts')
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        mockSharedLibraries('jenkins-pipeline-shared-libraries')
        setUpSharedLibrariesMocks()

        addParam('SKIP_TESTS', false)
        addParam('SKIP_INTEGRATION_TESTS', true)
        addEnvVar('GIT_BRANCH_NAME', 'BRANCH')
        addEnvVar('JENKINS_EMAIL_CREDS_ID', 'KOGITO_CI_EMAIL_TO')

        helper.addShMock('date -u "+%Y-%m-%d"', 'date', 0)

        helper.registerAllowedMethod('credentials', [String.class], { str -> return str })
    }

    void setUpSharedLibrariesMocks() {
        mockSharedLibVarsCall('mailer', 'sendMarkdownTestSummaryNotification', { jobId, subject, emails, addInfo, buildUrl -> registerTestCallstack('mailer.sendMarkdownTestSummaryNotification', "${jobId}, ${subject}, ${emails}, ${addInfo}, ${buildUrl}") })
    }

    @Test
    void default_execution() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map -> return [
            result: 'SUCCESS',
            absoluteUrl: 'URL',
        ] })

        runJenkinsfileAndAssertSuccess()

        assertBuildAndTestBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [ UPDATE_NIGHTLY_BRANCH: true ],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:],
            'optaplanner-quickstarts': [:],
        ])

        assertDeployArtifactsBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [:],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:]
        ])
        assertNoBuildCall('./optaplanner-quickstarts.deploy-artifacts')
    }

    @Test
    void skip_tests() throws Exception {
        addParam('SKIP_TESTS', true)
        helper.registerAllowedMethod('build', [Map.class], { map -> return [
            result: 'SUCCESS',
            absoluteUrl: 'URL',
        ] })

        runJenkinsfileAndAssertSuccess()

        assertBuildAndTestBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [ UPDATE_NIGHTLY_BRANCH: true ],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:],
            'optaplanner-quickstarts': [:],
        ], true)

        assertDeployArtifactsBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [:],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:]
        ])
        assertNoBuildCall('./optaplanner-quickstarts.deploy-artifacts')
    }

    @Test
    void build_and_test_failing() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map ->
            if (map.get('job') == './drools.build-and-test' || map.get('job') == './kogito-runtimes.build-and-test') {
                return [
                    result: 'FAILURE',
                    absoluteUrl: 'URL',
                ]
            }
            return [
                result: 'SUCCESS',
                absoluteUrl: 'URL',
            ]
        })

        runJenkinsfileAndAssertUnstable()

        assertBuildAndTestBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [ UPDATE_NIGHTLY_BRANCH: true ],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:],
            'optaplanner-quickstarts': [:],
        ])

        assertDeployArtifactsBuildCalls([
            'kogito-apps' : [:],
            'kogito-examples': [:],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:]
        ])
        assertNoDeployArtifactsBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'optaplanner-quickstarts' : [:],
        ])

        assertTestCallstackContains('mailer.sendMarkdownTestSummaryNotification', 'Nightly, [BRANCH] Drools, [KOGITO_CI_EMAIL_TO], , URL')
        assertTestCallstackContains('mailer.sendMarkdownTestSummaryNotification', 'Nightly, [BRANCH] Kogito Runtimes, [KOGITO_CI_EMAIL_TO], , URL')
    }

    @Test
    void build_and_test_unstable() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map ->
            if (map.get('job') == './drools.build-and-test' || map.get('job') == './kogito-apps.build-and-test') {
                return [
                    result: 'UNSTABLE',
                    absoluteUrl: 'URL',
                ]
            }
            return [
                result: 'SUCCESS',
                absoluteUrl: 'URL',
            ]
        })

        runJenkinsfileAndAssertUnstable()

        assertBuildAndTestBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [ UPDATE_NIGHTLY_BRANCH: true ],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:],
            'optaplanner-quickstarts': [:],
        ])

        assertDeployArtifactsBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [:],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:]
        ])
        assertNoDeployArtifactsBuildCalls([
            'optaplanner-quickstarts' : [:],
        ])

        assertTestCallstackContains('mailer.sendMarkdownTestSummaryNotification', 'Nightly, [BRANCH] Drools, [KOGITO_CI_EMAIL_TO], , URL')
        assertTestCallstackContains('mailer.sendMarkdownTestSummaryNotification', 'Nightly, [BRANCH] Kogito Apps, [KOGITO_CI_EMAIL_TO], , URL')
    }

    @Test
    void deploy_failing() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map ->
            if (map.get('job') == './drools.deploy-artifacts' || map.get('job') == './optaplanner.deploy-artifacts') {
                return [
                    result: 'FAILURE',
                    absoluteUrl: 'URL',
                ]
            }
            return [
                result: 'SUCCESS',
                absoluteUrl: 'URL',
            ]
        })

        runJenkinsfileAndAssertUnstable()

        assertBuildAndTestBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [ UPDATE_NIGHTLY_BRANCH: true ],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:],
            'optaplanner-quickstarts': [:],
        ])

        assertDeployArtifactsBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [:],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:]
        ])
        assertNoDeployArtifactsBuildCalls([
            'optaplanner-quickstarts' : [:],
        ])

        assertTestCallstackContains('mailer.sendMarkdownTestSummaryNotification', 'Nightly, [BRANCH] Drools, [KOGITO_CI_EMAIL_TO], , URL')
        assertTestCallstackContains('mailer.sendMarkdownTestSummaryNotification', 'Nightly, [BRANCH] Optaplanner, [KOGITO_CI_EMAIL_TO], , URL')
    }

    @Test
    void deploy_unstable() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map ->
            if (map.get('job') == './drools.deploy-artifacts' || map.get('job') == './optaweb-vehicle-routing.deploy-artifacts') {
                return [
                    result: 'UNSTABLE',
                    absoluteUrl: 'URL',
                ]
            }
            return [
                result: 'SUCCESS',
                absoluteUrl: 'URL',
            ]
        })

        runJenkinsfileAndAssertUnstable()

        assertBuildAndTestBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [ UPDATE_NIGHTLY_BRANCH: true ],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:],
            'optaplanner-quickstarts': [:],
        ])

        assertDeployArtifactsBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [:],
            'optaplanner': [:],
            'optaweb-vehicle-routing': [:],
            'optaweb-employee-rostering': [:]
        ])
        assertNoDeployArtifactsBuildCalls([
            'optaplanner-quickstarts' : [:],
        ])

        assertTestCallstackContains('mailer.sendMarkdownTestSummaryNotification', 'Nightly, [BRANCH] Drools, [KOGITO_CI_EMAIL_TO], , URL')
        assertTestCallstackContains('mailer.sendMarkdownTestSummaryNotification', 'Nightly, [BRANCH] Optaweb Vehicle Routing, [KOGITO_CI_EMAIL_TO], , URL')
    }

    @Test
    void pipeline_failure() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map -> throw new Exception('expected') })
        addParam('BUILD_URL', 'BUILD_URL')
        thrown.expect(Exception)

        runJenkinsfileAndAssertFailure()

        assertTestCallstackContains('mailer.sendMarkdownTestSummaryNotification', 'Nightly, [BRANCH], [KOGITO_CI_EMAIL_TO], , BUILD_URL')
    }

    void assertBuildAndTestBuildCalls(Map projects, boolean skipTests = false) {
        assertBuildCalls(projects, 'build-and-test', [
                DISPLAY_NAME: 'BRANCH-date',
                SKIP_TESTS: skipTests,
                SKIP_INTEGRATION_TESTS: skipTests,
            ])
    }

    void assertDeployArtifactsBuildCalls(Map projects) {
        assertBuildCalls(projects, 'deploy-artifacts', [
                DISPLAY_NAME: 'BRANCH-date',
            ])
    }

    void assertNoDeployArtifactsBuildCalls(Map projects) {
        assertNoBuildCalls(projects, 'deploy-artifacts')
    }

    void assertBuildCalls(Map projects, String jobSuffix, Map defaultBuildParams) {
        projects.each { projectName, extraParams ->
            def buildParams = [:]
            buildParams.putAll(defaultBuildParams)
            if (extraParams) {
                buildParams.putAll(extraParams)
            }
            assertBuildCall("./${projectName}.${jobSuffix}", buildParams, true, false)
        }
    }

    void assertNoBuildCalls(Map projects, String jobSuffix) {
        projects.each { projectName, extraParams ->
            assertNoBuildCall("./${projectName}.${jobSuffix}")
        }
    }

}
