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

* [kogito-runtimes-deploy](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.deploy.new)
* [kogito-runtimes-promote](https://github.com/kiegroup/kogito-runtimes/blob/master/Jenkinsfile.promote)
* [kogito-images-deploy](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.deploy)
* [kogito-images-promote](https://github.com/kiegroup/kogito-images/blob/master/Jenkinsfile.promote)
* [kogito-operator-deploy](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.deploy)
* [kogito-operator-promote](https://github.com/kiegroup/kogito-cloud-operator/blob/master/Jenkinsfile.promote)
