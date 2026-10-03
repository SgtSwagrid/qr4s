import IdeSettings.packagePrefix
import sbt._
import sbt.Keys._
import sbtunidoc.BaseUnidocPlugin.autoImport.*
import sbtunidoc.ScalaUnidocPlugin

// Every project is named after the library so as not to clash with the
// projects of a build that includes this one by reference.

val scala3 = "3.9.0"

ThisBuild / scalaVersion := scala3

ThisBuild / scalacOptions ++= Seq(
  "-explain",
  "-explain-types",
  "-explain-cyclic",
)

lazy val qr4s = projectMatrix
  .in(file("."))
  .settings(
    name          := "qr4s",
    packagePrefix := "com.alecdorrington.qr4s",

    // A matrix resolves sources against the working directory, which is not
    // this build's base when another build includes it by reference.
    sourceDirectory := (ThisBuild / baseDirectory).value / "src",
    Dependencies.munit,
  )
  // ZXing is JVM only.
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

    // The root shares the matrix's directory, so must not build its sources.
    Compile / unmanagedSourceDirectories := Nil,
    Test / unmanagedSourceDirectories    := Nil,

    // The JS side would only document the same sources again.
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(qr4s.jvm(scala3)),
    ScalaUnidoc / unidoc / scalacOptions ++= Seq("-project", "qr4s"),
  )
