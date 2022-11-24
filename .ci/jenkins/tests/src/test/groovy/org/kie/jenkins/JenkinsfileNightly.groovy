package org.kie.jenkins

import org.junit.Test
import org.junit.Before
import org.junit.Rule
import org.junit.rules.ExpectedException

class TestJenkinsfileNightly extends SingleFileDeclarativePipelineTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none()

    TestJenkinsfileNightly() {
        super('../Jenkinsfile.nightly')
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        mockSharedLibraries('jenkins-pipeline-shared-libraries')

        addParam('SKIP_TESTS', false)
        addParam('SKIP_INTEGRATION_TESTS', true)
        addEnvVar('GIT_BRANCH_NAME', 'BRANCH')
        addEnvVar('JENKINS_EMAIL_CREDS_ID', 'KOGITO_CI_EMAIL_TO')
        addEnvVar('STAGE_NAME', 'STAGE_NAME')
        addEnvVar('BUILD_NUMBER', 'BUILD_NUMBER')
        addEnvVar('BUILD_URL', 'BUILD_URL')

        helper.addShMock('date -u "+%Y-%m-%d"', 'date', 0)

        helper.registerAllowedMethod('credentials', [String.class], { str -> return str })
        helper.registerAllowedMethod('emailext', [Map.class], { str -> return 'emailext' })
    }

    @Test
    void default_execution() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map -> return [
            result: 'SUCCESS',
            absoluteUrl: 'URL',
        ] })

        runJenkinsfileAndAssertSuccess()

        assertDeployBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [ UPDATE_NIGHTLY_BRANCH: true ],
        ])
    }

    @Test
    void skip_tests() throws Exception {
        addParam('SKIP_TESTS', true)
        helper.registerAllowedMethod('build', [Map.class], { map -> return [
            result: 'SUCCESS',
            absoluteUrl: 'URL',
        ] })

        runJenkinsfileAndAssertSuccess()

        assertDeployBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [ UPDATE_NIGHTLY_BRANCH: true ],
        ], true)
    }

    @Test
    void deploy_failing() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map ->
            if (map.get('job') == 'drools-deploy' || map.get('job') == 'kogito-runtimes-deploy') {
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

        assertDeployBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [ UPDATE_NIGHTLY_BRANCH: true ],
        ])
    }

    @Test
    void deploy_unstable() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map ->
            if (map.get('job') == 'drools.deploy' || map.get('job') == 'kogito-apps-deploy') {
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

        assertDeployBuildCalls([
            'drools' : [:],
            'kogito-runtimes' : [:],
            'kogito-apps' : [:],
            'kogito-examples': [ UPDATE_NIGHTLY_BRANCH: true ],
        ])
    }

    @Test
    void pipeline_failure() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map -> throw new Exception('expected') })
        addParam('BUILD_URL', 'BUILD_URL')
        thrown.expect(Exception)

        runJenkinsfileAndAssertFailure()
    }

    void assertDeployBuildCalls(Map projects, boolean skipTests = false) {
        assertBuildCalls(projects, 'deploy', [
                DISPLAY_NAME: 'BRANCH-date',
                SEND_NOTIFICATION: true,
                SKIP_TESTS: skipTests,
            ])
    }

    void assertBuildCalls(Map projects, String jobSuffix, Map defaultBuildParams) {
        projects.each { projectName, extraParams ->
            def buildParams = [:]
            buildParams.putAll(defaultBuildParams)
            if (extraParams) {
                buildParams.putAll(extraParams)
            }
            assertBuildCall("${projectName}-${jobSuffix}", buildParams, true, false)
        }
    }

}
