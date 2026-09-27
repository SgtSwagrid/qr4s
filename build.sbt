import IdeSettings.packagePrefix
import sbt._
import sbt.Keys._
import sbtunidoc.BaseUnidocPlugin.autoImport.*
import sbtunidoc.ScalaUnidocPlugin

// This build is developed as part of a larger private project,
// which includes it by reference and from which it is automatically synchronised.
// Every project is named after the library, so that none clashes with a host's own.

val scala3 = "3.8.4"

ThisBuild / scalaVersion := scala3

ThisBuild / scalacOptions ++= Seq(
  "-explain",
  "-explain-types",
  "-explain-cyclic",
)

/**
  * An encoder of QR codes, with no dependencies. Cross-compiled for JVM and JS,
  * so that codes can be made in the browser.
  */
lazy val qr4s = projectMatrix
  .in(file("."))
  .settings(
    name          := "qr4s",
    packagePrefix := "com.alecdorrington.qr4s",

    // A matrix resolves its sources against the working directory, which is
    // not this build's own when a host includes it by reference:
    sourceDirectory := (ThisBuild / baseDirectory).value / "src",
    Dependencies.munit,
  )
  // The JVM's tests also hold each code to ZXing's, and scan it back:
  .jvmPlatform(
    scalaVersions = Seq(scala3),
    axisValues = Nil,
    configure = _.settings(Dependencies.zxing),
  )
  .jsPlatform(scalaVersions = Seq(scala3))

lazy val qr4sRoot = project
  .in(file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(qr4s.projectRefs *)
  .settings(
    publish / skip := true,

    // It shares the matrix's directory, so would build its sources a third
    // time without the matrix's settings, had it not none of its own:
    Compile / unmanagedSourceDirectories := Nil,
    Test / unmanagedSourceDirectories    := Nil,

    // Scaladoc is aggregated from the JVM side alone, as the JS side would
    // only document the same sources a second time:
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(qr4s.jvm(scala3)),
    ScalaUnidoc / unidoc / scalacOptions ++= Seq("-project", "qr4s"),
  )
