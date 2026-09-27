package com.alecdorrington.qr4s

import java.nio.charset.StandardCharsets

/**
  * A QR code: a square of modules, dark or light, which a camera reads back as
  * the text it encodes. Scanners need a light margin around it, its
  * [[QrCode.quietZone]], which is not part of it.
  *
  * @param modules
  *   Whether each module is dark, row by row from the top, each from the left.
  */
final case class QrCode(modules: Vector[Vector[Boolean]]):

  /** The width and height of the code in modules. */
  def size: Int = modules.size

  /** Whether the module in the given column and row is dark. */
  def dark(x: Int, y: Int): Boolean = modules(y)(x)

  /**
    * The dark modules as the data of one SVG path, one unit to a module with
    * the code's top left corner at the origin: a rectangle for each run of them
    * along a row. Fill it dark, over a light background reaching the
    * [[QrCode.quietZone]] beyond the code on every side.
    */
  def path: String = modules
    .zipWithIndex
    .flatMap((row, y) =>
      QrCode
        .runs(row)
        .map((x, length) => s"M$x ${ y }h${ length }v1h-${ length }z"),
    )
    .mkString

  /**
    * The code as an SVG image of its own, in its quiet zone. It is dark on
    * light whatever surrounds it, as not every scanner reads a code inverted,
    * and it scales to whatever box it is given.
    */
  def svg: String =
    val margin = QrCode.quietZone
    val span   = size + 2 * margin
    val box    = s"-$margin -$margin $span $span"
    s"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="$box" shape-rendering="crispEdges">""" +
      s"""<rect x="-$margin" y="-$margin" width="$span" height="$span" fill="#FFFFFF"/>""" +
      s"""<path d="$path" fill="#000000"/></svg>"""

/**
  * Encodes text as a [[QrCode]], as its standard (ISO/IEC 18004) describes, in
  * byte mode, which holds any text.
  */
object QrCode:

  /** The width in modules of the light margin scanners need around a code. */
  val quietZone: Int = 4

  /** The versions of code there are, each four modules wider than the last. */
  private val versions = 1 to 40

  /** The four bits announcing data in byte mode. */
  private val byteMode = 0x4

  /** The codewords that fill a code's room for data beyond its text, in turn. */
  private val padding = Vector(0xEC, 0x11)

  /**
    * The smallest code of the given text, as UTF-8, with whichever mask makes
    * it easiest to read.
    *
    * @param text
    *   The text to encode, e.g. a web address.
    *
    * @param correction
    *   How much of the code, at least, may be lost with it still being read.
    *   Where a stronger level fits a code of the same size, it is used instead.
    *
    * @return
    *   A code, unless the text is too long for any.
    */
  def of
    (
      text: String,
      correction: Correction = Correction.Medium,
    )
    : Option[QrCode] = candidates(text, correction).map(_.minBy(code =>
    Masking.penalty(code.modules),
  ))

  /**
    * The smallest code of the given text, at the strongest level of correction
    * from the given one that fits it, under each mask in turn, unless the text
    * is too long for any.
    */
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

  /** Whether the given number of bytes fits a code of the given version. */
  private def fits
    (
      bytes: Int,
      version: Int,
      correction: Correction,
    )
    : Boolean = 4 + countBits(version) + 8 * bytes <=
    8 * correction.dataCodewords(version)

  /** The bits the number of bytes takes in a code of the given version. */
  private def countBits(version: Int): Int = if version < 10 then 8 else 16

  /** The given bytes as a code of the given version under each mask. */
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
          patterns ++ Layout.format(version, correction, mask) ++
            flipped(unmasked, mask),
        ),
      )

  /**
    * Each module the given bytes fill in a code of the given version, with
    * whether it is dark before masking, in all the modules left by the given
    * patterns and the format information.
    */
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

  /** The given modules, each flipped where the given mask says. */
  private def flipped
    (
      modules: Vector[(Position, Boolean)],
      mask: Int,
    )
    : Vector[(Position, Boolean)] = modules.map { case ((x, y), dark) =>
    (x, y) -> (dark ^ Masking.masks(mask)(x, y))
  }

  /** The code of the given version whose every module is as given. */
  private def drawn
    (
      version: Int,
      modules: Map[Position, Boolean],
    )
    : QrCode =
    val size = Layout.size(version)
    QrCode(Vector.tabulate(size, size)((y, x) => modules((x, y))))

  /**
    * The data codewords of a code: byte mode, how many bytes there are, the
    * bytes, a terminator of up to four zero bits, then padding, filling the
    * code's room for data exactly.
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
      bitsOf(bytes.size, countBits(version)) ++ bytes.flatMap(bitsOf(_, 8))
    val ended = content ++
      Vector.fill(math.min(4, 8 * room - content.size))(false)
    val written = ended.grouped(8).map(byteOf).toVector
    written ++
      Vector.tabulate(room - written.size)(i => padding(i % padding.size))

  /**
    * The data codewords split into blocks, each given the codewords that
    * correct its errors, and interleaved: the first data codeword of every
    * block, then the second, and so on, then likewise the error correction.
    */
  private def interleaved
    (
      data: Vector[Int],
      version: Int,
      correction: Correction,
    )
    : Vector[Int] =
    val blocks = split(data, correction.blocksAt(version))
    val checks =
      blocks.map(ReedSolomon.remainder(_, correction.correctingAt(version)))
    interleave(blocks) ++ interleave(checks)

  /**
    * The given codewords split into the given number of blocks, the shorter
    * first, with the longer each one codeword longer.
    */
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

  /** The first codeword of every block, then the second, and so on. */
  private def interleave(blocks: Vector[Vector[Int]]): Vector[Int] =
    (0 until blocks.map(_.size).max)
      .flatMap(i => blocks.flatMap(_.lift(i)))
      .toVector

  /** The given number of the lowest bits of a value, the highest first. */
  private def bitsOf(value: Int, count: Int): Vector[Boolean] =
    (count - 1 to 0 by -1).map(Layout.bit(value, _)).toVector

  /** Up to eight bits as a byte, the first highest, padded with zeros. */
  private def byteOf(bits: Seq[Boolean]): Int = bits
    .padTo(8, false)
    .foldLeft(0)((byte, bit) => byte << 1 | (if bit then 1 else 0))

  /** Where each run of dark modules in a row starts, and its length. */
  private def runs(row: Vector[Boolean]): List[(Int, Int)] = row
    .indices
    .foldRight(List.empty[(Int, Int)]): (x, found) =>
      found match
        case _ if !row(x)                              => found
        case (start, length) :: rest if start == x + 1 =>
          (x, length + 1) :: rest
        case _ => (x, 1) :: found
