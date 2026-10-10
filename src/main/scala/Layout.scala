package com.alecdorrington.qr4s

/** A module's column, then its row, from the top left. */
private[qr4s] type Position = (Int, Int)

/** Where the patterns, format information and data sit in a [[QrCode]]. */
private[qr4s] object Layout:

  def size(version: Int): Int = 4 * version + 17

  /** The codewords a code holds, data and error correction together. */
  def codewords(version: Int): Int = dataModules(version) / 8

  /**
    * All modules but the finders and their separators, the timing patterns, the
    * format information and dark module, the alignment patterns (less where
    * they cross the timing patterns) and from version `7` the version
    * information.
    */
  private def dataModules(version: Int): Int =
    val width      = size(version)
    val alignments = centres(version).size
    val aligning   =
      if alignments == 0 then 0
      else 25 * (alignments * alignments - 3) - 10 * (alignments - 2)
    val versioning = if version >= 7 then 36 else 0
    width * width - 3 * 64 - 2 * (width - 16) - 31 - aligning - versioning

  /** Every fixed module but the format information, which varies by mask. */
  def patterns(version: Int): Map[Position, Boolean] =
    val width = size(version)
    timing(width) ++ finders(width) ++ alignments(version) ++
      versionInformation(version) + ((8, width - 8) -> true)

  private def timing(width: Int): Map[Position, Boolean] = (0 until width)
    .flatMap(i => Seq((6, i), (i, 6)).map(_ -> (i % 2 == 0)))
    .toMap

  private def finders(width: Int): Map[Position, Boolean] =
    Seq((3, 3), (width - 4, 3), (3, width - 4))
      .flatMap(square(_, 4, Set(0, 1, 3)))
      .filter { case ((x, y), _) => x >= 0 && x < width && y >= 0 && y < width }
      .toMap

  private def alignments(version: Int): Map[Position, Boolean] =
    val grid    = centres(version)
    val corners = grid
      .headOption
      .toSeq
      .flatMap(first =>
        Seq(
          (first, first),
          (first, grid.last),
          (grid.last, first),
        ),
      )
    grid
      .flatMap(x => grid.map(y => (x, y)))
      .filterNot(corners.contains)
      .flatMap(square(_, 2, Set(0, 2)))
      .toMap

  /**
    * The rows (and columns) of the alignment patterns' centres: none in version
    * `1`, else row `6` and others evenly spaced up to seven from the far edge.
    */
  private def centres(version: Int): Vector[Int] =
    if version == 1 then Vector.empty
    else
      val count = version / 7 + 2
      val step  = (version * 8 + count * 3 + 5) / (count * 4 - 4) * 2
      6 +: (count - 2 to 0 by -1).map(size(version) - 7 - _ * step).toVector

  /** Nested square rings around a centre, dark at the given distances. */
  private def square
    (
      centre: Position,
      radius: Int,
      darkRings: Set[Int],
    )
    : Seq[(Position, Boolean)] =
    val (x, y) = centre
    for
      dy <- -radius to radius
      dx <- -radius to radius
    yield (x + dx, y + dy) -> darkRings(math.max(dx.abs, dy.abs))

  private def versionInformation(version: Int): Map[Position, Boolean] =
    if version < 7 then Map.empty
    else
      val far  = size(version) - 11
      val bits = versionBits(version)
      (0 until 18)
        .flatMap: i =>
          val (a, b) = (far + i % 3, i / 3)
          Seq((a, b), (b, a)).map(_ -> bit(bits, i))
        .toMap

  /** The 18 bits of version information: the version, then its check bits. */
  def versionBits(version: Int): Int = version << 12 |
    remainder(version, 12, 0x1F25)

  def formatInformation
    (
      version: Int,
      correction: Correction,
      mask: Int,
    )
    : Map[Position, Boolean] =
    val bits = formatBits(correction, mask)
    (0 until 15)
      .flatMap(i => formatPositions(size(version), i).map(_ -> bit(bits, i)))
      .toMap

  def formatArea(version: Int): Set[Position] = (0 until 15)
    .flatMap(formatPositions(size(version), _))
    .toSet

  /**
    * The 15 bits of format information: the level and mask, then their check
    * bits, all masked so as never to be all light.
    */
  def formatBits(correction: Correction, mask: Int): Int =
    val data = correction.bits << 3 | mask
    (data << 10 | remainder(data, 10, 0x537)) ^ 0x5412

  private def formatPositions(width: Int, i: Int): Seq[Position] =
    val around = i match
      case _ if i < 6 => (8, i)
      case 6 | 7      => (8, i + 1)
      case 8          => (7, 8)
      case _          => (14 - i, 8)
    val split = if i < 8 then (width - 1 - i, 8) else (8, width - 15 + i)
    Seq(around, split)

  /** The BCH check bits: the data shifted by `degree`, mod the generator. */
  private def remainder(data: Int, degree: Int, generator: Int): Int =
    (0 until degree).foldLeft(data)((r, _) =>
      (r << 1) ^ ((r >>> (degree - 1)) * generator),
    )

  /**
    * The modules the data fills, in order: two columns at a time from the
    * right, alternately up and down, skipping every reserved module.
    */
  def dataOrder(version: Int, reserved: Set[Position]): Vector[Position] =
    val width = size(version)
    val order =
      for
        (right, pair) <- columns(width).zipWithIndex
        down          <- 0 until width
        x             <- Seq(right, right - 1)
      yield (x, if pair % 2 == 0 then width - 1 - down else down)
    order.filterNot(reserved)

  /** The right column of each pair, skipping the timing pattern's column. */
  private def columns(width: Int): Vector[Int] = (width - 1 to 1 by -2)
    .map(right => if right <= 6 then right - 1 else right)
    .toVector

  def bit(value: Int, index: Int): Boolean = ((value >>> index) & 1) == 1
