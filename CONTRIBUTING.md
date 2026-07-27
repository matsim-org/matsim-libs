# Contributing to MATSim

MATSim is an open-source framework for large-scale agent-based transport simulations. Contributions of all kinds are welcome, including bug fixes, new features, documentation improvements, performance optimizations, examples, and tests.

## Table of Contents

* Getting Started
* Ways to Contribute
* Development Setup
* Building the Project
* Coding Guidelines
* Testing
* Submitting Changes
* Reporting Issues
* Community

---

# Getting Started

Before contributing, make sure you:

* Have Java and Maven installed.
* Fork this repository.
* Clone your fork locally.
* Create a new branch for your work.

```bash
git clone <<url>>
cd matsim
git checkout -b feature/my-feature
```

Keeping your branch focused on a single change makes reviews much easier. Humans do enjoy making life harder for reviewers, but it isn't mandatory.

---

# Ways to Contribute

You can contribute by:

* Fixing bugs
* Implementing new features
* Improving performance
* Writing or improving documentation
* Adding examples
* Improving tests
* Reviewing pull requests
* Reporting bugs and suggesting enhancements

---

# Development Setup

Clone the repository:

```bash
git clone https://github.com/matsim-org/matsim.git
cd matsim
```

Build the entire project:

```bash
mvn package -DskipTests
```

Build and install only the core module:

```bash
mvn install --also-make --projects matsim
```

---

# Project Structure

The repository is organized into multiple modules.

* **matsim/** - Core framework
* **contribs/** - Optional extensions and contributed modules
* Additional modules may provide examples, utilities, or experimental functionality.

Extensions are located in the `contribs` directory and can be used independently or together with the core framework.

---

# Coding Guidelines

Please follow these guidelines when contributing:

* Keep changes focused and minimal.
* Follow the existing code style.
* Use meaningful variable, method, and class names.
* Avoid unnecessary dependencies.
* Document public APIs.
* Remove unused imports and dead code.
* Keep commits clean and logically separated.

If your contribution changes behavior, update the relevant documentation as well.

---

# Testing

Whenever possible:

* Add tests for new functionality.
* Update existing tests if behavior changes.
* Ensure all tests pass before submitting.

Run tests using:

```bash
mvn test
```

Or build without tests during development:

```bash
mvn package -DskipTests
```

---

# Submitting Changes

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes with clear commit messages.
4. Push the branch to your fork.
5. Open a Pull Request.

A good pull request should:

* Clearly describe the problem.
* Explain the solution.
* Reference any related issue(s).
* Include screenshots or benchmarks if applicable.

---

# Commit Messages

Prefer short, descriptive commit messages.

Examples:

```
Fix routing edge case

Improve event processing performance

Update documentation for controller module

Add unit tests for demand model
```

---

# Reporting Issues

Before opening a new issue:

* Search existing issues.
* Check the FAQ.
* Verify the issue on the latest version if possible.

When reporting a bug, include:

* MATSim version
* Java version
* Operating system
* Steps to reproduce
* Expected behavior
* Actual behavior
* Relevant logs or stack traces

---

# Documentation

Documentation improvements are always appreciated.

If you change:

* APIs
* Configuration options
* User-facing behavior
* Examples

please update the relevant documentation in the same pull request.

---

# Resources

* Project Website: http://www.matsim.org/
* FAQ: https://matsim.org/faq
* Issue Tracker: https://matsim.org/issuetracker
* Releases: https://repo.matsim.org/
* Example Project: https://github.com/matsim-org/matsim-example-project
* Code Examples: https://github.com/matsim-org/matsim-code-examples

