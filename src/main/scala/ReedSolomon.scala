package com.alecdorrington.qr4s

/**
  * Reed-Solomon error correction as QR codes use it. Its field is GF(2⁸): the
  * bytes, multiplied as polynomials over GF(2) modulo x⁸+x⁴+x³+x²+1. A block's
  * error-correcting codewords are the remainder of its data, read as a
  * polynomial, divided by a generator polynomial whose roots are the first
  * powers of `2`.
  */
private[qr4s] object ReedSolomon:

  /** The polynomial the field's products are reduced by. */
  private val modulus = 0x11D

  /**
    * The error-correcting codewords of a block of data.
    *
    * @param data
    *   The data codewords, each a byte from `0` to `255`.
    *
    * @param degree
    *   How many error-correcting codewords to make.
    */
  def remainder(data: Seq[Int], degree: Int): Vector[Int] =
    val divisor = generator(degree)
    data.foldLeft(Vector.fill(degree)(0)): (rest, codeword) =>
      val factor = codeword ^ rest.head
      (rest.tail :+ 0).zip(divisor).map((r, d) => r ^ times(d, factor))

  /**
    * The generator polynomial of the given degree, (x - 2⁰)(x - 2¹)..., highest
    * powers first, without its leading coefficient, which is always `1`.
    */
  private def generator(degree: Int): Vector[Int] =
    val start             = (Vector.fill(degree - 1)(0) :+ 1, 1)
    val (coefficients, _) = (0 until degree).foldLeft(start):
      case ((product, root), _) => (timesRoot(product, root), times(root, 2))
    coefficients

  /**
    * The given polynomial multiplied by (x - root). A polynomial is held as its
    * lowest coefficients, as many as the generator has, so that a leading `1`
    * falls off the front once the product reaches the generator's degree.
    */
  private def timesRoot(polynomial: Vector[Int], root: Int): Vector[Int] =
    polynomial
      .indices
      .map(i =>
        times(polynomial(i), root) ^ polynomial.applyOrElse(i + 1, _ => 0),
      )
      .toVector

  /** The product of two elements of the field. */
  def times(x: Int, y: Int): Int = (7 to 0 by -1).foldLeft(0): (product, bit) =>
    (product << 1) ^ ((product >>> 7) * modulus) ^ (((y >>> bit) & 1) * x)
