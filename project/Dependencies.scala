import sbt.*
import sbt.Keys.*

object Dependencies:

  object V:

    val munit = "1.3.3"
    val zxing = "3.5.3"

  lazy val munit = libraryDependencies ++=
    Seq("org.scalameta" %% "munit" % V.munit % Test)

  /** [ZXing](https://github.com/zxing/zxing), to test against. JVM only. */
  lazy val zxing = libraryDependencies +=
    "com.google.zxing" % "core" % V.zxing % Test
