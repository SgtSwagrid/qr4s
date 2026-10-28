package com.alecdorrington.qr4s

/**
  * A level of error correction: how much of a [[QrCode]] may be lost with it
  * still being read, paid for in room for data. The levels run from the weakest
  * to the strongest.
  *
  * @param bits
  *   The two bits naming the level in a code's format information.
  *
  * @param checksByVersion
  *   The number of error-correcting codewords in each block, by version from
  *   `1`.
  *
  * @param blocksByVersion
  *   The number of blocks the codewords are split into, by version from `1`.
  */
enum Correction
  (
    val bits: Int,
    checksByVersion: Vector[Int],
    blocksByVersion: Vector[Int],
  ):

  /**
    * Counts the error-correcting codewords in each block of a code.
    *
    * @param version
    *   The code's version, from `1` to `40`.
    *
    * @return
    *   A number of codewords.
    */
  def checkCodewords(version: Int): Int = checksByVersion(version - 1)

  /**
    * Counts the blocks a code's codewords are split into.
    *
    * @param version
    *   The code's version, from `1` to `40`.
    *
    * @return
    *   A number of blocks.
    */
  def blocks(version: Int): Int = blocksByVersion(version - 1)

  /**
    * Counts the codewords a code has left for data.
    *
    * @param version
    *   The code's version, from `1` to `40`.
    *
    * @return
    *   A number of codewords.
    */
  def dataCodewords(version: Int): Int = Layout.codewords(version) -
    checkCodewords(version) * blocks(version)

  // The tables are ten versions to a row.
  // format: off

  /** About 7% may be lost. */
  case Low extends Correction(1,
    Vector( 7, 10, 15, 20, 26, 18, 20, 24, 30, 18,
           20, 24, 26, 30, 22, 24, 28, 30, 28, 28,
           28, 28, 30, 30, 26, 28, 30, 30, 30, 30,
           30, 30, 30, 30, 30, 30, 30, 30, 30, 30),
    Vector( 1,  1,  1,  1,  1,  2,  2,  2,  2,  4,
            4,  4,  4,  4,  6,  6,  6,  6,  7,  8,
            8,  9,  9, 10, 12, 12, 12, 13, 14, 15,
           16, 17, 18, 19, 19, 20, 21, 22, 24, 25),
  )

  /** About 15% may be lost. */
  case Medium extends Correction(0,
    Vector(10, 16, 26, 18, 24, 16, 18, 22, 22, 26,
           30, 22, 22, 24, 24, 28, 28, 26, 26, 26,
           26, 28, 28, 28, 28, 28, 28, 28, 28, 28,
           28, 28, 28, 28, 28, 28, 28, 28, 28, 28),
    Vector( 1,  1,  1,  2,  2,  4,  4,  4,  5,  5,
            5,  8,  9,  9, 10, 10, 11, 13, 14, 16,
           17, 17, 18, 20, 21, 23, 25, 26, 28, 29,
           31, 33, 35, 37, 38, 40, 43, 45, 47, 49),
  )

  /** About 25% may be lost. */
  case Quartile extends Correction(3,
    Vector(13, 22, 18, 26, 18, 24, 18, 22, 20, 24,
           28, 26, 24, 20, 30, 24, 28, 28, 26, 30,
           28, 30, 30, 30, 30, 28, 30, 30, 30, 30,
           30, 30, 30, 30, 30, 30, 30, 30, 30, 30),
    Vector( 1,  1,  2,  2,  4,  4,  6,  6,  8,  8,
            8, 10, 12, 16, 12, 17, 16, 18, 21, 20,
           23, 23, 25, 27, 29, 34, 34, 35, 38, 40,
           43, 45, 48, 51, 53, 56, 59, 62, 65, 68),
  )

  /** About 30% may be lost. */
  case High extends Correction(2,
    Vector(17, 28, 22, 16, 22, 28, 26, 26, 24, 28,
           24, 28, 22, 24, 24, 30, 28, 28, 26, 28,
           30, 24, 30, 30, 30, 30, 30, 30, 30, 30,
           30, 30, 30, 30, 30, 30, 30, 30, 30, 30),
    Vector( 1,  1,  2,  4,  4,  4,  5,  6,  8,  8,
           11, 11, 16, 16, 18, 16, 19, 21, 25, 25,
           25, 34, 30, 32, 35, 37, 40, 42, 45, 48,
           51, 54, 57, 60, 63, 66, 70, 74, 77, 81),
  )

  // format: on
