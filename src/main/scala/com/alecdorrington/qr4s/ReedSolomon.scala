package com.alecdorrington.qr4s

/**
  * [Reed-Solomon](https://en.wikipedia.org/wiki/Reed%E2%80%93Solomon_error_correction)
  * error correction over `GF(2^8) modulo x⁸+x⁴+x³+x²+1`.
  */
private[qr4s] object ReedSolomon:

  private val modulus = 0x11D

  /** Creates a block's error-correcting codewords. */
  def remainder(degree: Int): Seq[Int] => Vector[Int] =
    val divisor = generatorPolynomial(degree)
    data =>
      data.foldLeft(Vector.fill(degree)(0)): (rest, codeword) =>
        val factor = codeword ^ rest.head
        (rest.tail :+ 0).zip(divisor).map((r, d) => r ^ gfMultiply(d, factor))

  /**
    * The coefficients of the generator polynomial `(x - 2⁰)(x - 2¹)...`, with
    *   - the highest powers first, and
    *   - the leading coefficient omitted, as it is always `1`.
    */
  private def generatorPolynomial(degree: Int): Vector[Int] =
    val start             = (Vector.fill(degree - 1)(0) :+ 1, 1)
    val (coefficients, _) = (0 until degree).foldLeft(start):
      case ((product, root), _) =>
        (multiplyByRoot(product, root), gfMultiply(root, 2))
    coefficients

  /** Multiplies a polynomial by `(x - root)`. */
  private def multiplyByRoot(polynomial: Vector[Int], root: Int): Vector[Int] =
    polynomial
      .indices
      .map(i =>
        gfMultiply(polynomial(i), root) ^ polynomial.applyOrElse(i + 1, _ => 0),
      )
      .toVector

  private def gfMultiply(x: Int, y: Int): Int = (7 to 0 by -1).foldLeft(0):
    (product, bit) =>
      (product << 1) ^ ((product >>> 7) * modulus) ^ (((y >>> bit) & 1) * x)
