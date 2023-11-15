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

package org.kie.jenkins.jobdsl.support

import org.yaml.snakeyaml.Yaml

import groovy.io.FileType
import java.util.Properties

class TestUtil {

    static List<File> getJobFiles() {
        List<File> files = []
        new File('jobs').eachFileRecurse(FileType.FILES) {
            if (it.name.endsWith('.groovy')) {
                files << it
            }
        }
        files
    }

    /**
     * Write a single XML file, creating any nested dirs.
     */
    static void writeFile(File dir, String name, String xml) {
        List tokens = name.split('/')
        File folderDir = tokens[0..<-1].inject(dir) { File tokenDir, String token ->
            new File(tokenDir, token)
        }
        folderDir.mkdirs()

        File xmlFile = new File(folderDir, "${tokens[-1]}.xml")
        xmlFile.text = xml
    }

    static Map readBranchConfig() {
        Map config = new Yaml().load(('./branch_config.yaml' as File).text)

        Map props = [:]
        fillEnvProperties(props, '', config)
        props.put('ENVIRONMENTS', config.environments ? config.environments.keySet().join(',') : '')
        return props
    }

    static void fillEnvProperties(Map envProperties, String envKeyPrefix, Map propsMap) {
        propsMap.each { key, value ->
            String newKey = generateEnvKey(envKeyPrefix, key)
            if (value instanceof Map) {
                fillEnvProperties(envProperties, newKey, value as Map)
            } else if (value instanceof List) {
                envProperties[newKey] = (value as List).join(',')
            } else {
                envProperties[newKey] = value
            }
        }
    }

    static String generateEnvKey(String envKeyPrefix, String key) {
        return (envKeyPrefix ? "${envKeyPrefix}_${key}" : key).toUpperCase()
    }

}
