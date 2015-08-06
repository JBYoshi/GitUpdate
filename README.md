# GitUpdate
A one-click program to fetch from and push to all your Git repositories.

When branches are fetched, Git Update attempts to fast-forward your local branches to their specified remote branch. If any merge (automatic or manual) is required, it will not modify that branch. It will also attempt to fast-forward it to a version from a remote named `upstream`, if it exists. To prevent your current branch from being changed, check out a commit or a tag.

Branches are only pushed if they have a configured remote.

Note: This program assumes that all your Git repositories are in `{user.home}/git/`.
