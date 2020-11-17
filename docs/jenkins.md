# Jenkins Jobs

* [Jenkins Jobs](#jenkins-jobs)
  * [Folder Structure](#folder-structure)
  * [Generated jobs structure](#generated-jobs-structure)
  * [Seed Job](#seed-job)
    * [Main configuration](#main-configuration)
      * [Branch configuration](#branch-configuration)
    * [Seed job testing](#seed-job-testing)
    * [Generate jobs for testing](#generate-jobs-for-testing)
  * [Jobs configuration in a repository](#jobs-configuration-in-a-repository)

## Folder Structure

    .
    ├── .jenkins                # repository jenkins folder
    │   ├── dsl                 # dsl script files to generate jobs for the repository
    │   │   ├── jobs            # contains the jobs for the current branch to be generated
    │   │   └── scripts         # scripts for dsl jobs
    │   └── tests               # tests for Jenkinsfiles
    ├── dsl                     # main folder for seed job generation and configuration of Kogito jobs
    │   ├── seed                # contains all information and configuration of the seed job
    │   │   └── src/main/*      # common classes which can be reused into the different groovy scripts for jobs generation
    │   │   └── src/test/*      # test classes to check groovy script are well formed
    │   │   └── config.yaml     # main configuration for the whole seed job
    │   └── branch_config.yaml  # jobs configuration for the current branch

## Generated jobs structure

    .
    ├── 0-seed-job                   # main seed job to create other jobs. once executed, it will update it self
    ├── nightly                      # all related nightly jobs
    │   ├── master                   # master nightly jobs
    │   └── ...{RELEASE BRANCHES}    # nightly jobs related to specific release branches
    ├── pullrequest                  # all related pr jobs
    │   ├── ...
    │   └── kogito-runtimes.bdd      # PR jobs for running BDD tests from kogito-runtimes repository
    ├── release                      # all related release jobs
    │   ├── create-release-branches  # create release branches for specific repositories
    │   ├── prepare-release          # Prepare all repo/jenkins jobs with a new release branch
    │   └── ...{RELEASE BRANCHES}    # release jobs related to specific release branches
    └── tools                        # tools jobs

## Seed Job

The seed job is represented by its [Jenkinsfile](../seed/../dsl/seed/Jenkinsfile.seed) and its [job script](../dsl/seed/jobs/seed_job.groovy).

To use that job, you will need to create a job in Jenkins using configuration detailed in the [job script](../dsl/seed/jobs/seed_job.groovy).

**IMPORTANT: Please adapt the `author`, the `credentials` and the `branch` to your needs. You should not be generating jobs from `kiegroup/master` !**

### Main configuration

The main configuration for the seed job can be found in the [config.yaml](../dsl/seed/config.yaml) file.  
This file contains all the default configuration for the job generation, which will be made available as environment variables
but also the git branches which will be used for the jobs generation.

Most probably, you will need to change it with your data.

#### Branch configuration

This configuration contains the configuration for a specific branch.

By default, there is a list of dependent git repositories where job scripts will be taken for jobs generation.  
For each of them, you can override the global Git information (author name, credentials ...).

### Seed job testing

```bash
$ cd dsl/seed && ./gradlew test
```

### Generate jobs for testing

1. Fork this repository
2. Create your branch
3. Update the [main configuration](../dsl/seed/config.yaml) with your data
4. Create the Jenkins job

## Jobs configuration in a repository

As shown in `.jenkins` folder, groovy scripts to generate jobs should be in `.jenkins/dsl/jobs` directory.  
Then, you can also add a small `.jenkins/dsl/test.sh` to test your groovy script:

```bash
#!/bin/bash -e

TEMP_DIR=`mktemp -d`

branch=$1
author=$2

if [ -z $branch ]; then
branch='master'
fi

if [ -z $author ]; then
author='kiegroup'
fi

echo "----- Cloning pipelines repo from ${author} on branch ${branch}"
git clone --single-branch --branch ${branch} https://github.com/${author}/kogito-pipelines.git $TEMP_DIR

echo '----- Launching seed tests'
${TEMP_DIR}/dsl/seed/scripts/seed_test.sh ${TEMP_DIR}
```

The script clones the `kogito-pipelines` repository and then call the `seed_test.sh` which should copy the jobs and 

Then you can call the script:

```bash
$ chmod u+x .jenkins/dsl/test.sh
$ cd .jenkins/dsl && ./scripts/test.sh
```

*NOTE: Your Jenkinsfiles should be in `.jenkins` folder but can be anywhere. The reference is anyway done in the job script, so you can put whatever you want if needed.*