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

import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils

/**
* Seed Job utils
* 
* Common methods to create seed jobs
**/
class SeedJobUtils {

    static def createSeedJobTrigger(def jenkinsScript, String jobName, String repository, String gitAuthor, String gitAuthorCredsId, String gitBranch, List pathsToListen, String jobRelativePathToTrigger) {
        if (pathsToListen.isEmpty()) {
            throw new RuntimeException('pathsToListen cannot be empty, else it would end up in an infinite loop ...');
        }
        def job = jenkinsScript.pipelineJob(jobName) {
            description('This job listens to pipelines repo and launch the seed job if needed. DO NOT USE FOR TESTING !!!! See https://github.com/apache/incubator-kie-kogito-pipelines/blob/main/docs/jenkins.md#test-specific-jobs')

            logRotator {
                numToKeep(5)
            }

            throttleConcurrentBuilds {
                maxTotal(1)
            }

            parameters {
                booleanParam('FORCE_REBUILD', false, 'Default, the job will scan for modified files and do the update in case some files are modified. In case you want to force the DSL generation')
            }

            environmentVariables {
                env('REPO_NAME', repository)
                env('GIT_AUTHOR_NAME', gitAuthor)
                env('GIT_AUTHOR_CREDS_ID', gitAuthorCredsId)
                env('GIT_BRANCH_NAME', gitBranch)

                env('JOB_RELATIVE_PATH_TO_TRIGGER', jobRelativePathToTrigger)
                env('LISTEN_TO_MODIFIED_PATHS', new groovy.json.JsonBuilder(pathsToListen).toString())

                env('AGENT_LABEL', 'ubuntu')
            }

            definition {
                cps {
                    script(jenkinsScript.readFileFromWorkspace('jenkinsfiles/Jenkinsfile.seed.trigger'))
                    sandbox()
                }
            }

            properties {
                githubProjectUrl("https://github.com/${gitAuthor}/${repository}")

                if (Utils.isProdEnvironment(jenkinsScript)) {
                    pipelineTriggers {
                        triggers {
                            githubPush()
                        }
                    }
                }
            }
        }
        // Trigger jobs need to be executed once for the hook to work ...
        if (Utils.isProdEnvironment(jenkinsScript)) {
            jenkinsScript.queue(jobName)
        }
        return job
    }
}
