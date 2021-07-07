# Jenkins Jobs

* [Jenkins Jobs](#jenkins-jobs)
  * [Folder Structure](#folder-structure)
  * [Generated jobs structure](#generated-jobs-structure)
  * [Seed Job](#seed-job)
    * [Main configuration](#main-configuration)
      * [Branch configuration](#branch-configuration)
    * [Seed job testing](#seed-job-testing)
  * [Jobs configuration in a repository](#jobs-configuration-in-a-repository)
  * [Test specific jobs](#test-specific-jobs)
    * [Generate only specific repositories](#generate-only-specific-repositories)
    * [Generate all](#generate-all)

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

## Jobs configuration in a repository

As shown in `.jenkins` folder, groovy scripts to generate jobs should be in `.jenkins/dsl/jobs` or `.ci/jenkins/dsl/jobs` directory.  
Then, you can also add a small `.jenkins/dsl/test.sh` or `.ci/jenkins/dsl/test.sh` to test your groovy script:

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

The script clones the `kogito-pipelines` repository and then call the `seed_test.sh` which should copy the jobs and run the tests.

Then you can call the script:

```bash
$ chmod u+x .jenkins/dsl/test.sh
$ cd .jenkins/dsl && ./scripts/test.sh
```

*NOTE: Your Jenkinsfiles can be stored anywhere in the repository. The reference is anyway done in the job script, so you can put whatever you want if needed. One good practise would be to store it at the root of the project or in the `.jenkins` folder.*

## Test specific jobs

To generate the jobs you need for testing, you will need to create a seed job with the configuration that you can find in the [seed job definition](../dsl/seed/jobs/seed_job.groovy).  

**WARNINGS**  
* **You should never used the production seed job for testing as you may overwrite some production configuration by doing so ...**

* **If you plan to test nightly and/or release pipelines, you need to have a special branch on a fork with your own [seed configuration](../dsl/seed/config.yaml), because you will need specific credentials, maven repository, container registry namespace, so that you are not altering the production artifacts/images. An example of a branch with a special configuration can be found here: https://github.com/radtriste/kogito-pipelines/tree/test-setup**

### Generate only specific repositories

Once that job is created, just execute the seed job with the correct `CUSTOM` parameters.

For example, if you are working with `kogito-images` and `kogito-operator` pipelines on branch `kogito-998756`:

* CUSTOM_BRANCH_KEY=kogito-998756
* CUSTOM_REPOSITORIES=kogito-images:kogito-998756,kogito-operator:kogito-998756
* CUSTOM_AUTHOR=<YOUR_GITHUB_AUTHOR>
* CUSTOM_MAIN_BRANCH=kogito-998756  
  => This will allow to generate pull requests, else it considers the main branch to be the one defined into the seed config (most of the time `master`) and does not generate those.

By default, in `CUSTOM_REPOSITORIES`, if you don't define any branch, `master` is taken.

*NOTE: If you are testing nightly/release pipelines, you will need to set the correct `SEED_AUTHOR` and `SEED_BRANCH` because you will need specific credentials for your test. Else you can use directly the `kiegroup/master` repository.*

### Generate all

Just run the created seed job with no `CUSTOM_XXX` parameters. It will generate all jobs.

This is useful if you want to test the full nightly and/or release pipeline(s).

**Again, please make sure in that case that you setup your own configuration !**
