## Release Checklist

1. Increment version in `kscript`
2. Make sure that support api version is up to date and available from jcenter
3. Push and create github release tag
```bash
export KSCRIPT_HOME="/Users/brandl/projects/kotlin/kscript"; 
export PATH=${KSCRIPT_HOME}:${PATH}
export PATH=~/go/bin/:$PATH

kscript_version=$(grep '^KSCRIPT_VERSION' ${KSCRIPT_HOME}/kscript | cut -f2 -d'=')
echo "new version is $kscript_version" 
## see https://github.com/aktau/github-release

## create and upload deployment file for sdkman
KSCRIPT_ARCHIVE=~/Dropbox/archive/kscript_versions/

mkdir -p $KSCRIPT_ARCHIVE/kscript-${kscript_version}/bin
cp ${KSCRIPT_HOME}/kscript ${KSCRIPT_ARCHIVE}/kscript-${kscript_version}/bin

cd ${KSCRIPT_ARCHIVE}
zip  -r ${KSCRIPT_ARCHIVE}/kscript-${kscript_version}.zip kscript-${kscript_version}
open ${KSCRIPT_ARCHIVE}


## create tag on github 
#github-release --help 

export GITHUB_TOKEN=${GH_TOKEN}
#echo $GITHUB_TOKEN


# make your tag and upload
cd ${KSCRIPT_HOME}

#git tag v${kscript_version} && git push --tags
(git diff --exit-code && git tag v${kscript_version})  || echo "could not tag current branch"
git push --tags

# check the current tags and existing releases of the repo
github-release info -u holgerbrandl -r kscript

# create a formal release
github-release release \\
    --user holgerbrandl \\
    --repo kscript \\
    --tag "v${kscript_version}" \\
    --name "v${kscript_version}" \\
    --description "NEWS.md](https://github.com/holgerbrandl/kscript/blob/master/NEWS.md)" 
#    \\
#    --pre-release


## upload sdk-man binary set
github-release upload \\
    --user aktau \\
    --repo gofinance \\
    --tag v0.1.0 \\
    --name "gofinance-osx-amd64" \\
    --file bin/darwin/amd64/gofinance
```


4. Update release branch

```bash
#http://stackoverflow.com/questions/13969050/how-to-create-a-new-empty-branch-for-a-new-project
git clone git@github.com:holgerbrandl/kscript.git kscript_releases
cd kscript_releases
#git checkout --orphan releases
#git reset --hard
#git rm --cached -r .

git checkout releases
cp ~/projects/kotlin/kscript/resdeps.kts ~/projects/kotlin/kscript/kscript .
git add -A 
git status
git commit -m "v1.4 release"

git push origin releases
```

5. create new version on jcenter

* Upload artifacts from ~/.m2/repository/de/mpicbg/scicomp/kutils to:
> https://bintray.com/holgerbrandl/mpicbg-scicomp/kutils

1. Check for release status on
https://jcenter.bintray.com/de/mpicbg/scicomp/

7. Bump versions for new release cycle
