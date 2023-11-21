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

import org.kie.jenkins.jobdsl.model.JobType
import org.kie.jenkins.jobdsl.utils.EnvUtils

class JenkinsFolder {

    String name
    JobType jobType
    String environmentName
    Map defaultEnv

    JenkinsFolder(JobType jobType, Map defaultEnv, String environmentName) {
        this.jobType = jobType
        this.defaultEnv = defaultEnv
        this.environmentName = environmentName
    }

    JenkinsFolder(JobType jobType, Map defaultEnv) {
        this(jobType, defaultEnv, '')
    }

    JenkinsFolder(JobType jobType) {
        this(jobType, [:], '')
    }

    String getName() {
        String name = this.jobType.getName()
        name += this.environmentName ? ".${this.environmentName}" : ''
        return name
    }

    String getEnvironmentName() {
        return this.environmentName
    }

    Map getDefaultEnvVars() {
        Map env = [
            JOB_TYPE: this.jobType.getName(),
            JOB_ENVIRONMENT: this.environmentName
        ]
        env.putAll(this.jobType.getDefaultEnvVars() ?: [:])
        env.putAll(this.defaultEnv ?: [:])
        return new HashMap(env)
    }

    String getDefaultEnvVarValue(String key) {
        return getDefaultEnvVars().find { defaultKey, defaultValue -> "${defaultKey}" == "${key}" }?.value
    }

    // A JenkinsFolder is active if jobType AND environment are active
    boolean isActive(def script) {
        return this.jobType.isActive(script) &&
            (this.environmentName ? EnvUtils.isEnvironmentEnabled(script, this.environmentName) : true)
    }

}
