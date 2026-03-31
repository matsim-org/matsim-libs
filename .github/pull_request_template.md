We are using [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/), so that we are able to auto generate a changelog. The format is described below:

#### Title
`<type>[optional scope]: <description>` e.g.: fix(QSim): Correctly calculate travel times on links

Available types:
 - feat: A new feature
 - fix: A bug fix
 - docs: Documentation only changes
 - style: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
 - refactor: A code change that neither fixes a bug nor adds a feature
 - perf: A code change that improves performance
 - test: Adding missing tests or correcting existing tests
 - build: Changes that affect the build system or external dependencies (example scopes: gulp, broccoli, npm)
 - ci: Changes to our CI configuration files and scripts (example scopes: Travis, Circle, BrowserStack, SauceLabs)
 - chore: Other changes that don't modify src or test files
 - revert: Reverts a previous commit

#### Body
Optionally, describe the change in more detail.

If the PR introduces a breaking change write: BREAKING CHANGE at the bottom of the message
