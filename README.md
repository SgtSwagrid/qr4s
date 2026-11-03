<div align="center">

  <h1>🔳 qr4s</h1>
  <p>Generate QR codes in <a href="https://www.scala-lang.org/">Scala</a>, on the JVM and in the browser.</p>

  <span>
    <a href="https://github.com/SgtSwagrid/qr4s/actions/workflows/build-integrity.yml"><img src="https://github.com/SgtSwagrid/qr4s/actions/workflows/build-integrity.yml/badge.svg" alt="Build status" /></a>
    <a href="https://search.maven.org/artifact/com.alecdorrington/qr4s_3"><img src="https://img.shields.io/maven-central/v/com.alecdorrington/qr4s_3.svg" alt="Maven Central" /></a>
    <a href="https://alecdorrington.com/qr4s"><img src="https://img.shields.io/badge/docs-latest-blue.svg" alt="Documentation" /></a>
  </span>

</div>

> [!WARNING]
> qr4s is still in beta. Anything could change between minor versions.

An encoder of QR codes for [Scala 3](https://www.scala-lang.org/), as per [ISO/IEC 18004](https://www.iso.org/standard/83389.html).

### Features
- Encodes any UTF-8 string as a QR code.
- Supports export to SVG.
- Works both on the [JVM](https://en.wikipedia.org/wiki/Java_virtual_machine), and in the browser with [Scala.js](https://www.scala-js.org/).
- Supports every [version](https://en.wikipedia.org/wiki/QR_code#Information_capacity) (1 to 40) and [error correction](https://en.wikipedia.org/wiki/QR_code#Error_correction) level.
- Automatically picks the best [mask](https://en.wikipedia.org/wiki/QR_code#Format_information_and_masking) for each input.
- Automatically increases the error correction level when there is spare information capacity.

## ⬇️ Installation

> [!NOTE]
> The following applies for [Scala 3](https://www.scala-lang.org/) projects built with [sbt 2](https://www.scala-sbt.org/).

Add the library to your `build.sbt` file, with the desired version number (e.g. [latest](../../releases/latest)):

```scala
libraryDependencies += "com.alecdorrington" %% "qr4s" % "{version}"
```

## 🚀 Usage

### Creating a QR code

[`QrCode.of`](src/main/scala/com/alecdorrington/qr4s/QrCode.scala) encodes a string in the smallest QR code that can fit it
(or `None` if it is too long for even the largest size).

```scala
import com.alecdorrington.qr4s.QrCode

val code: Option[QrCode] = QrCode.of("https://example.com")
```

### Drawing a QR code

`.svg()` writes the code as an SVG image (black-on-white, by default).

```scala
val image = code.map(_.svg()) // <svg xmlns="http://www.w3.org/2000/svg" viewBox="-4 -4 33 33" ...
```

The colours can be changed by passing hex strings.
Alternatively, use `none` for transparent, or `currentColor` for the colour of the text around an inlined image.

```scala
code.map(_.svg(dark = "#312E81", light = "#EEF2FF"))
```

Or draw it any way you like, voxel-by-voxel:

```scala
val code: QrCode = ???
val voxels = for
  y <- 0 until code.size
  x <- 0 until code.size
  if code.dark(x, y)
yield (x, y)
```

> [!NOTE]
> Make sure to maintain sufficient contrast between the light and dark colours, or else some QR scanners may struggle.

### Error correction

A [`Correction`](src/main/scala/com/alecdorrington/qr4s/Correction.scala) level determines how much of a code may be smudged, torn or
misread while completely preserving readability. The options include `Low` (about 7%), `Medium` (15%, the default),
`Quartile` (25%) or `High` (30%). Of course, higher levels of error correction will typically generate larger QR codes, given the same data.

```scala
import com.alecdorrington.qr4s.Correction

QrCode.of("https://example.com", Correction.High)
```

## 👁️ See also
- [ZXing](https://zxing.org/w/decode.jspx) is a library which supports encoding and decoding QR codes, alongside other similar formats like barcodes.
  _qr4s_ by contrast is much simpler: it doesn't support decoding, and it only supports QR codes.
  However, ZXing is written in Java and so only supports the JVM; it won't allow you to generate codes in a browser.
- This library was made using [Scala Library Template](https://github.com/SgtSwagrid/scala-library-template).
