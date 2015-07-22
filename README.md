# GitUpdate
A one-click program to fetch from and push to all your Git repositories.

Branches are only pushed under the following conditions:
- There must be a valid remote named `origin`. Branches are only pushed to this remote.
- The branch must already exist on `origin`. To push commits to a new branch, push them manually first.

Note: This program assumes that all your Git repositories are in `{user.home}/git/`.
