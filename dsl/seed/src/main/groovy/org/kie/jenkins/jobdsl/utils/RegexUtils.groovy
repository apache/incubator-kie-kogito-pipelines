
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

import org.kie.jenkins.jobdsl.Utils

class RegexUtils {

    static String getRegexFirstLetterCase(String id) {
        return id.length() > 1 ? "${getRegexLetterStringCase(id.substring(0, 1))}${id.substring(1).toLowerCase()}" : getRegexLetterStringCase(id)
    }

    static String getRegexLetterStringCase(String str) {
        return "[${str.toUpperCase()}|${str.toLowerCase()}]"
    }

    static String getRegexMultipleCase(String str) {
        return "(${str.toUpperCase()}|${str.toLowerCase()}|${Utils.firstLetterUpperCase(str.toLowerCase())})"
    }
}
