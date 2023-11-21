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

package org.kie.jenkins.jobdsl.utils

/*
* *DEPRECATED*
* This class is deprecated and will be removed in the future, once all maitained branches do not need it anymore !
*/
@Deprecated
class FolderUtils {

    static String NIGHTLY_FOLDER = 'nightly'
    static String RELEASE_FOLDER = 'release'
    static String TOOLS_FOLDER = 'tools'
    static String PULLREQUEST_FOLDER = 'pullrequest'
    static String OTHER_FOLDER = 'other'
    static String PULLREQUEST_RUNTIMES_BDD_FOLDER = "${PULLREQUEST_FOLDER}/kogito-runtimes.bdd"

    static List getAllNeededFolders() {
        return [
            NIGHTLY_FOLDER,
            RELEASE_FOLDER,
            TOOLS_FOLDER,
            PULLREQUEST_FOLDER,
            PULLREQUEST_RUNTIMES_BDD_FOLDER,
            OTHER_FOLDER,
        ]
    }

    static String getNightlyFolder(def script) {
        return NIGHTLY_FOLDER
    }

    static String getReleaseFolder(def script) {
        return RELEASE_FOLDER
    }

    static String getToolsFolder(def script) {
        return TOOLS_FOLDER
    }

    static String getPullRequestFolder(def script) {
        return PULLREQUEST_FOLDER
    }

    static String getPullRequestRuntimesBDDFolder(def script) {
        return PULLREQUEST_RUNTIMES_BDD_FOLDER
    }

    static String getOtherFolder(def script) {
        return OTHER_FOLDER
    }

}