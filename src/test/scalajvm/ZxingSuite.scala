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

class ZxingSuite extends FunSuite:

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

  private def scanned
    (
      code: QrCode,
      scale: Int,
      hints: Map[DecodeHintType, Any] = Map.empty,
    )
    : Option[String] = read(code, scale, hints).map(_.getText)

  /** The longest lower-case text that fits, which ZXing puts in byte mode. */
  private def longest(version: Int, correction: Correction): String =
    val count = if version < 10 then 8 else 16
    val bytes = (8 * correction.dataCodewords(version) - 4 - count) / 8
    Random(version).alphanumeric.filter(_.isLower).take(bytes).mkString

  private val versions = (1 to 40).toList

  /** The versions either side of each change in a code's layout or fields. */
  private val boundaries = Set(1, 2, 7, 9, 10, 26, 27, 40)

  private val pure = Map(DecodeHintType.PURE_BARCODE -> true)

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

  // One test per level: all four together can exceed munit's 30 s limit on CI.
  Correction
    .values
    .foreach: correction =>
      test(
        s"every version and mask at level $correction is the code ZXing makes",
      ):
        val different = versions.flatMap: version =>
          val text  = longest(version, correction)
          val codes = QrCode.candidates(text, correction).get
          val masks = codes
            .indices
            .filterNot(mask =>
              matches(
                codes(mask),
                text,
                version,
                correction,
                mask,
              ),
            )
            .map(mask => s"version $version, mask $mask")
          // The code chosen is one of them, so of this version and level.
          val chosen = Option.when(
            boundaries(version) &&
            !QrCode.of(text, correction).exists(codes.contains),
          )(s"version $version, as chosen")
          masks ++ chosen
        assertEquals(different, Nil)

  test("a code is read back at any scale, at the strongest level that fits"):
    val link = "https://example.com/rlk46"
    List(2, 3, 10).foreach(scale =>
      assertEquals(
        QrCode.of(link).flatMap(scanned(_, scale)),
        Some(link),
      ),
    )
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
