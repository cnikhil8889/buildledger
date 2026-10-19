# Put BuildLedger on GitHub

Suggested repository name: **buildledger**. Description: **Android construction management app for builders, office teams and workers.**

## With GitHub Desktop

1. Extract `BuildLedger-GitHub.zip`.
2. In GitHub Desktop, select **File → Add local repository**, choose the extracted `buildledger` folder, then use **create a repository here** if prompted. Keep the existing project files; do not replace the supplied README or .gitignore.
3. Review the changed files, enter **Initial BuildLedger project** and commit to `main`.
4. Select **Publish repository**, keep **Keep this code private** selected and publish.

The `.github` folder contains the automated build. Include it when committing.

## With Git

Create an empty **private** repository named `buildledger` on GitHub. Leave GitHub's README, .gitignore and license options unchecked because this project already supplies its files.

From inside the extracted `buildledger` folder:

```bash
git init -b main
git add .
git status
git commit -m "Initial BuildLedger project"
git remote add origin https://github.com/YOUR-USERNAME/buildledger.git
git push -u origin main
```

Replace `YOUR-USERNAME` with your GitHub username. Use your own Git identity and GitHub sign-in. The archive intentionally has no Git history or account credentials.

## Download a CI APK

After the first push, open **Actions → BuildLedger Android CI**. Open the successful run and download **BuildLedger-debug-apk** from **Artifacts**. Extract the downloaded artifact ZIP to find the APK.

If the Actions page offers an **Enable workflows** button, enable it for this repository. A queued run is waiting for a runner; a failed run contains the error in its red step. CI needs first-run internet access to download the pinned toolchain and dependencies.

The default APK shows backend setup pending. To make a configured private test build, follow [RELEASING.md](RELEASING.md) locally with your existing backend configuration and signing key. The CI debug APK has a different package/certificate from the existing Cloud Test installation.

## Files kept outside Git

The supplied ignore rules exclude local backend configuration, .env files, signing keys, APK/AAB outputs, build directories, test screenshots/reports, dependencies and IDE state. Existing customer records and exported backups are not part of this source package.

The repository workflow uses read-only repository permission, pinned action commits and no deployment credentials. It runs tests/builds only. No GitHub repository has been created merely by extracting this ZIP.

Official help: [Create a repository](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-new-repository), [GitHub Desktop local repositories](https://docs.github.com/en/desktop/adding-and-cloning-repositories/adding-an-existing-project-to-github-using-github-desktop), [Workflow artifacts](https://docs.github.com/en/actions/managing-workflow-runs/downloading-workflow-artifacts).
