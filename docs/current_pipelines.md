# Drools 8 / Kogito / OptaPlanner 8 pipelines

Here is a small overview of current pipelines existing around Kogito project.

## Which projects ?

- Drools
  - https://github.com/kiegroup/drools
  - https://github.com/kiegroup/drools-website
  - https://github.com/kiegroup/kie-benchmarks
  - https://github.com/kiegroup/kie-pmml-integration
- Kogito
  - https://github.com/kiegroup/kogito-pipelines
  - https://github.com/kiegroup/kogito-runtimes
  - https://github.com/kiegroup/kogito-apps
  - https://github.com/kiegroup/kogito-examples
  - https://github.com/kiegroup/kogito-images
  - https://github.com/kiegroup/kogito-operator
  - https://github.com/kiegroup/kogito-docs
  - https://github.com/kiegroup/kogito-serverless-operator
  - https://github.com/kiegroup/kie-docs
  - https://github.com/kiegroup/kie-tools
- OptaPlanner
  - https://github.com/kiegroup/optaplanner
  - https://github.com/kiegroup/optaplanner-quickstarts
  - https://github.com/kiegroup/optaplanner-website

## Jenkins

## GitHub Actions

## Jenkins 

### Seed generation

TODO

### Jenkins jobs

### Job environments

Environments can be found in the branch seed configuration.

Usually you find those:

- default
  or the "empty" environment
- native
  Execute build with native maven commands
- quarkus-branch
  Check the project against the latest updates from the current quarkus branch used
- quarkus-main
  Check the project against the latest updates from quarkus main branch (forward compatiblity)
- quarkus-3
  Migrate repositories to Quarkus 3
- quarkus-lts
  Check the project against the latest updates from quarkus LTS branch (backward compatiblity)
- native-lts
  Check the project against the latest updates from quarkus LTS branch (backward compatiblity) in native mode

#### PRs

TODO
#### Nightlies

**Main nightly**

TODO

**Environment nightlies**

TODO

#### Setup branch

TODO

#### Release

TODO

#### Tools

TODO

### Quarkus Platform nightly

As we need to deploy a specific Quarkus Platform with our snapshot artifacts, there is a nightly existing, which is launched by the main nightly.

### Quarkus 3

The Quarkus 3 flow is a bit specific as we are preparing for releasing versions in parallel of Quarkus 2.x version.

#### Automatic Quarkus 3 branch creation

TODO

#### Quarkus 3 dsl configurations

TODO

#### Quarkus 3 specific jobs

TODO

## GitHub Actions

### PR checks

We make use of 

1) Migrate repository

## How-tos ?

### Update Quarkus version

1) Create an issue on [kie-issues](https://github.com/kiegroup/kie-issues)
2) Launch `main/tools/update-quarkus-all` job with 
   * [Drools](https://eng-jenkins-csb-business-automation.apps.ocp-c1.prod.psi.redhat.com/job/KIE/job/drools/job/main/job/tools/job/update-quarkus-all/)
   * [Kogito](https://eng-jenkins-csb-business-automation.apps.ocp-c1.prod.psi.redhat.com/job/KIE/job/kogito/job/main/job/tools/job/update-quarkus-all/)
   * [OptaPlanner](https://eng-jenkins-csb-business-automation.apps.ocp-c1.prod.psi.redhat.com/job/KIE/job/optaplanner/job/main/job/tools/job/update-quarkus-all/)
3) Wait for the PRs to arrive on [kogito-ci](https://kie.zulipchat.com/#narrow/stream/236603-kogito-ci) and [optaplanner-ci](https://kie.zulipchat.com/#narrow/stream/354864-optaplanner-ci) streams
4) Review the changes.
   **Make sure there is no downgrade of dependency version !!!!**