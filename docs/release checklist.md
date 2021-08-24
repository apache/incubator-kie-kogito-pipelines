# Release Checklist

## Cut-off

- Prepare release branch
- Retrieve PR created from the job
- Once PR is merged, execute `0-seed-job` (automation not yet fully available ...)

Jobs should then be created for the branch and nightly are automatically activated.

## Release day

- Start release pipeline
- Once all tests passed and artifacts are staged, create the Quarkus Platform PR (../tools/update-quarkus-platform.sh)
- Once platform PR is green, artifacts can be released (release pipeline job can continue)
- Release pipeline is done
  - Ask for OperatorHub PRs (on Zulip)
  - Notify for Docs team (on Zulip)

## All done

- Release can be announced
  Please check also with Kogito Tooling team
