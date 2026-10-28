package com.alecdorrington.qr4s

/**
  * Reed-Solomon error correction over GF(2⁸) modulo x⁸+x⁴+x³+x²+1, as QR codes
  * use it.
  */
private[qr4s] object ReedSolomon:

  private val modulus = 0x11D

  /**
    * Makes a block's `degree` error-correcting codewords, deriving the
    * generator once.
    */
  def remainder(degree: Int): Seq[Int] => Vector[Int] =
    val divisor = generator(degree)
    data =>
      data.foldLeft(Vector.fill(degree)(0)): (rest, codeword) =>
        val factor = codeword ^ rest.head
        (rest.tail :+ 0).zip(divisor).map((r, d) => r ^ times(d, factor))

  /**
    * The generator polynomial (x - 2⁰)(x - 2¹)..., highest powers first, less
    * its leading coefficient, which is always `1`.
    */
  private def generator(degree: Int): Vector[Int] =
    val start             = (Vector.fill(degree - 1)(0) :+ 1, 1)
    val (coefficients, _) = (0 until degree).foldLeft(start):
      case ((product, root), _) => (timesRoot(product, root), times(root, 2))
    coefficients

  /**
    * Multiplies a polynomial by (x - root), keeping only as many of the lowest
    * coefficients as the generator has, so the leading `1` falls off.
    */
  private def timesRoot(polynomial: Vector[Int], root: Int): Vector[Int] =
    polynomial
      .indices
      .map(i =>
        times(polynomial(i), root) ^ polynomial.applyOrElse(i + 1, _ => 0),
      )
      .toVector

  def times(x: Int, y: Int): Int = (7 to 0 by -1).foldLeft(0): (product, bit) =>
    (product << 1) ^ ((product >>> 7) * modulus) ^ (((y >>> bit) & 1) * x)
