package com.alecdorrington.qr4s

/**
  * The eight masks that may flip a QR code's data modules, and the penalty the
  * standard chooses between them by: the fewer features the code has that
  * confuse a scanner, such as long runs of one colour or shapes like its finder
  * patterns, the lower.
  */
private[qr4s] object Masking:

  /**
    * Whether each mask, numbered from `0` as the format information names it,
    * flips the module in the given column and row.
    */
  // Kept from aligning the `%` of each line, as it would a dependency's.
  // format: off
  val masks: Vector[(Int, Int) => Boolean] = Vector(
    (x, y) => (x + y) % 2 == 0,
    (_, y) => y % 2 == 0,
    (x, _) => x % 3 == 0,
    (x, y) => (x + y) % 3 == 0,
    (x, y) => (x / 3 + y / 2) % 2 == 0,
    (x, y) => x * y % 2 + x * y % 3 == 0,
    (x, y) => (x * y % 2 + x * y % 3) % 2 == 0,
    (x, y) => ((x + y) % 2 + x * y % 3) % 2 == 0,
  )
  // format: on

  /** How hard the given modules are to read: the lower, the easier. */
  def penalty(modules: Vector[Vector[Boolean]]): Int =
    (modules ++ modules.transpose)
      .map(line => runs(line) + lookalikes(line))
      .sum + blocks(modules) + imbalance(modules)

  /**
    * Three for each run of five or more modules of one colour in a line, and
    * one more for each module past the fifth.
    */
  private def runs(line: Vector[Boolean]): Int = lengths(line)
    .filter(_ >= 5)
    .map(_ - 2)
    .sum

  /** The lengths of the runs of one colour a line is made of. */
  private def lengths(line: Vector[Boolean]): List[Int] = line
    .foldLeft(List.empty[(Boolean, Int)]):
      case ((colour, length) :: rest, module) if module == colour =>
        (colour, length + 1) :: rest
      case (runs, module) => (module, 1) :: runs
    .map((_, length) => length)

  /** Three for each square of four modules of one colour. */
  private def blocks(modules: Vector[Vector[Boolean]]): Int =
    val squares =
      for
        y <- 0 until modules.size - 1
        x <- 0 until modules.size - 1
      yield Set(
        modules(y)(x),
        modules(y)(x + 1),
        modules(y + 1)(x),
        modules(y + 1)(x + 1),
      )
    3 * squares.count(_.size == 1)

  /**
    * Forty for each look-alike of a finder pattern in a line: dark, light,
    * three dark, light and dark, with four light modules on either side, where
    * the quiet zone beyond the edges counts as light.
    */
  private def lookalikes(line: Vector[Boolean]): Int =
    val margin = Vector.fill(4)(false)
    40 * (margin ++ line ++ margin).sliding(11).count(finderLike.contains)

  /** The look-alikes of a finder pattern, with their light side each way. */
  private val finderLike: Set[Vector[Boolean]] =
    Set("10111010000", "00001011101").map(_.map(_ == '1').toVector)

  /**
    * Ten for each five percent by which the share of dark modules strays from
    * half, beyond the first five.
    */
  private def imbalance(modules: Vector[Vector[Boolean]]): Int =
    val total = modules.size * modules.size
    val dark  = modules.map(_.count(identity)).sum
    10 * math.max(
      0,
      ((dark * 20 - total * 10).abs + total - 1) / total - 1,
    )
