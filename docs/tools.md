# Tools scripts

## Update quarkus version

There is a Jenkins job which can be found on Jenkins, which will update the Quarkus version on all Kogito/OptaPlanner projects which need it.  
You can find the `update-quarkus-all` job in `kogito/[branch]/tools` Jenkins folder.

Alternatively, there is an `update-quarkus-{project}` job for each project if you need to update the quarkus version for a specific job.  
*`update-quarkus-all` job is in fact just an interfact to call all the different `update-quarkus-{project}` jobs* at once.

The called job(s) will create a new PR, which you is mentioned into the logs.  
It also sends it to the Zulip [kogito-ci](https://kie.zulipchat.com/#narrow/stream/236603-kogito-ci) stream.

### Run the script locally

#### Update quarkus version on an existing repository

You can call the `../tools/update-project-dependencies.sh` script with the required options:

```bash
bash ./update-project-dependencies.sh -v '2.7.0.Final' -m 'kogito-dependencies-bom' -p 'version.io.quarkus' -c 'kogito-quarkus-bom'
```

#### Update quarkus version from scratch

The same script will need to be called via the `../tools/run-git-script-modification.sh` script:

```bash
bash ./run-git-script-modification.sh -p kogito-runtimes -f evacchi -b main -- ./update-project-dependencies.sh -v '2.7.0.Final' -m 'kogito-dependencies-bom' -p 'version.io.quarkus' -c 'kogito-quarkus-bom'
```

This will checkout the `evacchi/kogito-runtimes` repository on the main branch, execute the `./update-project-dependencies.sh -q '2.7.0.Final' -m 'kogito-dependencies-bom' -p 'version.io.quarkus' -c 'kogito-quarkus-bom'` command in that folder and create a PR from there.

## Update quarkus platform

You will need to run the `../tools/update-quarkus-platform.sh` locally.

### Update quarkus platform on an existing repository

You can call the `../tools/update-project-dependencies.sh` script with the required options:

```bash
bash ./update-quarkus-platform.sh -v 15.0.Final stage
```

### Update quarkus platform from scratch

The same script will need to be called via the `../tools/run-git-script-modification.sh` script:

```bash
bash ./run-git-script-modification.sh -f evacchi -b main -- ./update-quarkus-platform.sh -v 15.0.Final stage
```

This will checkout the `evacchi/quarkus-platform` repository on the main branch, execute the `./update-quarkus-platform.sh -v 15.0.Final stage` command in that folder and create a PR from there.
