# Nightly & Release pipelines

* [Nightly & Release pipelines](#nightly--release-pipelines)
  * [Nightly pipeline](#nightly-pipeline)
    * [Nightly pipeline Architecture](#nightly-pipeline-architecture)
    * [Activate/Deactivate release branch](#activatedeactivate-release-branch)
    * [Nightly pipeline Parameters](#nightly-pipeline-parameters)
    * [Nightly pipeline Troubleshooting](#nightly-pipeline-troubleshooting)
      * [Nightly pipeline is failing](#nightly-pipeline-is-failing)
      * [Nightly pipeline is unstable](#nightly-pipeline-is-unstable)
      * [Nightly Build & Deploy job is failing](#nightly-build--deploy-job-is-failing)
      * [Nightly Promote job is failing](#nightly-promote-job-is-failing)
    * [Testing the Nightly Pipeline](#testing-the-nightly-pipeline)
      * [Change pipeline envs for nightly testing](#change-pipeline-envs-for-nightly-testing)
      * [Setup Jenkins job(s) for nightly testing](#setup-jenkins-jobs-for-nightly-testing)
      * [Setup Jenkins creds for nightly testing](#setup-jenkins-creds-for-nightly-testing)
      * [Launch release with minimal parameters for nightly testing](#launch-release-with-minimal-parameters-for-nightly-testing)
  * [Release pipeline](#release-pipeline)
    * [Architecture of the Release pipeline](#architecture-of-the-release-pipeline)
    * [Release pipeline parameters](#release-pipeline-parameters)
      * [Minimal parameters for the release](#minimal-parameters-for-the-release)
      * [Start release pipeline with specific parameters](#start-release-pipeline-with-specific-parameters)
    * [Release pipeline Manual interventions](#release-pipeline-manual-interventions)
      * [Default manual interventions](#default-manual-interventions)
      * [Retry/Skip/Abort manual intervention](#retryskipabort-manual-intervention)
    * [Release pipeline Troubleshooting](#release-pipeline-troubleshooting)
      * [Release pipeline is failing](#release-pipeline-is-failing)
      * [Release pipeline is reporting a called *-deploy job is failing](#release-pipeline-is-reporting-a-called--deploy-job-is-failing)
      * [Release pipeline is reporting a called *-promote job is failing](#release-pipeline-is-reporting-a-called--promote-job-is-failing)
    * [Testing the Release Pipeline](#testing-the-release-pipeline)
      * [Change pipeline envs for release testing](#change-pipeline-envs-for-release-testing)
      * [Setup Jenkins job(s) for release testing](#setup-jenkins-jobs-for-release-testing)
      * [Setup Jenkins creds for release testing](#setup-jenkins-creds-for-release-testing)
      * [Use specific nexus for released artifacts](#use-specific-nexus-for-released-artifacts)
      * [Launch release with minimal parameters for testing](#launch-release-with-minimal-parameters-for-testing)
  * [Explanation on architecture of those pipelines](#explanation-on-architecture-of-those-pipelines)
    * [Background & Objectives](#background--objectives)
    * [Problems / Solutions](#problems--solutions)
      * [Problems](#problems)
      * [Solution](#solution)
    * [Nightly pipeline Proposal](#nightly-pipeline-proposal)
    * [Release pipeline Proposal](#release-pipeline-proposal)

In order to perform, Nightly and Release pipelines need to call deploy and promote jobs for runtimes, images and operator.  
Those jobs should be present at the same level as the nightly and/or release job, so they can be found when called.

Here is the list of jobs and link to Jenkinsfiles:

* [kogito-runtimes-deploy](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.deploy)
* [kogito-runtimes-promote](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.promote)
* [kogito-images-deploy](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.deploy)
* [kogito-images-promote](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.promote)
* [kogito-operator-deploy](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.deploy)
* [kogito-operator-promote](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.promote)

## Nightly pipeline

### Nightly pipeline Architecture

The nightly pipeline is composed of many steps, calling different other jobs to perform the build&test of runtimes/images/operator as well as the deployment of jar artifacts and nightly container images.

**NOTE:** The nightly pipeline is a multibranch pipeline job and runs on `master` and each active release branch (for example 0.15.x).

![Flow](./images/nightly-flow.png)

Steps could be separated into 2 parts:

* **Build & Deploy**  
  Composed of calls to `*-deploy` jobs, is reponsible to runtimes, images and operator are ok.  
  Note that if any problem happens in a `deploy` job, then other deploy jobs are still run (they will take the previous stable artifacts/images) to run.
* **Promote**  
  Composed of calls to `*-promote` jobs, is responsible to deploy snapshots artifacts/nightly images to repositories/registries.

Note that for a particular part of the project (runtimes, images or operator), if the `Build & Deploy` failed, then the `Promote` part is skipped.

### Activate/Deactivate release branch

Once a branch is created or another one should be deactivated, you just need to go the configuration of the [nightly job](https://rhba-jenkins.rhev-ci-vms.eng.rdu2.redhat.com/job/KIE/job/kogito/job/kogito-nightly/configure) and change the `include` or `exclude` configuration of Git.

### Nightly pipeline Parameters

If needed, the Nightly pipeline can be restarted with `SKIP_TESTS` or `SKIP` specific part options.  
See [nightly Jenkinsfile](./../Jenkinsfile.nightly) for more information on parameters.

### Nightly pipeline Troubleshooting

Here are some information in case the Nightly pipeline fails ...

In the Zulip kogito-ci stream, there should be a link to the failing job. Open it and then open the `Blue Ocean` view. This will be easier to detect what is failing.

#### Nightly pipeline is failing

In case the main pipeline is failing, this will be most likely a Groovy error.  
This can happen when changes have been made to the [nightly Jenkinsfile](./../Jenkinsfile.nightly).

The problem will need to be corrected on the corresponding branch and the pipeline could be restarted.

#### Nightly pipeline is unstable

This usually means that one or many of the called jobs is/are failing. List of unsuccessful jobs should be displayed into Zulip and can be reviewed into the Blue Ocean.

By clicking the failing stage and accessing the failing job, you should be able to review the problem.

#### Nightly Build & Deploy job is failing

In that case, identify the error (Groovy script error or test problem), correct it and relaunch the nightly pipeline if necessary.

Here are some problems which can occur on a `Build & Deploy` job:

* Groovy script error (Jenkinsfile.deploy has some problems)
* Compilation
* Test errors
* Deployment problem (credentials)

#### Nightly Promote job is failing

In that case, identify the error (Groovy script error or test problem), correct it and rebuild the failing job.

Here are some problems which can occur on a `Promote` job:

* Groovy script error (Jenkinsfile.deploy has some problems)
* Deployment problem (credentials)

### Testing the Nightly Pipeline

In order to test the full nightly pipeline, and in order to avoid any problem, you will need to change some env in [Jenkinsfile.nightly](./Jenkinsfile.nightly), create jobs in Jenkins and setup some credentials.

* Have a specific container registry and credentials registered with `push` rights on it.
* For deploying runtimes artifacts, and to avoid any conflict with main repository on snapshot artifacts, you will need to provide a nexus repository to deploy the artifacts (see [nexus-operator](https://github.com/m88i/nexus-operator)).

#### Change pipeline envs for nightly testing

* `IMAGE_NAMESPACE` (and `IMAGE_REGISTRY_CREDENTIALS`, see [Setup Jenkins creds](#setup-jenkins-creds-for-nightly-testing))

#### Setup Jenkins job(s) for nightly testing

You will need to create single pipeline jobs and let them run once to update the `parameters` part (you should stop them quickly as it makes no sense to let them run until the end. Just wait for the checkout of repo and the `node` command done).

**NOTE:** You will need to access the correct branch for each !

* [kogito-runtimes-deploy](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.deploy)
* [kogito-runtimes-promote](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.promote)
* [kogito-images-deploy](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.deploy)
* [kogito-images-promote](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.promote)
* [kogito-operator-deploy](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.deploy)
* [kogito-operator-promote](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.promote)

**NOTE:** Deploy & Promote jobs of a specific repository can be ignored (and so not created for testing), but you will need to check the corresponding `SKIP_` parameter.

#### Setup Jenkins creds for nightly testing

In Jenkins, you should set those credentials and set the correct values in env:

* `IMAGE_REGISTRY_CREDENTIALS` (username/password credential)  
  Credential to push image to the container registry (should have rights to `IMAGE_NAMESPACE`)
* `KOGITO_CI_EMAIL_TO` (secret text credential)  
  Email for notifications. You can set your email for example

#### Launch release with minimal parameters for nightly testing

* `PROJECT_VERSION`
* `ARTIFACTS_REPOSITORY` to set to the correct repository
* (optional) `SKIP_TESTS` (usually you will want that)
* (optional) `SKIP_*` to skip different phases

## Release pipeline

### Architecture of the Release pipeline

The release pipeline is composed of many steps, calling different other jobs to set the correct version, perform the build&test of runtimes/images/operator and then promote released artifacts and container images as production ready.

![Flow](./images/release-flow.png)

Like the nightly pipeline, steps could be separated into 2 parts: `Build & Deploy` and `Promote`.

The release pipeline is also adding extra functionalities to this:

* **[Create release branches](../Jenkinsfile.create-release-branches)**  
  Based on the given version, a new release branch (with `Major.minor.x`) is created if not already existing.
* **Create PRs**  
  In the `Build & Deploy` phase, the different `Build & Deploy` jobs will create PR(s) with version changes on the release branch of the different repositories.
* **Merge PRs**  
  In the `Promote` phase, PRs create are merged by the different `Promote` jobs.
* **Set next snapshot**  
  For some of the repositories, it will also PR create&merge to update to the next "snapshot" on the release branch.

### Release pipeline parameters

#### Minimal parameters for the release

In order to start, here are the minimal parameters to the release pipeline:

* **PROJECT_VERSION**  
  Corresponds to the Kogito version to be set during the release.
* **DEPLOY_AS_LATEST**
  Should be set to true if we want the container images to be tagged as `latest`.

#### Start release pipeline with specific parameters

The Release pipeline can be tweaked with some other parameters if needed.

One option is the possibility to skip some stages, depending on which part you want to release.  
**NOTE: If you decide to skip the runtimes part, please be careful on `ARTIFACTS_REPOSITORY`, `EXAMPLES_URI` and `EXAMPLES_REF` parameters**

See [release Jenkinsfile](./../Jenkinsfile.release) for the full list on parameters.

### Release pipeline Manual interventions

#### Default manual interventions

One other specificity of the release pipeline are the manual interventions.  
They are currently 3 of them:

* **Get staging repository** (happens in kogito-runtimes-deploy)  
  When asked, Staging repository can be retrived from [JBoss Nexus repository](https://repository.jboss.org/nexus/). For that, the user needs to have specific rights or ask someone who has those rights.  
  Once retrieved, you can put the url to staging repository into the input asking for it on Jenkins.
* **Promote staging repository to Maven Central** (happens in kogito-runtimes-deploy)  
  Same as getting staging repository, you need the rights to promote the artifacts from [JBoss Nexus repository](https://repository.jboss.org/nexus/) to Maven Central, or ask someone with rights to do it.  
  Once promoted, just confirm it is promoted on Jenkins.
* **Check kogito-apps artifacts are set to correct url** (happens in kogito-images-promote)
  Jenkins will ask you to review the logs of this job and check urls are correct in the different displayed `module.yaml` files.  
  Review them and confirm if everything is ok.

#### Retry/Skip/Abort manual intervention

For each called jobs, in case of failure, there is retry/skip/abort manual intervention.

* **Retry**  
  will completely relaunch the job, taking latest changes on the branch (test or groovy correction).
* **Skip**  
  if you think the test failed because of random error but it is ok (can happen for BDD tests where trying again will pass the test, due to instability of Openshift ...).
* **Abort**  
  if any other problem.  
  As long as `promote` phase did not start, there will be no impact on the release.

### Release pipeline Troubleshooting

Here are some information in case the Nightly pipeline fails ...

In the Zulip kogito-ci stream, there should be a link to the failing job. Open it and then open the `Blue Ocean` view. This will be easier to detect what is failing.

#### Release pipeline is failing

In case the main pipeline is failing, this will be most likely a Groovy error.  
This can happen when changes have been made to the release [Jenkinsfile](./../Jenkinsfile.release).

The problem will need to be corrected on `master` and the pipeline could be restarted.

#### Release pipeline is reporting a called *-deploy job is failing

In that case, identify the error (Groovy script error or test problem), correct it and relaunch the nightly pipeline if necessary.

If the problem is a test error, check the errors and then decide to retry or skip the called job (see [retry possibility](#retryskipabort-manual-intervention)).

If the problem in `Build & Deploy` job is one of those:

* Groovy script error (Jenkinsfile.deploy has some problems)
* Compilation error
* Create PR or deployment problem (credentials)

You can correct it, and then just retry the job (see [retry possibility](#retryskipabort-manual-intervention))

#### Release pipeline is reporting a called *-promote job is failing

In that case, identify the error (Groovy script error or test problem), correct it and rebuild the failing job.

Here are some problems which can occur on a `Promote` job:

* Groovy script error (Jenkinsfile.deploy has some problems)
* Merge PR (on set release version PR) or Deployment problem (credentials)

You can correct it, and then just retry the job (see [retry possibility](#retryskipabort-manual-intervention))

**WARNING:**  
**If the problem occurs during a `set next snapshot` stage, then you should not retry the job. If that happens, that means that "release" steps have been done. So just skip the job and set the next snapshot manually in the repository on the corresponding release branch**

### Testing the Release Pipeline

In order to test the full release pipeline, and in order to avoid any problem, you will need to change some env in [Jenkinsfile.release](./Jenkinsfile.release), create jobs in Jenkins and setup some credentials.

* Have a specific container registry and credentials registered with `push` rights on it.
* Have a specific author repository that you can test against
* If you don't want to flood your main repository, you should use a "bot account", referred as `BOT_*`
* For deploying runtimes artifacts, and to avoid any conflict by creating a staging repository inadvertly, you will need to provide a nexus repository to deploy the artifacts (see [nexus-operator](https://github.com/m88i/nexus-operator)).

#### Change pipeline envs for release testing

* `GIT_AUTHOR` (and `GIT_AUTHOR_CREDS_ID`, see [Setup Jenkins creds](#setup-jenkins-creds-for-release-testing))
* `BOT_AUTHOR` (and `BOT_AUTHOR_CREDS_ID`, see [Setup Jenkins creds](#setup-jenkins-creds-for-release-testing))
* `IMAGE_NAMESPACE` (and `IMAGE_REGISTRY_CREDENTIALS`, see [Setup Jenkins creds](#setup-jenkins-creds-for-release-testing))

#### Setup Jenkins job(s) for release testing

You will need to create single pipeline jobs and let them run once to update the `parameters` part (you should stop them quickly as it makes no sense to let them run until the end. Just wait for the checkout of repo and the `node` command done).

**NOTE:** You will need to access the correct branch for each !

* [kogito-release](./Jenkinsfile.release)
* [create-release-branches](./Jenkinsfile.create-release-branches)
* [kogito-runtimes-deploy](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.deploy)
* [kogito-runtimes-promote](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.promote)
* [kogito-images-deploy](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.deploy)
* [kogito-images-promote](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.promote)
* [kogito-operator-deploy](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.deploy)
* [kogito-operator-promote](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.promote)

**NOTE:** Deploy & Promote jobs of a specific repository can be ignored (and so not created for testing), but you will need to check the corresponding `SKIP_` parameter.

#### Setup Jenkins creds for release testing

In Jenkins, you should set those credentials and set the correct values in env:

* `GIT_AUTHOR_CREDS_ID` (username/password credential)  
  Username / [GitHub token](https://github.com/settings/tokens) which has rights on `GIT_AUTHOR`
* `BOT_AUTHOR_CREDS_ID` (username/password credential)  
  Username / [GitHub token](https://github.com/settings/tokens) which has rights on `BOT_AUTHOR`
* `GITHUB_TOKEN_CREDS_ID` (secret text credential)  
  [GitHub token](https://github.com/settings/tokens) for Github CLI
* `IMAGE_REGISTRY_CREDENTIALS` (username/password credential)  
  Credential to push image to the container registry (should have rights to `IMAGE_NAMESPACE`)
* `KOGITO_CI_EMAIL_TO` (secret text credential)  
  Email for notifications. You can set your email for example

#### Use specific nexus for released artifacts

In case staging and release repositories are the same for testing, you can uncomment the line `addStringParam(buildParams, 'MAVEN_ARTIFACT_REPOSITORY', env.STAGING_REPOSITORY)` in `Promote images` stage. This is made to make the called job aware of the release repository to update correctly the artifacts.

#### Launch release with minimal parameters for testing

* `PROJECT_VERSION`
* `ARTIFACTS_REPOSITORY` to set to the correct repository
* (optional) `SKIP_TESTS` (usually you will want that)
* (optional) `SKIP_*` to skip different phases

## Explanation on architecture of those pipelines

### Background & Objectives

The Kogito project is composed of 3 parts:

* Runtimes (kogito-runtimes, kogito-apps, kogito-examples)

  * Jar artifacts
  * Deployed to Maven repository

* Images (kogito-images)

  * Cekit (docker) build
  * Deployed to Quay

* Operator (kogito-cloud-operator)

  * Go / OperatorSDK
  * Deployed to Quay

The objectives are:

* Unify deployment process from runtimes to operator
* Avoid human interaction
* Reuse processes

### Problems / Solutions

#### Problems

* Need runtimes’ artifacts to test images & operator
* Different technologies (Java, Cekit/Docker, Go)
* High-level tests are done in BDD tests (operator)

#### Solution

* Main pipeline to control the flow
* Separate build&tests and promote
* Promote is done only when all tests in all repositories are ok
* Each “repo” keeps its own deployment/promote process

### Nightly pipeline Proposal

The [nightly pipeline](./Jenkinsfile.nightly) for Kogito is responsible to build&test runtimes artifacts, images and operator.  
For that, it will call different jobs for deployment and then for promote if all tests passed.  

![Nightly pipeline](./images/pipeline-nightly.png)

If the pipeline is failing or is unstable, then a notification is sent to Zulip.

### Release pipeline Proposal

The [release pipeline](./Jenkinsfile.release) aims to enhance the nightly pipeline by providing added features like `set version`, `create/merge PRs`, `git tag`...

![Release pipeline](./images/pipeline-release.png)

Some steps are still manual (like jar artifacts promotion to Maven Central) and notifications are sent to Zulip for manual intervention.
