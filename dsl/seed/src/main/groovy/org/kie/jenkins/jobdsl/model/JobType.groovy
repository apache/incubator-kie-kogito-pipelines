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

package org.kie.jenkins.jobdsl.model

import org.kie.jenkins.jobdsl.Utils

/*
* JobType corresponds to a type of job.
*
* So a job type can be disabled and the closture `isActiveClosure` should be initialized else it returns `true` by default.
*/
class JobType {

    public static final JobType NIGHTLY = new JobType(
        name: 'nightly',
        environmentDependent: true
    )
    public static final JobType OTHER = new JobType(
        name: 'other'
    )

    public static final JobType PULL_REQUEST = new JobType(
        name: 'pullrequest',
        environmentDependent: true
    )
    public static final JobType RELEASE = new JobType(
        name: 'release',
        isActiveClosure: { script -> !Utils.isMainBranch(script) },
        defaultEnv: [ RELEASE: 'true' ],
    )
    public static final JobType SETUP_BRANCH = new JobType(
        name: 'setup-branch',
    )
    public static final JobType TOOLS = new JobType(
        name: 'tools'
    )

    String name
    boolean environmentDependent
    Closure isActiveClosure
    Map defaultEnv = [:]

    String getName() {
        return this.name
    }

    boolean isEnvironmentDependent() {
        return environmentDependent
    }

    boolean isActive(def script) {
        return this.isActiveByConfig(script) && (this.isActiveClosure ? this.isActiveClosure(script) : true)
    }

    Map getDefaultEnvVars() {
        return this.defaultEnv
    }

    private boolean isActiveByConfig(def script) {
        return !Utils.isJobTypeDisabled(script, this.name)
    }
}
