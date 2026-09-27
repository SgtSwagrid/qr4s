package com.alecdorrington.qr4s

import com.google.zxing.{
  BinaryBitmap, DecodeHintType, EncodeHintType, Result, ResultMetadataType,
  RGBLuminanceSource,
}
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import munit.FunSuite
import scala.jdk.CollectionConverters.*
import scala.util.{Random, Try}

/**
  * Tests of QR codes against ZXing, an encoder and reader of them written apart
  * from ours: that each code is the one ZXing makes of the same text, module
  * for module, and that ZXing reads it back from an image.
  */
class ZxingSuite extends FunSuite:

  /** What is read from an image of the given code, if it is found at all. */
  private def read
    (
      code: QrCode,
      scale: Int,
      hints: Map[DecodeHintType, Any] = Map.empty,
    )
    : Option[Result] =
    val margin = QrCode.quietZone
    val width  = (code.size + 2 * margin) * scale
    val pixels = Array.tabulate(width * width): i =>
      val (x, y) = (i % width / scale - margin, i / width / scale - margin)
      val inside = x >= 0 && y >= 0 && x < code.size && y < code.size
      if inside && code.dark(x, y) then 0xFF000000 else 0xFFFFFFFF
    val image =
      BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, width, pixels)))
    Try(QRCodeReader().decode(image, hints.asJava)).toOption

  /** The text read from an image of the given code, if it is found at all. */
  private def scanned
    (
      code: QrCode,
      scale: Int,
      hints: Map[DecodeHintType, Any] = Map.empty,
    )
    : Option[String] = read(code, scale, hints).map(_.getText)

  /**
    * The longest text of lower-case letters that fits the given code, all of
    * which ZXing too puts in byte mode.
    */
  private def longest(version: Int, correction: Correction): String =
    val count = if version < 10 then 8 else 16
    val bytes = (8 * correction.dataCodewords(version) - 4 - count) / 8
    Random(version).alphanumeric.filter(_.isLower).take(bytes).mkString

  /** Every version of code. */
  private val versions = (1 to 40).toList

  /** The hint that an image holds nothing but the code, as ours do. */
  private val pure = Map(DecodeHintType.PURE_BARCODE -> true)

  /** Whether the given code is the one ZXing makes of its text. */
  private def matches
    (
      code: QrCode,
      text: String,
      version: Int,
      correction: Correction,
      mask: Int,
    )
    : Boolean =
    val hints = Map(
      EncodeHintType.QR_VERSION      -> version,
      EncodeHintType.QR_MASK_PATTERN -> mask,
    )
    val level  = ErrorCorrectionLevel.valueOf(correction.toString.take(1))
    val theirs = Encoder.encode(text, level, hints.asJava).getMatrix
    code.size == theirs.getWidth && (0 until code.size).forall(y =>
      (0 until code.size).forall(x =>
        code.dark(x, y) == (theirs.get(x, y) == 1),
      ),
    )

  // Each level is a test of its own: all four together can take CI longer than
  // the thirty seconds munit allows a test.
  Correction
    .values
    .foreach: correction =>
      test(
        s"every version and mask at level $correction is the code ZXing makes",
      ):
        val different =
          for
            version <- versions
            text = longest(version, correction)
            (code, mask) <- QrCode.candidates(text, correction).get.zipWithIndex
            if !matches(code, text, version, correction, mask)
          yield s"version $version, mask $mask"
        assertEquals(different, Nil)

  Correction
    .values
    .foreach: correction =>
      test(s"every version at level $correction is read back as its text"):
        val unread =
          for
            version <- versions
            text = longest(version, correction)
            code = QrCode.of(text, correction)
            if code.map(_.size) != Some(4 * version + 17) ||
            code.flatMap(scanned(_, 4, pure)) != Some(text)
          yield s"version $version"
        assertEquals(unread, Nil)

  test("a code takes the strongest level of correction that fits its size"):
    def level(text: String, correction: Correction): Option[AnyRef] = read(
      QrCode.of(text, correction).get,
      4,
      pure,
    ).map(_.getResultMetadata.get(ResultMetadataType.ERROR_CORRECTION_LEVEL))
    // Three letters fit version 1 at every level, so at the strongest.
    assertEquals(level("abc", Correction.Low), Some("H"))
    // Version 1 holds at most 14 letters at level M, and 11 at level Q.
    assertEquals(
      level("a" * 14, Correction.Low),
      Some("M"),
    )
    assertEquals(
      level("a" * 14, Correction.Medium),
      Some("M"),
    )

  test("a link is found and read back in an image at any scale"):
    val link = "https://example.com/rlk46"
    (2 to 10).foreach(scale =>
      assertEquals(
        QrCode.of(link).flatMap(scanned(_, scale)),
        Some(link),
      ),
    )
