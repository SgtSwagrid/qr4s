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
is its `modules`, drawn by `svgPath` (SVG path data, a rectangle per run of dark modules along a row) or
`svg(dark, light)` (black on white unless given other SVG colours, each escaped into its attribute by `quoted`). The
rest is private: `Correction` holds the standard's block tables, `ReedSolomon` the error correction over GF(2⁸),
`Layout` where the finder, timing and alignment patterns, the format and version information and the data go, and
`Masking` the masks.

- It must stay dependency-free and portable: nothing JVM-only in `src/main`, which Scala.js compiles too (the
  `java.nio.charset` it uses is implemented there).
- `ZxingSuite` (`src/test/scalajvm`, as ZXing is a JVM test dependency) holds every version, level and mask to
  ZXing's own encoder, module for module, and scans codes back with ZXing's reader. Keep it passing: it is what says
  the codes are right. Each level is a test of its own, as all four together can outlast munit's 30 seconds on CI.
- Codes are dark on light, always: not every scanner reads one inverted. `svg` takes any colours, and its
  documentation tells callers to keep the dark much darker; it doesn't check them.
- Scalafmt's `align.preset = more` aligns every `%` in a table, as it would a dependency's, which mangles arithmetic;
  such tables (`Correction`, `Masking.masks`) sit between `// format: off` and `// format: on`.

See [README.md](README.md) for usage.

### Where this code lives

qr4s is developed here: make every change in this repository, by pull request. The shared configuration (workflows,
Scalafmt, IDE settings, `project/plugins-common.sbt`, `project/plugins-library.sbt`, `LICENSE.md`, `CONTRIBUTING.md`)
is synced into it from [Scala Library Config](https://github.com/SgtSwagrid/scala-library-config) by
[GitHub Graph](https://github.com/SgtSwagrid/github-graph), which overwrites local edits, so change those files there.
`build.sbt`, `release.sbt`, `project/Dependencies.scala`, `project/plugins-scalajs.sbt`, `README.md`, this file and the
sources belong to the library.

Other projects use qr4s as a dependency from Maven Central, never from source, so a change one of them needs reaches
it only through a release (below).

### Build

- `qr4s` is a project matrix, one row per platform: `qr4s` on the JVM and `qr4sJS` in JS, published as `qr4s_3` and
  `qr4s_sjs1_3`. `qr4sRoot` aggregates them and builds the Scaladoc (of the JVM side only), and is not published.
- The matrix pins its `sourceDirectory` to this build's own `src`, as sbt 2 otherwise resolves a matrix's sources
  against the working directory, which is not this build's when another includes it.

### Releases

1. Land the changes on `main` by pull request, the last of them setting the README's installation line to the new
   version. qr4s is in beta, versioned by early semver (`versionScheme`): while it is `0.x`, the minor version goes up
   for any change that can break a user's build or behaviour, and the patch version for anything else.
2. Publish a GitHub release of `main` tagged `v<version>`, with notes written for a user, as the earlier releases' are
   (the installation line, then what is Changed, Added and Fixed). `publish.yml` then publishes `qr4s_3` and
   `qr4s_sjs1_3` to Maven Central through `sbt-ci-release`, which takes the version from the tag, and `docs.yml`
   publishes the Scaladoc at [alecdorrington.com/qr4s](https://alecdorrington.com/qr4s/).
3. Once Maven Central serves the new version, bump it in each project using qr4s, by pull request there. Their
   Scala Steward would otherwise propose it on its next weekly run. A version that breaks callers needs their code
   changed too.

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
- Document every public type and member: a summary, then `@param` for each explicit parameter,
  `@tparam` for each type parameter, and `@return` for any result but `Unit`, each one short
  sentence. Summaries read: types "A ...", values "The ...", Booleans "Whether ...", methods a
  third-person verb ("Sends ..."), never "Returns ...". Private members get a comment only for a
  non-obvious contract or gotcha, usually in one sentence.

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
- Use the `gh` CLI to do the above.
