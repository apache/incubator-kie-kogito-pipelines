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

## Jenkins jobs generation structure

We identify 3 different parts for the job generation:

* Seed API  
  This is the seed repository containing the common API to generate jobs
* Config files  
  Yaml files which setup the configuration for the jobs generation (repositories, branches, ...)
* Repositories' jobs definition
  This is where each repository decide which job(s) to generate

### Seed library

The seed structure is as follow:

    .
    ├── dsl                     # main folder for seed job generation and configuration of Kogito jobs
    │   ├── seed                # contains all information and configuration of the seed job
    |   │   └── jenkinsfiles
    |   │   │   ├── scripts                         # common script for seed generation
    |   |   │   │   ├── utils.groovy                 # common script used by seed pipelines
    |   |   │   │   └── ...
    |   │   │   ├── Jenkinsfile.seed.branch         # pipeline for the branch seed job
    |   │   │   ├── Jenkinsfile.seed.main           # pipeline for the main seed job
    |   │   └── jobs
    |   │   │   ├── seed_job_branch.groovy          # generate a branch seed job
    |   │   │   ├── seed_job_main.groovy            # generate a main seed job
    |   │   │   └── seed_job_repo.groovy            # generate a repo seed job
    │   │   ├── src             
    |   │   │   ├── main/*      # common classes which can be reused into the different groovy scripts for jobs generation
    |   │   │   └── test/*      # test classes to check groovy script are well formed

The entry point is the main seed job `0-seed-job`. It configuration can be found in the [main dsl groovy file](../dsl/seed/jobs/seed_job_main.groovy).

This job will read the main config file (see below) and will generate a folder for each defined branch. In each of those folders, it will generate a `0-seed-job` based on the [branch dsl groovy file](../dsl/seed/jobs/seed_job_branch.groovy) which will generate the repositories jobs based on the branch config file (see below). It also generates trigger jobs which will listen to modifications on specific paths.

Next to that it will generate root jobs defined into the [root jobs dsl groovy file](../dsl/seed/jobs/root_jobs.groovy).

### Seed config files

Config files can be defined anywhere. The seed jobs will know how to retrieve from:

- The parameters for the main 0-seed-job
- The main config file for the branch 0-seed-job

#### Seed main config file

The seed main config file will be given to the `0-seed-job` as a parameter.

```yaml
# Ecosystem contains all different projects concerned by this configuration
# It is used by the seed jobs and the prepare release job
ecosystem: 
  main_project: kogito
  projects:
  - name: drools
    regexs:
    - drools.*
  - name: kogito
    regexs:
    - kogito.*

# This part describes all branches which need generation
git:
  branches:
  # For each branch, you can also define specific `seed_config_file` information
  # which will override the default defined below
  - name: improve_dsl_generation_test
  - name: main
    main_branch: true

# This can be overriden for each defined git branches
# This gives the branch config information (see below for branch configuration)
seed:
  config_file: # main branch seed config file
    git:
      repository: kogito-pipelines
      author:
        name: radtriste
        credentials_id: radtriste
      branch: branch_config_file # Used only for main branch checkout. Else it should be defined for each branch at git.branches level. By default, this value will be overriden by the branch name.
    path: dsl/config/branch.yaml

# For any notification
jenkins:
  email_creds_id: KOGITO_CI_EMAIL_TO_PERSO
```

#### Seed branch config file

The branch config file is given to the seed job via the configuration into the main config file (see above).

All values from this config file will be available to job generation as env variables.

```yaml
# Allow to disable job types
# This is useful in testing to avoid to generate all jobs
# Current jobtype can be found in ../dsl/seed/src/main/groovy/org/kie/jenkins/jobdsl/model/JobType.groovy
# job_types:
  # init-branch:
  #   disabled: true
  # nightly:
  #   disabled: true
  # other:
  #   disabled: true
  # pullrequest:
  #   disabled: true
  # release:
  #   disabled: true
  # tools:
  #   disabled: true
# Define the different environments
environment:
  quarkus:
    main:
      enabled: true
    branch:
      enabled: true
      version: '2.9'
    lts:
      enabled: true
      version: '2.7'
  native:
    enabled: true
  mandrel:
    enabled: true
    builder_image: quay.io/quarkus/ubi-quarkus-mandrel:21.3-java11
  mandrel_lts:
    enabled: true
  runtimes_bdd:
    enabled: true

productized_branch: true

# Used to force the disabling of triggers
# Useful when a branch is no more maintained but you still want to keep job history
disable:
  triggers: true

repositories:
  - name: kogito-pipelines
    branch: any_branch
  - name: drools
    branch: any_branch

# Main Git configuration
# This information can be overriden in any of the repositories above
git:
  author:
    name: radtriste
    credentials_id: radtriste
    token_credentials_id: radtriste-gh-token
  bot_author:
    name: radtriste-bot-account
    credentials_id: radtriste-bot
  jenkins_config_path: .ci/jenkins

# Full repository Example
# repositories:
#   - name: NAME
#     branch: branch_name
#     disabled: false
#     author:
#       name: another_gh_author
#       credentials_id: another_gh_author_creds
#       token_credentials_id: another_gh_author_creds_token
#     bot_author:
#       name: another_gh_bot_author
#       credentials_id: another_gh_bot_author_creds
#     jenkins_config_path: .ci/jenkins/

maven:
  settings_file_id: 2bd418aa-56fa-4403-9232-8c77a50fc528
  nexus:
    release_url: https://repository.stage.jboss.org/nexus
    release_repository: jboss-releases-repository
    staging_profile_url: https://repository.stage.jboss.org/nexus/content/groups/kogito-public/
    staging_profile_id: 2161b7b8da0080
    build_promotion_profile_id: 1966c60aff37d
  artifacts_repository: http://nexus3-tradisso-nexus.apps.kogito-cloud.hosted.psi.rdu2.redhat.com/repository/kogito-test/
  #artifacts_repository: ''
  pr_checks:
    repository:
      url: https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/content/repositories/kogito-runtimes-pr-full-testing/
      creds_id: unpacks-zip-on-qa-nexus
cloud:
  image:
    registry_credentials_nightly: tradisso_registry_token
    registry_credentials_release: tradisso_registry_token
    registry: quay.io
    namespace: tradisso
    latest_git_branch: main
jenkins:
  email_creds_id: KOGITO_CI_EMAIL_TO_PERSO

```

### Repository jobs definition

Each repository which want its job to be generated should be registered into the branch config file (see above) and should contain this folder structure at its root folder:

    .
    ├── .ci/jenkins                
    │   ├── dsl
    │   │   ├── jobs.groovy      # contains the jobs for the current branch to be generated
    │   └── tests                # (optional) tests for Jenkinsfiles

*NOTE: The main folder, aka `.ci/jenkins` can be changed but must be specified into the branch config file*

### Generated jobs structure

Here is an example of the generated job hierarchy:

    .
    ├── 0-seed-job                   # main seed job to create other branch & repo jobs.
    ├── 0-prepare-release-branch     # prepare new release branch
    ├── main                         # all main jobs
        ├── 0-seed-job               # branch seed job to create other branch & repo jobs.
    │   ├── nightly                  # all related nightly jobs
    │   ├── other                    # some other jobs
    │   ├── pullrequest              # all related pr jobs
    │   └── tools                    # tools jobs
    ├── {RELEASE BRANCH}             # all release branch specific jobs
    │   ├── nightly                  # all related nightly jobs
    │   ├── other                    # some other jobs
    │   ├── pullrequest              # all related pr jobs
    │   ├── release                  # release jobs
    │   └── tools                    # tools jobs
    └── z-seed-trigger-job           # Trigger job which listen to change on kgoito-pipelines

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

file=$(mktemp)
# For more usage of the script, use ./test.sh -h
curl -o ${file} https://raw.githubusercontent.com/kiegroup/kogito-pipelines/main/dsl/seed/scripts/seed_test.sh
chmod u+x ${file}
${file} $@
```

The script clones the `kogito-pipelines` repository and then call the `seed_test.sh` which should copy the jobs and run the tests.

Then you can call the script:

```bash
$ chmod u+x .ci/jenkins/dsl/test.sh
$ .ci/jenkins/dsl/test.sh .ci/jenkins/dsl
```

### Test on Jenkins

The best way to test your changes is to copy the `/KIE/kogito/0-seed-job` into your custom folder, create a test branch and generate jobs from this test branch. You can then in that config file setup the repositories you want to generate to avoid useless generation.

If you don't have access to the Kogito `0-seed-job`, you can also create one based its [dsl job groovy file](../dsl/seed/jobs/seed_job_main.groovy).

**WARNINGS**

* **You should never used the production seed job for testing as you may overwrite some production configuration by doing so ...**

* **You need to have a special branch on a fork with your own [branch seed configuration](../dsl/config/branch.yaml), because you will need specific Jenkins credentials (Git, Registry), Maven repository (see annex), Container registry namespace, so that you are not altering the production artifacts/images. An example of a branch with a special configuration can be found here: https://github.com/radtriste/kogito-pipelines/tree/test-config**

#### Test steps

1) Setup your [main config file](../dsl/config/main.yaml) to have only the testing branch.
2) Setup your [branch config file](../dsl/config/branch.yaml) with test repositories and credentials/author.
3) Copy the create the `0-seed-job` and point the parameters of the job to the main config file you created.  
   Depending on what you changed and need to test, you may have different parameters to update:
   * Setup `SEED_CONFIG_FILE_*` parameters correctly to the author/branch you pushed your test modifications. This is always required.
   * Change the `SEED_*` parameters only if you did some changes in the Kogito Pipelines `dsl/seed` folder. Else you can use the default, aka `kiegroup/main`.
4) Launch the `0-seed-job` and it should create all the needed jobs

**Again, please make sure that you setup your own configuration !**

#### Create specific Maven repository for testing

For deploying runtimes and examples artifacts, and to avoid any conflict with main repository on snapshot artifacts, you will need to provide a nexus repository to deploy the artifacts.

If don't have one already, you can create one with the [nexus-operator](https://github.com/m88i/nexus-operator).

**IMPORTANT:** We don't support yet specific user's credentials. Anonymous user needs to have push rights to it.
