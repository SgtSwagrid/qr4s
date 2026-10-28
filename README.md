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
> qr4s is in beta. It is young, it has one user, and anything may change between minor versions.

An encoder of QR codes, as their standard (ISO/IEC 18004) describes them, written in plain Scala with no
dependencies, so that it runs unchanged in the browser with [Scala.js](https://www.scala-js.org/).
[ZXing](https://github.com/zxing/zxing) is Java, and so cannot.

- Every version (1 to 40) and every level of error correction.
- The standard's penalty picks the mask that makes each code easiest to read.
- A code gets the strongest error correction that fits its size, so short text gets extra robustness for free.
- A code can be drawn as SVG, or read module by module to draw it any other way.

Every code is tested against ZXing's own encoder, module for module, and scanned back by ZXing's reader.

## ⬇️ Installation

Add the following to your `build.sbt`:

```scala
libraryDependencies += "com.alecdorrington" %% "qr4s" % "0.1.0"
```

With sbt 1, write `%%%` in a Scala.js project. Compiled with Scala `3.9.0`, with no intention to explicitly support
older versions.

## 🚀 Usage

[`QrCode.of`](src/main/scala/QrCode.scala) encodes text as UTF-8, in the smallest code it fits, or
`None` if it is too long for any:

```scala
import com.alecdorrington.qr4s.QrCode

val code: Option[QrCode] = QrCode.of("https://example.com")
```

### Drawing a code

`svg` is the code as an SVG image of its own: black on white, in the margin scanners need around it,
scaling to whatever box it is given.

```scala
code.map(_.svg()) // <svg xmlns="http://www.w3.org/2000/svg" viewBox="-4 -4 33 33" ...
```

To match the colours around it, give it others: any colour SVG knows, for the dark modules and for the light
ones with the margin. `currentColor` is the colour of the text around an image inlined in a page, and `none`
leaves the light modules transparent, over a light background of the page's own.

```scala
code.map(_.svg(dark = "#312E81", light = "#EEF2FF"))
```

To build the image yourself, as with a UI library's own SVG elements, `svgPath` is the dark modules as the data
of one SVG path, one unit to a module. Fill it dark, over a light background reaching `QrCode.quietZone`
modules beyond the code on every side.

Or draw it any way you like, module by module:

```scala
code.map: qr =>
  for
    y <- 0 until qr.size
    x <- 0 until qr.size
    if qr.dark(x, y)
  yield (x, y)
```

Whatever its colours, keep a code much darker than its background: not every scanner reads one inverted, or one
of little contrast.

### Error correction

A [`Correction`](src/main/scala/Correction.scala) level says how much of a code may be smudged, torn or
misread with it still being read, paid for in room for data: `Low` (about 7%), `Medium` (15%, the default),
`Quartile` (25%) or `High` (30%). It is the least a code gets: where a stronger level fits a code of the same
size, that is used instead.

```scala
import com.alecdorrington.qr4s.Correction

QrCode.of("https://example.com", Correction.High)
```

## 👁️ See also

- [Hecate](https://github.com/SgtSwagrid/hecate), a sibling, for user accounts, sessions, groups and permissions.
- [Eunomia](https://github.com/SgtSwagrid/eunomia), a sibling, for filtering, ordering and paging lists.
- [Iris](https://github.com/SgtSwagrid/iris), a sibling, a provider-agnostic client for large language models.
- [Dike](https://github.com/SgtSwagrid/dike), a sibling, for ranking by pairwise comparison.
- This library was made using [Scala Library Template](https://github.com/SgtSwagrid/scala-library-template).
