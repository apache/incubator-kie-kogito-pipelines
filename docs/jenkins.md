# Jenkins Jobs

* [Jenkins Jobs](#jenkins-jobs)
  * [Structure](#structure)
    * [Seed structure](#seed-structure)
    * [Jobs Structure](#jobs-structure)
      * [Seed file entry point](#seed-file-entry-point)
    * [Generated jobs structure](#generated-jobs-structure)
  * [Testing](#testing)
    * [Local testing](#local-testing)
      * [Seed job local testing](#seed-job-local-testing)
      * [Repository jobs local testing](#repository-jobs-local-testing)
    * [Test on Jenkins](#test-on-jenkins)
      * [Test specific repository jobs](#test-specific-repository-jobs)
      * [Test the whole Kogito jobs](#test-the-whole-kogito-jobs)
      * [Generate only specific repositories](#generate-only-specific-repositories)
      * [Generate all](#generate-all)
  * [Annex](#annex)
    * [Create specific Maven repository for testing](#create-specific-maven-repository-for-testing)

We use [Jenkins Job DSL](https://github.com/jenkinsci/job-dsl-plugin) to create the different Kogito jobs

## Structure

### Seed structure

The seed structure is as follow:

    .
    ├── dsl                     # main folder for seed job generation and configuration of Kogito jobs
    │   ├── seed                # contains all information and configuration of the seed job
    |   │   └── config
    |   │   │   ├── main.yaml       # main configuration for the whole seed job
    |   │   │   └── branch.yaml     # jobs configuration for the current branch
    |   │   └── jobs
    |   │   │   ├── scripts                         # common script for seed generation
    |   |   │   │   ├── seed_repo_generation.groovy # common script used by the repo seed job
    |   |   │   │   └── ...
    |   │   │   ├── Jenkinsfile.seed.branch         # pipeline for the branch seed job
    |   │   │   ├── Jenkinsfile.seed.main           # pipeline for the main seed job
    |   │   │   ├── seed_job_branch.groovy          # generate a branch seed job
    |   │   │   ├── seed_job_main.groovy            # generate a main seed job
    |   │   │   └── seed_job_repo.groovy            # generate a repo seed job
    │   │   ├── src             
    |   │   │   ├── main/*      # common classes which can be reused into the different groovy scripts for jobs generation
    |   │   │   └── test/*      # test classes to check groovy script are well formed

The entry point is the main seed job `0-seed-repo`.  
This job will read the [main config](../dsl/seed/config/main.yaml) and generate a branch seed job `z-seed-{branch}-job` for each branch defined in the config file.

Each branch seed job will then read the [branch config](../dsl/seed/config/branch.yaml) and generate a repo seed job `z-seed-{branch}-{repo}-job` for each repository defined there.

Each repo seed job will get the [branch config](../dsl/seed/config/branch.yaml) and generate the job stored in the repository's dsl path (see after).

### Jobs Structure

Each repository which want its job to be generated should be registered into the [seed job branch config](../dsl/seed/config/branch.yaml) and should contain this folder structure in it root:

    .
    ├── .ci/jenkins                
    │   ├── dsl
    │   │   ├── Jenkinsfile.seed # Main entry point for generation of the jobs
    │   │   ├── jobs.groovy      # contains the jobs for the current branch to be generated
    │   └── tests                # (optional) tests for Jenkinsfiles

#### Seed file entry point

`Jenkinsfile.seed` should be defined as follow:

```groovy
@Library('jenkins-pipeline-shared-libraries')_

seed_generation = null
node('master') {
    dir("${SEED_REPO}") {
        checkout(githubscm.resolveRepository("${SEED_REPO}", "${SEED_AUTHOR}", "${SEED_BRANCH}", false))
        seed_generation = load "${SEED_SCRIPTS_FILEPATH}"
    }
}
seed_generation.generate()
```

It is just an entry point for the job generation. The setup for job generation is then done in the `seed_generation.generate()` method.  
The user does just need to create then the `.ci/jenkins/dsl/jobs.groovy` file to generate the needed jobs for the repository.  
See an example [here](../.ci/jenkins/dsl/jobs.groovy)

Using a proper `Jenkinsfile.seed` stored directly into the repository folders will allow to set a hook on that repository to be able to refresh the jobs easily when an update is done.

*NOTE: Your pipelines' Jenkinsfiles can be stored anywhere in the repository. The reference is anyway done in the job script, so you can put whatever you want if needed. One good practise would be to store it in the `.ci/jenkins` folder.*

### Generated jobs structure

Here is an example of the generated job hierarchy:

    .
    ├── 0-seed-job                   # main seed job to create other branch & repo jobs.
    ├── nightly                      # all related nightly jobs
    │   ├── main                     # main nightly jobs
    │   └── ...{RELEASE BRANCHES}    # nightly jobs related to specific release branches
    ├── other                        # some other jobs
    ├── pullrequest                  # all related pr jobs
    │   ├── main                     # main pr jobs
    │   ├── {RELEASE_BRANCH}         # release branch pr jobs
    │   └── kogito-runtimes.bdd      # PR jobs for running BDD tests from kogito-runtimes repository
    ├── release                      # all related release jobs
    │   ├── prepare-release          # Prepare all repo/jenkins jobs with a new release branch
    │   └── ...{RELEASE BRANCHES}    # release jobs related to specific release branches
    │── tools                        # tools jobs
    └── z-seed-*                     # branch & repo seed jobs used to generate the other jobs

## Testing

### Local testing

Local testing will make sure your code is correctly handled by the framework.

#### Seed job local testing

```bash
$ cd dsl/seed && ./gradlew test
```

#### Repository jobs local testing

As shown in `.ci/jenkins` folder, groovy scripts to generate jobs should be in `.ci/jenkins/dsl/jobs` directory.  
Then, you can also add a small `.ci/jenkins/dsl/test.sh` to test your groovy script:

```bash
#!/bin/bash -e

TEMP_DIR=`mktemp -d`

branch=$1
author=$2

if [ -z $branch ]; then
branch='main'
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
$ chmod u+x .ci/jenkins/dsl/test.sh
$ cd .ci/jenkins/dsl && ./test.sh
```

### Test on Jenkins

#### Test specific repository jobs

You can use the description in [seed_job_repo.groovy](../dsl/seed/jobs/seed_job_repo.groovy) and create the repo seed job directly by setting then the Seed author/branch (take the `kiegroup/main` by default if you don't have any) and then set correctly those environement variables:

* **REPO_NAME**  
  This is the repository you want to test
* **GIT_BRANCH**  
  Branch you want to test on your repository
* **GIT_AUTHOR**  
  Usually your Git author name
* **GENERATION_BRANCH**  
  The "key" for job generation. You can use the same value as `GIT_BRANCH`
* **GIT_MAIN_BRANCH**
  This is to define what is the main branch when processing jobs.  
  If the jobs you want to test is branch agnostic, then you can just set `main` or anything else.  
  If the job you want to test is only for the main branch, then you should set the same value as `GIT_BRANCH`.
* **GIT_JENKINS_CONFIG_PATH**  
  This is where to find the jenkins seed file for job generation. Default is `.ci/jenkins`

#### Test the whole Kogito jobs

To generate the jobs you need for testing, you will need to create the main seed job with the configuration that you can find in the [seed job definition](../dsl/seed/jobs/seed_job_main.groovy).  

**WARNINGS**

* **You should never used the production seed job for testing as you may overwrite some production configuration by doing so ...**

* **If you plan to test nightly and/or release pipelines, you need to have a special branch on a fork with your own [branch seed configuration](../dsl/seed/config/branch.yaml), because you will need specific Jenkins credentials (Git, Registry), Maven repository (see annex), Container registry namespace, so that you are not altering the production artifacts/images. An example of a branch with a special configuration can be found here: https://github.com/radtriste/kogito-pipelines/tree/kogito-7110**

#### Generate only specific repositories

Once that main seed job is created, just execute it with the correct `CUSTOM` parameters.

For example, if you are working with `kogito-images` and `kogito-operator` pipelines on branch `kogito-998756`:

* CUSTOM_BRANCH_KEY=kogito-998756
* CUSTOM_REPOSITORIES=kogito-images:kogito-998756,kogito-operator:kogito-998756
* CUSTOM_AUTHOR=<YOUR_GITHUB_AUTHOR>
* CUSTOM_MAIN_BRANCH=kogito-998756  
  => This will allow to generate pull requests, else it considers the main branch to be the one defined into the seed config (most of the time `main`) and does not generate those.

By default, in `CUSTOM_REPOSITORIES`, if you don't define any branch, `main` is taken.

*NOTE: If you are testing nightly/release pipelines, you will need to set the correct `SEED_AUTHOR` and `SEED_BRANCH` because you will need specific credentials for your test. Else you can use directly the `kiegroup/main` repository.*

#### Generate all

Setup your [main seed configuration](../dsl/seed/config/main.yaml) to have only the testing branch.

The, just run the created seed job with no `CUSTOM_XXX` parameters but correct `SEED_*` parameters. It will generate all jobs.

This is useful if you want to test the full nightly and/or release pipeline(s).

**Again, please make sure in that case that you setup your own configuration !**

## Annex

### Create specific Maven repository for testing

For deploying runtimes and examples artifacts, and to avoid any conflict with main repository on snapshot artifacts, you will need to provide a nexus repository to deploy the artifacts.

If don't have one already, you can create one with the [nexus-operator](https://github.com/m88i/nexus-operator).

**IMPORTANT:** We don't support yet specific user's credentials. Anonymous user needs to have push rights to it.
