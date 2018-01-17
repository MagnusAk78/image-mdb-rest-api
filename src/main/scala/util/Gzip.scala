package util

import java.io.{ ByteArrayOutputStream, ByteArrayInputStream }
import java.util.zip.{ GZIPOutputStream, GZIPInputStream }
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.nio.charset.StandardCharsets
import scala.util.Try

object Gzip {

  def compress(input: String): String = {
    val compressedBytes = compressBytes(input.getBytes)
    Base64.getEncoder.encodeToString(compressedBytes)
  }

  def decompress(compressed: String): String = decompressBytes(Base64.getDecoder.decode(compressed))

  private def compressBytes(input: Array[Byte]): Array[Byte] = {
    val bos = new ByteArrayOutputStream(input.length)
    val gzip = new GZIPOutputStream(bos)
    gzip.write(input)
    gzip.close()
    bos.toByteArray
  }

  private def decompressBytes(compressed: Array[Byte]): String = {
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(compressed))
    scala.io.Source.fromInputStream(inputStream).mkString
  }
}
