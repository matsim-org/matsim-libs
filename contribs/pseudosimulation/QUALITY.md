# PSim quality harness

The PSim quality harness gives contributors one local gate for formatting,
linting, static analysis, unit tests, and coverage. All versioned configuration
is contained in this contrib.

## Requirements

- Java 25
- Maven 3.8 or newer
- Git, when using the pre-commit hook

Run the complete gate from anywhere in the repository:

```bash
contribs/pseudosimulation/scripts/quality.sh
```

The first run downloads the pinned Maven plugins and may take longer. Later
runs reuse Maven's local cache. A failed stage prints its report location or a
specific repair command.

## Formatting

Formatting uses Spotless and the Eclipse JDT formatter. Spotless ratchets from
`origin/main`: existing formatting debt does not block unrelated work, while
Java files changed on a branch must conform to the formatter.

The commit hook only checks formatting; it never changes files. Apply fixes
explicitly, inspect them, and stage them again:

```bash
contribs/pseudosimulation/scripts/format.sh
```

## Pre-commit hook

Install the versioned hook once per clone:

```bash
contribs/pseudosimulation/scripts/install-pre-commit-hook.sh
```

The installer creates a symbolic link under `.git/hooks`. It is idempotent and
refuses to overwrite any existing unmanaged hook. If a hook already exists,
make that hook invoke `contribs/pseudosimulation/scripts/pre-commit`.

The hook skips commits without relevant PSim changes. Maven reads the working
tree rather than Git's index, so the hook rejects partially staged relevant
files instead of validating content different from the pending commit.

## Quality gates and ratcheting

The initial gates intentionally prevent regression without requiring all
historic debt to be repaired in this setup change:

- Checkstyle enables a small set of objective, low-noise rules.
- SpotBugs reports only high-priority findings at minimum analysis effort.
- Tests permit no failures.
- JaCoCo requires at least 1% line coverage and 1% branch coverage.

The two coverage properties live in `pom.xml`. Treat every gate as a monotonic
ratchet: increase coverage floors, add lint rules, and increase SpotBugs effort
or sensitivity as the refactor progresses. Never lower or remove a gate merely
to make a change pass. Narrow, documented exclusions are acceptable only when
a finding is proven to be a false positive and cannot be expressed more safely.

JaCoCo's HTML report is written to `target/site/jacoco/index.html`. Surefire and
SpotBugs details are written under `target/surefire-reports` and
`target/spotbugsXml.xml`, respectively.
