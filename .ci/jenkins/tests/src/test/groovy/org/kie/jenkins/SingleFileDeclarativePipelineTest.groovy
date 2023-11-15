/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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