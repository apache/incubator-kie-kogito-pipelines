#### if the CI for quarkus-platform is green, 
#### when artifacts are promoted, we remove the <repositories> section,
#### amend the commit, and force push

set -euxo pipefail
 
# undo patch to add repos
cat update-platform-patch.diff | patch -R pom.xml
 
# overwrite old commit (no need to squash)
git commit --amend --no-edit pom.xml
 
# push forced (we are overwriting the old commit)
git push --force-with-lease
