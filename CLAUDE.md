# CLAUDE.md

This file provides guidance to [Claude Code](https://claude.com/product/claude-code) when working with code in this repository.
It is not intended for human eyes.

### Maintenance

You (robot or human) have standing permission to update this file without asking.
Add important patterns, gotchas, or context that would help future sessions.
Keep it concise and actionable.

## Project overview

This is qr4s, a Scala 3 encoder of QR codes (ISO/IEC 18004) with no dependencies, cross-compiled for the JVM and
Scala.js so that codes can be made in the browser. It is in beta.

It is one module in `com.alecdorrington.qr4s`. `QrCode.of(text, correction)` is the public interface: byte mode
(UTF-8), every version (1-40) and level, the smallest version that fits, the strongest `Correction` that fits a code
of that size, and whichever of the eight masks the standard's penalty (`Masking.penalty`) scores lowest. A `QrCode`
is its `modules`, drawn by `path` (SVG path data, a rectangle per run of dark modules along a row) or `svg`. The
rest is private: `Correction` holds the standard's block tables, `ReedSolomon` the error correction over GF(2⁸),
`Layout` where the finder, timing and alignment patterns, the format and version information and the data go, and
`Masking` the masks.

- It must stay dependency-free and portable: nothing JVM-only in `src/main`, which Scala.js compiles too (the
  `java.nio.charset` it uses is implemented there).
- `ZxingSuite` (`src/test/scalajvm`, as ZXing is a JVM test dependency) holds every version, level and mask to
  ZXing's own encoder, module for module, and scans codes back with ZXing's reader. Keep it passing: it is what says
  the codes are right. Each level is a test of its own, as all four together can outlast munit's 30 seconds on CI.
- Codes are dark on light, always: not every scanner reads one inverted.
- Scalafmt's `align.preset = more` aligns every `%` in a table, as it would a dependency's, which mangles arithmetic;
  such tables (`Correction`, `Masking.masks`) sit between `// format: off` and `// format: on`.

See [README.md](README.md) for usage.

### Where this code lives

This repository is a mirror. The library is developed inside a larger private project, beneath `qr4s/`, and every file
here is copied from there by [GitHub Graph](https://github.com/SgtSwagrid/github-graph) whenever that project's `main`
changes, overwriting whatever is here. So make changes there, never here. The shared configuration (workflows, Scalafmt, IDE settings, `project/plugins-*.sbt`) comes from further upstream still, in
[Scala Library Config](https://github.com/SgtSwagrid/scala-library-config), which syncs into the private project's `qr4s/` first.
`build.sbt`, `release.sbt`, `project/Dependencies.scala`, `project/plugins-scalajs.sbt`, `README.md` and this file
belong to the library.

### Build

- `qr4s` is a project matrix, one row per platform: `qr4s` on the JVM and `qr4sJS` in JS, published as `qr4s`. Their
  ids are the library's name because the private project includes this build by reference
  (`ProjectRef(file("qr4s"), "qr4s")`, `"qr4sJS"`), alongside projects of its own. `qr4sRoot` aggregates them and
  builds the Scaladoc, and is not published.
- The matrix pins its `sourceDirectory` to this build's own `src`: sbt 2 otherwise resolves a matrix's sources against
  the working directory, and included by reference it would compile to an empty JAR.
- The library must never depend on anything in the project that includes it.
- Versions come from git tags (`sbt-ci-release`); publishing a GitHub release publishes to Maven Central.

## Instructions

### Compilation and Diagnostics

- When the user asks for help with a compilation or type error, start by running `sbt compile` to see the error for yourself.
  If there are many errors, making it unclear which one the user is referring to, ask them to clarify, and then focus only on that issue.
- IntelliJ MCP integration is active. When a request seems to implicitly refer to something the user is looking at, always check
  `mcp__ide__getDiagnostics` first to see which file(s) are open and get associated diagnostics (errors, warnings, and info hints with line numbers).

#### Testing

- After making code changes, always run `sbt compile` to verify that issues are fixed and no new ones are introduced.
- Repeatedly retry upon failure until the build succeeds. If you are unsure how to fix an issue, ask for help or refer to existing code for examples.
- Before trying to fix an error, make sure you first understand it fully.
- You should never report that a feature is complete without testing it first.

### Code Style

- You must read the [Code Style Guidelines](docs/STYLE_GUIDE.md).

### Pull Requests

When asked to publish the code changes, your task is to open one or more pull requests (PRs) to merge the changes into `main` on GitHub:

- Use `git` to check what has changed as compared to the `main` branch on `origin`.
- If the changes are thematically linked, they can be published as a single PR.
- Otherwise, you'll need to divide the changes into multiple PRs using your own judgement.
- Each PR should have a singular focus, shouldn't break anything, and should be able to be merged independently.
- Ensure that all code is staged, committed and pushed. Ensure no new files are left uncommitted, and no debug code is left in the codebase.
- When creating a PR, ensure that the title and description are clear, informative, and comprehensive.
- All feature/bugfix/etc branch names should be formatted as "feature_<short description>" or "fix_<short description>" or similar.
- All PR titles should be formatted as "[<scope>] <Short summary>", e.g. "[renderer] Fixed colour inversion bug."
- You have GitHub MCP integration that can be used to do the above.
