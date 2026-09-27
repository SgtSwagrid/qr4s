package com.alecdorrington.qr4s

import munit.FunSuite

/** Tests of how text is encoded as a QR code. */
class QrCodeSuite extends FunSuite:

  private val link = "https://example.com/ab3de"

  test("each level leaves as many codewords to data as the standard says"):
    val standard = Map(
      1  -> List(19, 16, 13, 9),
      2  -> List(34, 28, 22, 16),
      7  -> List(156, 124, 88, 66),
      10 -> List(274, 216, 154, 122),
      20 -> List(861, 669, 485, 385),
      35 -> List(2306, 1812, 1286, 986),
      40 -> List(2956, 2334, 1666, 1276),
    )
    standard.foreach((version, codewords) =>
      assertEquals(
        Correction.values.toList.map(_.dataCodewords(version)),
        codewords,
        s"version $version",
      ),
    )

  test("a block's error correction is that of the standard's worked example"):
    // "HELLO WORLD" at version 1, level M.
    val data = Vector(
      32, 91, 11, 120, 209, 114, 220, 77, 67, 64, 236, 17, 236, 17, 236, 17,
    )
    assertEquals(
      ReedSolomon.remainder(data, 10),
      Vector(196, 35, 39, 119, 235, 215, 231, 226, 93, 23),
    )

  test("the format and version information carry the standard's check bits"):
    val formats = Map(
      Correction.Low      -> 0x77C4,
      Correction.Medium   -> 0x5412,
      Correction.Quartile -> 0x355F,
      Correction.High     -> 0x1689,
    )
    formats.foreach((level, bits) =>
      assertEquals(Layout.formatBits(level, 0), bits),
    )
    assertEquals(Layout.versionBits(7), 0x07C94)
    assertEquals(Layout.versionBits(40), 0x28C69)

  test("a text takes the smallest version it fits"):
    val medium = QrCode.of(link)
    val high   = QrCode.of(link, Correction.High)
    assertEquals(medium.map(_.size), Some(25))
    assertEquals(high.map(_.size), Some(33))

  test("a text too long for every version has no code"):
    val longest = "a" * 2331
    assertEquals(
      QrCode.of(longest).map(_.size),
      Some(177),
    )
    assertEquals(QrCode.of(longest + "a"), None)

  test("a code has a finder pattern in three of its corners, not the fourth"):
    val code = QrCode.of(link).get
    val far  = code.size - 7
    assert(finder(code, 0, 0))
    assert(finder(code, far, 0))
    assert(finder(code, 0, far))
    assert(!finder(code, far, far))

  test("the timing patterns alternate between the finder patterns"):
    val code = QrCode.of(link).get
    (8 until code.size - 8).foreach: i =>
      assertEquals(code.dark(i, 6), i % 2 == 0)
      assertEquals(code.dark(6, i), i % 2 == 0)

  test("every mask gives a code of its own"):
    val codes = QrCode.candidates(link, Correction.Medium).get
    assertEquals(
      codes.distinct.size,
      Masking.masks.size,
    )

  test("the path covers every dark module, and no light one, exactly once"):
    Correction
      .values
      .foreach: correction =>
        val code = QrCode.of(link * 20, correction).get
        assertEquals(
          painted(code.path, code.size),
          code.modules,
        )

  test("the image is the path, black, over its quiet zone, white"):
    val code  = QrCode.of(link).get
    val image = code.svg()
    val span  = code.size + 2 * QrCode.quietZone
    assert(image.contains(s"""viewBox="-4 -4 $span $span""""))
    assert(image.contains(s"""width="$span" height="$span" fill="#FFFFFF""""))
    assert(image.contains(s"""<path d="${ code.path }" fill="#000000"/>"""))

  test("the image is drawn in whichever colours it is given"):
    val code  = QrCode.of(link).get
    val image = code.svg(dark = "navy", light = "none")
    assert(image.contains("""fill="none"/>"""))
    assert(image.contains(s"""<path d="${ code.path }" fill="navy"/>"""))

  test("a colour can neither end its attribute nor add markup"):
    val image = QrCode.of(link).get.svg(dark = """red"/><script>&""")
    assert(image.contains("""fill="red&quot;/&gt;&lt;script&gt;&amp;"/>"""))
    assert(!image.contains("<script>"))

  /**
    * The modules of a code of the given size which the given path paints, each
    * `true` where it is painted once, and failing where it is painted twice.
    */
  private def painted(path: String, size: Int): Vector[Vector[Boolean]] =
    val run      = raw"M(\d+) (\d+)h(\d+)v1h-(\d+)z".r
    val covering = run
      .findAllMatchIn(path)
      .toList
      .flatMap: m =>
        val List(x, y, length, back) = m.subgroups.map(_.toInt)
        assertEquals(back, length)
        (x until x + length).map(_ -> y)
    assertEquals(run.replaceAllIn(path, ""), "")
    assertEquals(covering.distinct.size, covering.size)
    val cells = covering.toSet
    Vector.tabulate(size, size)((y, x) => cells.contains(x -> y))

  /**
    * Whether the given code has a finder pattern with its top left corner at
    * the given column and row: a dark ring, a light one, and a dark square.
    */
  private def finder(code: QrCode, left: Int, top: Int): Boolean = (0 until 7)
    .forall(y =>
      (0 until 7).forall(x =>
        code.dark(left + x, top + y) ==
          (math.max((x - 3).abs, (y - 3).abs) != 2),
      ),
    )
