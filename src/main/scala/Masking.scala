package com.alecdorrington.qr4s

/** The eight data masks, and the penalty that chooses between them. */
private[qr4s] object Masking:

  /** Whether each mask flips a module, in format information order. */
  // Stops scalafmt aligning each `%` as it would a dependency's.
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

  /** How hard the modules are to read: the lower, the easier. */
  def penalty(modules: Vector[Vector[Boolean]]): Int =
    (modules ++ modules.transpose)
      .map(line => runs(line) + lookalikes(line))
      .sum + squares(modules) + imbalance(modules)

  /** Three per run of five or more of one colour, plus one per module beyond. */
  private def runs(line: Vector[Boolean]): Int = lengths(line)
    .filter(_ >= 5)
    .map(_ - 2)
    .sum

  private def lengths(line: Vector[Boolean]): List[Int] = line
    .foldLeft(List.empty[(Boolean, Int)]):
      case ((colour, length) :: rest, module) if module == colour =>
        (colour, length + 1) :: rest
      case (runs, module) => (module, 1) :: runs
    .map((_, length) => length)

  /** Three per square of two by two modules all of one colour. */
  private def squares(modules: Vector[Vector[Boolean]]): Int =
    val colours =
      for
        y <- 0 until modules.size - 1
        x <- 0 until modules.size - 1
      yield Set(
        modules(y)(x),
        modules(y)(x + 1),
        modules(y + 1)(x),
        modules(y + 1)(x + 1),
      )
    3 * colours.count(_.size == 1)

  /** Forty per finder look-alike in a line, the quiet zone counting as light. */
  private def lookalikes(line: Vector[Boolean]): Int =
    val margin = Vector.fill(4)(false)
    40 * (margin ++ line ++ margin).sliding(11).count(finderLike.contains)

  private val finderLike: Set[Vector[Boolean]] =
    Set("10111010000", "00001011101").map(_.map(_ == '1').toVector)

  /** Ten per five percent the dark share strays from half, beyond the first. */
  private def imbalance(modules: Vector[Vector[Boolean]]): Int =
    val total = modules.size * modules.size
    val dark  = modules.map(_.count(identity)).sum
    10 * math.max(
      0,
      ((dark * 20 - total * 10).abs + total - 1) / total - 1,
    )
