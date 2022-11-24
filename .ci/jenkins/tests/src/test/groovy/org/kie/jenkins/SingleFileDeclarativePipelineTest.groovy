package org.kie.jenkins

import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource
import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.assertj.core.api.Assertions.assertThat

abstract class SingleFileDeclarativePipelineTest extends KogitoDeclarativePipelineTest {

    String jenkinsfilePath

    SingleFileDeclarativePipelineTest(String jenkinsfilePath) {
        super()
        this.jenkinsfilePath = jenkinsfilePath
    }

    void runJenkinsfileAndAssertSuccess(boolean printStack = true) {
        runJenkinsfile(printStack)
        assertJobStatusSuccess()
    }

    void runJenkinsfileAndAssertUnstable(boolean printStack = true) {
        runJenkinsfile(printStack)
        assertJobStatusUnstable()
    }

    void runJenkinsfileAndAssertFailure(boolean printStack = true) {
        runJenkinsfile(printStack)
        assertJobStatusFailure()
    }

    void runJenkinsfile(boolean printStack = false) {
        runScript(this.jenkinsfilePath)
        if (printStack) {
            printCallStack()
        }
    }

}