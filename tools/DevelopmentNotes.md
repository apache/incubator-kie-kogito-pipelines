<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
  -->

## zip-sources-all.sh

Clones specified repositories, adds some files for root directory, and zips them as a source code distribution.

To test `zip-sources-all.sh` locally,

- Edit `./tools/zip-sources-all.sh` to uncomment and set variables

for example:
```
TARGET_VERSION="10.1.0"
SOURCES_DEFAULT_BRANCH="main"
GIT_AUTHOR="apache"

SOURCES_REPOSITORIES="incubator-kie-drools
incubator-kie-kogito-runtimes
incubator-kie-kogito-apps
incubator-kie-optaplanner
incubator-kie-tools"
```

- Execute `./tools/zip-sources-all.sh source.zip`
