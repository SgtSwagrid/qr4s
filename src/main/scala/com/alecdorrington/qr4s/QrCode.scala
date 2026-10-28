package com.alecdorrington.qr4s

import java.nio.charset.StandardCharsets

/**
  * A QR code: a square of dark and light modules. Scanners need a light margin
  * around it, the [[QrCode.quietZone]], which is not part of it.
  *
  * @param modules
  *   Whether each module is dark, row by row from the top, each row from the
  *   left.
  */
final case class QrCode(modules: Vector[Vector[Boolean]]):

  /** The width and height of the code in modules. */
  def size: Int = modules.size

  /**
    * Whether the module in a given column and row is dark.
    *
    * @param x
    *   The column, from `0` at the left.
    *
    * @param y
    *   The row, from `0` at the top.
    *
    * @return
    *   Whether the module is dark.
    */
  def dark(x: Int, y: Int): Boolean = modules(y)(x)

  /**
    * The dark modules as the data of one SVG path, one unit per module, with
    * the code's top left corner at the origin. Fill it dark over a light
    * background reaching the [[QrCode.quietZone]] beyond the code on every
    * side.
    */
  def svgPath: String = modules
    .zipWithIndex
    .flatMap((row, y) =>
      QrCode
        .runs(row)
        .map((x, length) => s"M$x ${ y }h${ length }v1h-${ length }z"),
    )
    .mkString

  /**
    * Draws the code as a standalone SVG image, quiet zone included, scaling to
    * fit any box. Keep the dark colour much darker than the light, as not every
    * scanner reads an inverted or low-contrast code.
    *
    * @param dark
    *   The colour of the dark modules, in any SVG syntax, such as `#1C2333` or
    *   `currentColor`.
    *
    * @param light
    *   The colour of the light modules and the quiet zone, or `none` for
    *   transparent over a light background.
    *
    * @return
    *   An `<svg>` element, as markup.
    */
  def svg
    (
      dark: String = "#000000",
      light: String = "#FFFFFF",
    )
    : String =
    val margin = QrCode.quietZone
    val span   = size + 2 * margin
    val box    = s"-$margin -$margin $span $span"
    val ink    = QrCode.quoted(dark)
    val paper  = QrCode.quoted(light)
    s"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="$box" shape-rendering="crispEdges">""" +
      s"""<rect x="-$margin" y="-$margin" width="$span" height="$span" fill="$paper"/>""" +
      s"""<path d="$svgPath" fill="$ink"/></svg>"""

/** An encoder of text as [[QrCode]]s, in byte mode, by ISO/IEC 18004. */
object QrCode:

  /** The width in modules of the light margin scanners need around a code. */
  val quietZone: Int = 4

  private val versions = 1 to 40

  private val byteMode = 0x4

  private val padding = Vector(0xEC, 0x11)

  /**
    * Encodes text, as UTF-8, in the smallest code that holds it, under the mask
    * that makes it easiest to read.
    *
    * @param text
    *   The text to encode.
    *
    * @param correction
    *   The weakest level of error correction to use. A stronger level is used
    *   where it fits a code of the same size.
    *
    * @return
    *   A code, or `None` when the text is too long for any.
    */
  def of
    (
      text: String,
      correction: Correction = Correction.Medium,
    )
    : Option[QrCode] = candidates(text, correction).map(_.minBy(code =>
    Masking.penalty(code.modules),
  ))

  /** The codes [[of]] chooses between: one under each mask. */
  private[qr4s] def candidates
    (text: String, correction: Correction)
    : Option[Vector[QrCode]] =
    val bytes = text.getBytes(StandardCharsets.UTF_8).toVector.map(_ & 0xFF)
    versions
      .find(version => fits(bytes.size, version, correction))
      .map: version =>
        val strongest = Correction
          .values
          .filter(level =>
            level.ordinal >= correction.ordinal &&
            fits(bytes.size, version, level),
          )
          .last
        masked(bytes, version, strongest)

  private def fits
    (
      bytes: Int,
      version: Int,
      correction: Correction,
    )
    : Boolean = 4 + lengthBits(version) + 8 * bytes <=
    8 * correction.dataCodewords(version)

  private def lengthBits(version: Int): Int = if version < 10 then 8 else 16

  private def masked
    (
      bytes: Vector[Int],
      version: Int,
      correction: Correction,
    )
    : Vector[QrCode] =
    val patterns = Layout.patterns(version)
    val unmasked = placed(
      bytes,
      version,
      correction,
      patterns.keySet,
    )
    Masking
      .masks
      .indices
      .toVector
      .map(mask =>
        drawn(
          version,
          patterns ++ Layout.formatInformation(version, correction, mask) ++
            flipped(unmasked, mask),
        ),
      )

  private def placed
    (
      bytes: Vector[Int],
      version: Int,
      correction: Correction,
      patterns: Set[Position],
    )
    : Vector[(Position, Boolean)] =
    val order = Layout.dataOrder(
      version,
      patterns ++ Layout.formatArea(version),
    )
    val codewords = interleaved(
      data(bytes, version, correction),
      version,
      correction,
    )
    val bits = codewords.flatMap(bitsOf(_, 8))
    order.zip(bits.padTo(order.size, false))

  private def flipped
    (
      modules: Vector[(Position, Boolean)],
      mask: Int,
    )
    : Vector[(Position, Boolean)] = modules.map { case ((x, y), dark) =>
    (x, y) -> (dark ^ Masking.masks(mask)(x, y))
  }

  private def drawn
    (
      version: Int,
      modules: Map[Position, Boolean],
    )
    : QrCode =
    val size = Layout.size(version)
    QrCode(Vector.tabulate(size, size)((y, x) => modules((x, y))))

  /**
    * The data codewords: the mode, the byte count, the bytes, a terminator of
    * up to four zero bits, then padding to fill the room exactly.
    */
  private def data
    (
      bytes: Vector[Int],
      version: Int,
      correction: Correction,
    )
    : Vector[Int] =
    val room    = correction.dataCodewords(version)
    val content = bitsOf(byteMode, 4) ++
      bitsOf(bytes.size, lengthBits(version)) ++ bytes.flatMap(bitsOf(_, 8))
    val ended = content ++
      Vector.fill(math.min(4, 8 * room - content.size))(false)
    val written = ended.grouped(8).map(byteOf).toVector
    written ++
      Vector.tabulate(room - written.size)(i => padding(i % padding.size))

  /** The data codewords in blocks, interleaved, then their error correction. */
  private def interleaved
    (
      data: Vector[Int],
      version: Int,
      correction: Correction,
    )
    : Vector[Int] =
    val blocks = split(data, correction.blocks(version))
    val checks =
      blocks.map(ReedSolomon.remainder(correction.checkCodewords(version)))
    interleave(blocks) ++ interleave(checks)

  /** Splits codewords into blocks, the shorter first, as the standard orders. */
  private def split(codewords: Vector[Int], count: Int): Vector[Vector[Int]] =
    val short = codewords.size / count
    val longs = codewords.size % count
    val ends  = (0 to count).map(i =>
      i * short + math.max(0, i - (count - longs)),
    )
    ends
      .zip(ends.tail)
      .map((start, end) => codewords.slice(start, end))
      .toVector

  private def interleave(blocks: Vector[Vector[Int]]): Vector[Int] =
    (0 until blocks.map(_.size).max)
      .flatMap(i => blocks.flatMap(_.lift(i)))
      .toVector

  /** The lowest `count` bits of a value, the highest first. */
  private def bitsOf(value: Int, count: Int): Vector[Boolean] =
    (count - 1 to 0 by -1).map(Layout.bit(value, _)).toVector

  private def byteOf(bits: Seq[Boolean]): Int = bits
    .padTo(8, false)
    .foldLeft(0)((byte, bit) => byte << 1 | (if bit then 1 else 0))

  /** Escapes text for a double-quoted attribute, so it cannot add markup. */
  private def quoted(text: String): String = text.flatMap:
    case '&'       => "&amp;"
    case '<'       => "&lt;"
    case '>'       => "&gt;"
    case '"'       => "&quot;"
    case character => character.toString

  private def runs(row: Vector[Boolean]): List[(Int, Int)] = row
    .indices
    .foldRight(List.empty[(Int, Int)]): (x, found) =>
      found match
        case _ if !row(x)                              => found
        case (start, length) :: rest if start == x + 1 =>
          (x, length + 1) :: rest
        case _ => (x, 1) :: found
