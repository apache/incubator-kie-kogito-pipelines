package org.kie.jenkins

import org.junit.Test
import org.junit.Before
import org.junit.Rule
import org.junit.rules.ExpectedException
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError

class TestJenkinsfileEcosystemUpdateQuarkus extends SingleFileDeclarativePipelineTest {

    def buildChainLibMock = [:]
    def mailerLibMock = [:]
    def mavenLibMock = [:]
    def utilLibMock = [:]

    @Rule
    public ExpectedException thrown = ExpectedException.none()

    TestJenkinsfileEcosystemUpdateQuarkus() {
        super('../Jenkinsfile.ecosystem.update-quarkus')
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        mockSharedLibraries('jenkins-pipeline-shared-libraries')
    }

    @Test
    void default_execution() throws Exception {
        addParam('NEW_VERSION', 'anything')

        runJenkinsfileAndAssertSuccess()

        assertBuildCall('./update-quarkus-kogito-runtimes', [
            NEW_VERSION: 'anything'
        ], false)

        assertBuildCall('./update-quarkus-kogito-examples', [
            NEW_VERSION: 'anything'
        ], false)

        assertBuildCall('./update-quarkus-optaplanner', [
            NEW_VERSION: 'anything'
        ], false)

        assertBuildCall('./update-quarkus-optaplanner-quickstarts', [
            NEW_VERSION: 'anything'
        ], false)
    }

    @Test
    void no_version_param() throws Exception {
        thrown.expect(PowerAssertionError)

        runJenkinsfileAndAssertFailure()
    }

}
