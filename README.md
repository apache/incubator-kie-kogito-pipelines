# kogito-pipelines

CI/CD pipelines for Kogito

## Nightly & Release pipelines

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

### Nightly pipeline

The [nightly pipeline](./Jenkinsfile.nightly) for Kogito is responsible to build&test runtimes artifacts, images and operator.  
For that, it will call different jobs for deployment and then for promote if all tests passed.  

![Nightly pipeline](./docs/images/pipeline-nightly.png)

If the pipeline is failing or is unstable, then a notification is sent to Zulip.

### Release pipeline

The [release pipeline](./Jenkinsfile.release) aims to enhance the nightly pipeline by providing added features like `set version`, `create/merge PRs`, `git tag`...

![Release pipeline](./docs/images/pipeline-release.png)

Some steps are still manual (like jar artifacts promotion to Maven Central) and notifications are sent to Zulip for manual intervention.

### Job dependencies

In order to perform, Nightly and Release pipelines need to call deploy and promote jobs for runtimes, images and operator.  
Those jobs should be present at the same level as the nightly and/or release job, so they can be found when called.  

Here is the list of jobs and link to Jenkinsfiles:

* [create-release-branches](create-release-branches.Jenkinsfile) (only called by the release pipeline)
* [kogito-runtimes-deploy](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.deploy)
* [kogito-runtimes-promote](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.promote)
* [kogito-images-deploy](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.deploy)
* [kogito-images-promote](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.promote)
* [kogito-operator-deploy](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.deploy)
* [kogito-operator-promote](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.promote)

## Test release pipeline

In order to test the full release pipeline, and in order to avoid any problem, you will need to change some env in [Jenkinsfile.release](./Jenkinsfile.release), create jobs in Jenkins and setup some credentials.

* Have a specific author repository that you can test against
* If you don't want to flood your main repository, you should use a "bot account", referred as `BOT_*`

### Change pipeline envs

* **GIT_AUTHOR** (and `GIT_AUTHOR_CREDS_ID`, see [Setup Jenkins creds](#setup-jenkins-creds))
* **BOT_AUTHOR** (and `BOT_AUTHOR_CREDS_ID`, see [Setup Jenkins creds](#setup-jenkins-creds))
* **IMAGE_NAMESPACE** (and `IMAGE_REGISTRY_CREDENTIALS`, see [Setup Jenkins creds](#setup-jenkins-creds))

### Setup Jenkins job

You will need to create single pipeline jobs and let them run once to update the `parameters` part (you should stop them quickly as it makes no sense to let them run until the end. Just wait for the checkout of repo and the `node` command done).

**NOTE:** You will need to access the correct branch for each !

* [kogito-release](./Jenkinsfile.release)
* [create-release-branches](./Jenkinsfile.create-release-branches)
* [kogito-runtimes-deploy](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.deploy)
* [kogito-runtimes-promote](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.promote)
* [[kogito-images-deploy](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.deploy)
* [kogito-images-promote](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.promote)
* [kogito-operator-deploy](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.deploy)
* [kogito-operator-promote](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.promote)

**NOTE:** Deploy & Promote jobs of a specific repository can be ignored (and so not created for testing), but you will need to check the corresponding `SKIP_` parameter.

### Setup Jenkins creds

In Jenkins, you should set those credentials and set the correct values in env

* GIT_AUTHOR_CREDS_ID (username/password credential)  
  Username / [GitHub token](https://github.com/settings/tokens) which has rights on `GIT_AUTHOR`
* BOT_AUTHOR_CREDS_ID (username/password credential)  
  Username / [GitHub token](https://github.com/settings/tokens) which has rights on `BOT_AUTHOR`
* GITHUB_TOKEN_CREDS_ID (secret text credential)  
  [GitHub token](https://github.com/settings/tokens) for Github CLI
* IMAGE_REGISTRY_CREDENTIALS (username/password credential)  
  Credential to push image to the container registry (should have rights to `IMAGE_NAMESPACE`)
* KOGITO_CI_EMAIL_TO (secret text credential)  
  Email for notifications. You can set your email for example.