## Release Checklist

1. Increment version in `kscript`
2. Make sure that support api version is up to date and available from jcenter
3. push and create github release tag
4. Update release branch

```bash
#http://stackoverflow.com/questions/13969050/how-to-create-a-new-empty-branch-for-a-new-project
git clone git@github.com:holgerbrandl/kscript.git kscript_releases
cd kscript_releases
#git checkout --orphan releases
#git reset --hard
#git rm --cached -r .

git checkout releases
cp ~/projects/kotlin/kscript/expandcp.kts ~/projects/kotlin/kscript/kscript .
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
