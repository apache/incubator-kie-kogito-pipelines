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

import groovy.json.JsonOutput

String getDslSeedFolderAbsolutePath(String seedRepoPath = '') {
    String path = "${WORKSPACE}"
    if (seedRepoPath) {
        path += "/${seedRepoPath}"
    }
    return "${path}/dsl/seed"
}

boolean isDebug() {
    return params.DEBUG ?: false
}

def deepCopyObject(def originalMap) {
    return readJSON(text: JsonOutput.toJson(originalMap))
}

boolean getMainBranch(Map mainBranches, String repository) {
    return mainBranches.get(mainBranches.containsKey(repository) ? repository : 'default')
}

def readSeedConfigFile(config) {
    def seedConfig = [:]
    assert config.filepath : "Read seed config: missing Git Filepath. Please check the configuration: ${config}"

    dir(checkoutSeedConfigFile(config)) {
        seedConfig = readYaml(file: "${config.filepath}")
        if (isDebug()) {
            println '[DEBUG] Seed config:'
            println "[DEBUG] ${seedConfig}"
        }
    }
    return seedConfig
}

String checkoutSeedConfigFile(config) {
    assert config.repository : "Read seed config: missing Git Repository. Please check the configuration: ${config}"
    assert config.author : "Read seed config: missing Git Author. Please check the configuration: ${config}"
    assert config.credentials : "Read seed config: missing Git Credentials. Please check the configuration: ${config}"
    assert config.branch : "Read seed config: missing Git Branch. Please check the configuration: ${config}"

    String dirName = "seed-${config.repository}-${config.author}-${config.branch}"
    dir(dirName) {
        deleteDir()
        checkout(githubscm.resolveRepository(config.repository, config.author, config.branch, false, config.credentials))
    }
    return dirName
}

return this
