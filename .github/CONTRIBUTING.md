# Contributing to RTDE4J Getting Started

Thank you for helping improve the public examples, tutorials, and documentation for **RTDE4J**.

This repository is a **public learning and examples project**. Use the published `com.javiergs:rtde4j:1.0.0` Maven dependency.

## What to contribute

We welcome:

- Clearer installation, URSim setup, and troubleshooting instructions.
- Small, reproducible Java examples using the published RTDE4J API.
- Tests and fixes for public examples.
- Tutorials explaining RTDE state monitoring, URScript commands, joint positions, and TCP poses.
- Examples that compare a requested command with observed RTDE feedback, including tolerance and timeout behavior.
- Documented limitations and simulator observations.

For robot-specific applications or research prototypes developed by the ROVER team, use the team's [ASU26-ROVER repository](https://github.com/calhci/ASU26-ROVER) and its accepted-story/task workflow. Propose an example here when it is general, documented, and reproducible for other learners.

**Do not submit library implementation code here.** If you identify a missing library capability, describe the use case and expected API behavior in an issue or PR; maintainers will evaluate implementation changes separately.

## Propose a change

You do not need write access to this repository.

1. **Fork** `javiergs/rtde4j-getting-started` to your GitHub account.
2. Create a focused branch in your fork, such as `docs/ursim-troubleshooting` or `examples/read-joints`.
3. Make one well-scoped change. Keep the published library version unchanged unless a maintainer requests an update.
4. Document prerequisites and exact steps to reproduce your results.
5. Run `mvn clean verify`. For URSim-dependent examples, also run them against a simulator and record the observed behavior. Compilation alone does **not** prove runtime correctness.
6. Push the branch and open a **pull request targeting this repository's `main` branch**.

## Pull request checklist

Please include:

- **Purpose:** What problem does the change solve?
- **Changes:** What examples or documentation changed?
- **How to test:** Exact commands, Java/Maven versions, URSim version, and networking setup.
- **Results:** Observed output, screenshots, or logs where relevant.
- **RTDE and URScript details:** Which interface and port were used; monitored fields, units, commands, and any feedback-based validation.
- **Limitations and safety:** What was tested only in URSim; what was *not* tested on physical hardware.
- **Related work:** Link a ROVER issue or PR if the work originated there.

Use Java 21+, readable Java code, descriptive names, JavaDoc for reusable public classes or methods, and appropriate exception handling. Avoid credentials, personal data, private network addresses, compiled artifacts, and unrelated formatting changes.

## Simulator and robot safety

Use **URSim** for student experiments. RTDE monitoring (normally TCP port `30004`) reads robot state; URScript (normally TCP port `30002`) can send commands that move the robot. Do not run experimental movement examples on physical hardware without explicit instructor authorization and an appropriate safety assessment.

A successfully sent URScript command does **not** establish that the robot executed it or reached the target. When testing movement, use bounded commands, appropriate limits, RTDE feedback, tolerances, and timeouts. Software stop commands are **not** emergency-stop substitutes.

## Review and merging

Submit changes through a pull request for instructor/maintainer review. Do not push directly to `main` or merge your own changes. Address review feedback before the maintainer merges the PR. Opening a PR does not guarantee acceptance.

## Reporting issues

For documentation or example bugs, open an issue with OS, Java and Maven versions, URSim version, reproduction steps, expected and actual behavior, and relevant logs. Do not post secrets or private robot connection details.
