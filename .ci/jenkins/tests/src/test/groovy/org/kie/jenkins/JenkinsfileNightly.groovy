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

        addParam('SKIP_TESTS', false)
        addParam('SKIP_INTEGRATION_TESTS', true)
        addEnvVar('GIT_BRANCH_NAME', 'BRANCH')
        addEnvVar('GIT_AUTHOR', 'AUTHOR')
        addEnvVar('GIT_AUTHOR_CREDS_ID', 'AUTHOR_CREDS_ID')
        addEnvVar('JENKINS_EMAIL_CREDS_ID', 'KOGITO_CI_EMAIL_TO')
        addEnvVar('STAGE_NAME', 'STAGE_NAME')
        addEnvVar('BUILD_NUMBER', 'BUILD_NUMBER')
        addEnvVar('BUILD_URL', 'BUILD_URL')

        helper.addShMock('date -u "+%Y-%m-%d"', 'date', 0)

        helper.registerAllowedMethod('credentials', [String.class], { str -> return str })
        helper.registerAllowedMethod('emailext', [Map.class], { str -> return 'emailext' })

        mockSharedLibraries('jenkins-pipeline-shared-libraries')
        mockSharedLibVarsCall('githubscm', 'resolveRepository', { repo, author, branch, ignoreErrors, credsId -> registerTestCallstack('githubscm.resolveRepository', "${repo}, ${author}, ${branch}, ${ignoreErrors}, ${credsId}") })
        mockSharedLibVarsCall('githubscm', 'createBranch', { branch -> registerTestCallstack('githubscm.createBranch', "${branch}") })
        mockSharedLibVarsCall('githubscm', 'pushObject', { remote, branch, credsId -> registerTestCallstack('githubscm.pushObject', "${remote}, ${branch}, ${credsId}") })

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
            'kogito-examples': [:],
        ])
        assertTestCallstackContains('githubscm.resolveRepository', 'kogito-examples, AUTHOR, BRANCH, false, AUTHOR_CREDS_ID')
        assertTestCallstackContains('githubscm.createBranch', 'nightly-BRANCH')
        assertTestCallstackContains('githubscm.pushObject', 'origin, nightly-BRANCH, AUTHOR_CREDS_ID')
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
            'kogito-examples': [:],
        ], true)

        assertTestCallstackContains('githubscm.resolveRepository', 'kogito-examples, AUTHOR, BRANCH, false, AUTHOR_CREDS_ID')
        assertTestCallstackContains('githubscm.createBranch', 'nightly-BRANCH')
        assertTestCallstackContains('githubscm.pushObject', 'origin, nightly-BRANCH, AUTHOR_CREDS_ID')
    }

    @Test
    void deploy_failing() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map ->
            if (map.get('job') == 'drools.build-and-deploy' || map.get('job') == 'kogito-runtimes.build-and-deploy') {
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
            'kogito-examples': [:],
        ])

        assertTestCallstackContains('githubscm.resolveRepository', 'kogito-examples, AUTHOR, BRANCH, false, AUTHOR_CREDS_ID')
        assertTestCallstackContains('githubscm.createBranch', 'nightly-BRANCH')
        assertTestCallstackContains('githubscm.pushObject', 'origin, nightly-BRANCH, AUTHOR_CREDS_ID')
    }

    @Test
    void deploy_unstable() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map ->
            if (map.get('job') == 'drools.build-and-deploy' || map.get('job') == 'kogito-apps.build-and-deploy') {
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
            'kogito-examples': [:],
        ])

        assertTestCallstackContains('githubscm.resolveRepository', 'kogito-examples, AUTHOR, BRANCH, false, AUTHOR_CREDS_ID')
        assertTestCallstackContains('githubscm.createBranch', 'nightly-BRANCH')
        assertTestCallstackContains('githubscm.pushObject', 'origin, nightly-BRANCH, AUTHOR_CREDS_ID')
    }

    @Test
    void pipeline_failure() throws Exception {
        helper.registerAllowedMethod('build', [Map.class], { map -> throw new Exception('expected') })
        addParam('BUILD_URL', 'BUILD_URL')
        thrown.expect(Exception)

        runJenkinsfileAndAssertFailure()

        assertTestCallstackDoesNotContain('githubscm.resolveRepository')
        assertTestCallstackDoesNotContain('githubscm.createBranch')
        assertTestCallstackDoesNotContain('githubscm.pushObject')
    }

    void assertDeployBuildCalls(Map projects, boolean skipTests = false) {
        assertBuildCalls(projects, '.build-and-deploy', [
                DISPLAY_NAME: 'BRANCH-date',
                SEND_NOTIFICATION: true,
                SKIP_TESTS: skipTests,
                SKIP_INTEGRATION_TESTS: skipTests,
            ])
    }

    void assertBuildCalls(Map projects, String jobSuffix, Map defaultBuildParams) {
        projects.each { projectName, extraParams ->
            def buildParams = [:]
            buildParams.putAll(defaultBuildParams)
            if (extraParams) {
                buildParams.putAll(extraParams)
            }
            assertBuildCall("${projectName}${jobSuffix}", buildParams, true, false)
        }
    }

}
