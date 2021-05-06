# Execute in `optaplanner` or `kogito-runtimes`

# set the git remote for the PR
# e.g. I set evacchi for https://github.com/evacchi/kogito-runtimes
REMOTE=evacchi
BRANCH=master
KIE_VERSION=7.53.0.Final
PR_BRANCH=bump-$KIE_VERSION
 
git checkout $BRANCH
 
# create branch named like version
git checkout -b $PR_BRANCH
 
# process versions
mvn -pl :kogito-build-parent \
versions:set-property \
-Dproperty=version.org.kie7 \
-DnewVersion=$KIE_VERSION \
-DgenerateBackupPoms=false
 
# commit all
git commit -am "[$BRANCH] Bump KIE $KIE_VERSION"
 
# push the branch to a remote
git push -u $REMOTE $PR_BRANCH
 
# Open a PR to kogito-runtimes using the commit as a title
# e.g. see https://github.com/kiegroup/kogito-runtimes/pull/1200
gh pr create --fill --base $BRANCH -R kiegroup/kogito-runtimes
