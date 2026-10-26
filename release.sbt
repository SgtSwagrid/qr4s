ThisBuild / description :=
  "Generate QR codes in Scala, on the JVM and in the browser."

ThisBuild / homepage := Some(uri("https://github.com/SgtSwagrid/qr4s"))

ThisBuild / organization         := "com.alecdorrington"
ThisBuild / organizationName     := "SgtSwagrid"
ThisBuild / organizationHomepage := Some(uri("https://github.com/SgtSwagrid"))

// Still in beta: anything may change between minor versions until 1.0.0.
ThisBuild / versionScheme := Some("early-semver")

ThisBuild / licenses :=
  List("MIT" -> uri("https://opensource.org/licenses/MIT"))

ThisBuild / developers := List(Developer(
  id = "SgtSwagrid",
  name = "Alec Dorrington",
  email = "alecdorrington@gmail.com",
  url = uri("https://github.com/SgtSwagrid"),
))
