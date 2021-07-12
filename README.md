# Kogito Pipelines

This repository contains some of the pipelines of Kogito project.

* [Kogito Pipelines](#kogito-pipelines)
* [Kogito Repositories](#kogito-repositories)
* [The different Kogito pipelines](#the-different-kogito-pipelines)
  * [Nightly & Release pipelines](#nightly--release-pipelines)
  * [Tools pipelines](#tools-pipelines)
  * [Repositories' specific pipelines](#repositories-specific-pipelines)
    * [Kogito Runtimes' other jobs](#kogito-runtimes-other-jobs)
    * [PR checks](#pr-checks)
      * [Jenkins checks](#jenkins-checks)
      * [GitHub Action checks](#github-action-checks)
    * [Sonar cloud](#sonar-cloud)
* [Configuration of pipelines](#configuration-of-pipelines)
  * [Jenkins](#jenkins)
    * [Jenkins jobs generation](#jenkins-jobs-generation)
  * [Zulip notifications](#zulip-notifications)
    * [Format](#format)

# Kogito Repositories

Apart from this repository, pipelines are also concerning those repositories:

* [kogito-runtimes](https://github.com/kiegroup/kogito-runtimes)
* [optaplanner](https://github.com/kiegroup/optaplanner)
* [kogito-apps](https://github.com/kiegroup/kogito-apps)
* [kogito-examples](https://github.com/kiegroup/kogito-examples)
* [kogito-images](https://github.com/kiegroup/kogito-images)
* [kogito-operator](https://github.com/kiegroup/kogito-operator)

# The different Kogito pipelines

## Nightly & Release pipelines

Kogito has 2 main pipelines:

* [Nightly pipeline](./.ci/jenkins/Jenkinsfile.nightly)  
  is a multibranch pipeline which is run daily on each active branch
* [Release pipeline](./.ci/jenkins/Jenkinsfile.release)  
  is a on-demand single pipeline job

More information can be found [here](./docs/nightly_and_release.md).

## Tools pipelines

This is a set of cleanup utils jobs.

## Repositories' specific pipelines

### Kogito Runtimes' other jobs

Those jobs can be found into the `kogito-runtimes` repository.  
They are daily run jobs:

* [kogito-native](https://github.com/kiegroup/kogito-runtimes/blob/main/.ci/jenkins/Jenkinsfile.native)  
  Perform a daily native build&test of runtimes.
* [kogito-quarkus](https://github.com/kiegroup/kogito-runtimes/blob/main/.ci/jenkins/Jenkinsfile.quarkus)  
  Perform a daily check of runtimes against latest snapshot of Quarkus.
* [kogito-drools-snapshot](https://github.com/kiegroup/kogito-runtimes/blob/main/.ci/jenkins/Jenkinsfile.drools)  
  Perform a daily check of runtimes against latest snapshot of Drools.

### PR checks

For each PR there are two different CI builds, one using internal Jenkins executor and another one that leverage GitHub actions.

#### Jenkins checks

Each repository has a `Jenkinsfile` for the PR check.

The jobs are located into the `pullrequest` folder in Jenkins.  
Only the Operator PR check is not yet in this folder as it is still on another Jenkins

#### GitHub Action checks

Each repository has a `ci-pr.yaml` file in `.github/workflows` folder to configure the workflow.
The `kogito-runtimes` repo has a different configuration, see additional notes at the end of this paragraph.

Repo dependencies and build commands are configured in the centralized [`.ci` folder](https://github.com/kiegroup/kogito-pipelines/tree/main/.ci) in this repo.

Build execution is performed using the [`github-action-chain`](https://github.com/kiegroup/github-action-build-chain) action that automatically performs cross repo builds.

After the build, test results are parsed and logged using the [`action-surefire-report`](https://github.com/ScaCap/action-surefire-report) action (actually we are using a forked version until this [PR #56](https://github.com/ScaCap/action-surefire-report/pull/56) will be accepted).

Additional notes about `kogito-runtimes` build: this repo requires a full downstream build of all the repositories so to minimize disk usage and execution time the repo has been configured to use multiple jobs in parallel, one for each repo.
This means that instead of a single `ci-pr.yaml` file, there are four of them: `runtimes-pr.yaml`, `optaplanner-pr.yaml`, `apps-pr.yaml` and `examples-pr.yaml`. For each of them all upstream repos are just compiled without test execution.

### Sonar cloud

NOTE: test coverage analysis is executed only by Jenkins PR build and not while using GitHub action

# Configuration of pipelines

## Jenkins

All pipelines can be found in [kogito Jenkins folder](https://eng-jenkins-csb-business-automation.apps.ocp4.prod.psi.redhat.com/job/KIE/job/kogito).

### Jenkins jobs generation

More information can be found [here](./docs/jenkins.md).

## Zulip notifications

Any message / error is sent to [kogito-ci](https://kie.zulipchat.com/#narrow/stream/236603-kogito-ci) stream.

### Format

    [branch] Project
