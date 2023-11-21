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

class JenkinsFolderRegistry {

    static final Map<String, JenkinsFolder> FOLDER_REGISTRY = [:]

    private JenkinsFolderRegistry() {
    }

    static boolean hasFolder(String folderName) {
        return FOLDER_REGISTRY.containsKey(folderName)
    }

    static void register(JenkinsFolder folder) {
        if (hasFolder(folder.getName())) {
            throw new RuntimeException("Trying to register a new folder with name '${folder.getName()}' but this one is already existing in registry...")
        }
        if (folder.environmentName && !folder.jobType.isEnvironmentDependent()) {
            throw new RuntimeException("Trying to register a new folder of type ${folder.jobType.getName()} for the environment ${folder.environmentName} but this job type is not environment dependent...")
        }
        FOLDER_REGISTRY.put(folder.getName(), folder)
    }

    static JenkinsFolder getOrRegisterFolder(def script, JobType jobType, String envName = '') {
        // Create folder struct
        JenkinsFolder folder = new JenkinsFolder(jobType, EnvUtils.getEnvironmentEnvVars(script, envName), envName)

        if (hasFolder(folder.getName())) {
            return getFolder(folder.getName())
        } else {
            register(folder)
            return folder
        }
    }

    static JenkinsFolder getFolder(String folderName) {
        return hasFolder(folderName) ? FOLDER_REGISTRY.get(folderName) : null
    }

    static getPullRequestFolder(def script, String envName = '') {
        return getOrRegisterFolder(script, JobType.PULL_REQUEST, envName)
    }

    static getNightlyFolder(def script, String envName = '') {
        return getOrRegisterFolder(script, JobType.NIGHTLY, envName)
    }

}
