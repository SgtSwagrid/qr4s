import sbt.*
import sbt.Keys.*

/** External library dependencies, all of them for testing alone. */
object Dependencies:

  /** The version to use for each dependency. */
  object V:

    val munit = "1.3.3"
    val zxing = "3.5.3"

  /** Library dependencies for testing with MUnit. */
  lazy val munit = libraryDependencies ++=
    Seq("org.scalameta" %% "munit" % V.munit % Test)

  /**
    * [ZXing](https://github.com/zxing/zxing), an encoder and reader of QR codes
    * written apart from this one, to test that the codes made here are the
    * same, and scan. JVM only.
    */
  lazy val zxing = libraryDependencies +=
    "com.google.zxing" % "core" % V.zxing % Test
