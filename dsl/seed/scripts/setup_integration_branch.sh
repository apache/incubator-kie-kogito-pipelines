#!/bin/bash
set -euo pipefail

integration_branch="${INTEGRATION_BRANCH:-}"

if [ -z "${integration_branch}" ]; then
    echo 'No `INTEGRATION_BRANCH` defined. Nothing to be done here ...'
    exit 0
fi

commit_message="${COMMIT_MESSAGE:-"Update Integration Branch ${integration_branch}"}"
git_remote="${GIT_REMOTE:-origin}"

echo "Setup integration branch ${integration_branch}"

set -x

echo $(git ls-remote ${git_remote} ${integration_branch} | wc -l)

git fetch "${git_remote}"

if git rev-parse ${integration_branch}; then
    git branch -D ${integration_branch}
fi
git checkout -b ${integration_branch}

if [ "$(git status --porcelain)" != '' ]; then
    git status
    git diff
    git add -u
    git commit -m "${commit_message}"
else
    echo 'No changes to commit'
fi

git config user.email ${GITHUB_USER}@jenkins.redhat
git config user.name ${GITHUB_USER}

# set +x
git config --local credential.helper "!f() { echo username=\\$GITHUB_USER; echo password=\\$GITHUB_TOKEN; }; f"
# set -x

if (( $(git ls-remote ${git_remote} ${integration_branch} | wc -l) > 0 )); then
    git push -d ${git_remote} ${integration_branch}
fi

git push ${git_remote} ${integration_branch}