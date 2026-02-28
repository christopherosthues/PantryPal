# Pipelines for Compose Multiplatform apps

Using `git subtree` is a great choice here because it allows you to keep the CI/CD code physically in the repository (so GitHub Actions can see the workflows immediately) while still allowing you to pull updates from your central pipeline repo.

## Add the remote repository

First, add your pipeline repository as a remote so you can refer to it easily. Run this in the root of your project:

```bash
git remote add -f pipeline git@github.com:christopherosthues/compose-multiplatform-pipelines.git
```

## Add the subtree

Now, use the `git subtree add` command to pull the contents of that repository into a specific subdirectory.
Since you want to include it in the `.github` folder, you can choose a prefix like `.github`. Note: GitHub Actions automatically detects YAML files in `.github/workflows`. If your shared repo contains a `workflows` folder, pointing the prefix to `.github` will merge them into your project's existing `.github` structure.
To put everything into `.github`:

```bash
git subtree add  --prefix .github pipeline main --squash
```

## How to pull updates in the future

If you make changes to the code in the `compose-multiplatform-pipelines` repository and want to sync them to your project, run:

```bash
git subtree pull --prefix .github pipeline main --squash
```

## Important Tip for GitHub Actions

If your shared repository contains reusable workflows (e.g., in a `workflows/` folder), and you used the prefix `.github/pipelines`, you would reference them in your main project's workflows like this:

```yaml
jobs:
  call-workflow:
    uses: ./.github/pipelines/workflows/shared-pipeline.yml
```

This setup ensures that your pipelines are version-controlled alongside your code, but managed centrally in your dedicated pipeline repo.
