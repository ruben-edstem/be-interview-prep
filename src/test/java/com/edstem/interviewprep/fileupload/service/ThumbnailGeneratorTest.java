package com.edstem.interviewprep.fileupload.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edstem.interviewprep.fileupload.TestFiles;
import com.edstem.interviewprep.fileupload.entity.FileType;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;

class ThumbnailGeneratorTest {

  private final ThumbnailGenerator generator = new ThumbnailGenerator();

  @Test
  void pngIsShrunkToTheMaximumSideKeepingProportions() {
    byte[] png = TestFiles.image("png", 800, 400);

    Optional<byte[]> thumbnail = generator.create(FileType.PNG, png);

    BufferedImage image = TestFiles.decode(thumbnail.orElseThrow());
    assertEquals(200, image.getWidth());
    assertEquals(100, image.getHeight());
  }

  @Test
  void jpegIsShrunkToTheMaximumSideKeepingProportions() {
    byte[] jpeg = TestFiles.image("jpg", 300, 600);

    Optional<byte[]> thumbnail = generator.create(FileType.JPEG, jpeg);

    BufferedImage image = TestFiles.decode(thumbnail.orElseThrow());
    assertEquals(100, image.getWidth());
    assertEquals(200, image.getHeight());
  }

  @Test
  void thumbnailKeepsTheSourceFormat() {
    Optional<byte[]> png = generator.create(FileType.PNG, TestFiles.image("png", 400, 400));
    Optional<byte[]> jpeg = generator.create(FileType.JPEG, TestFiles.image("jpg", 400, 400));

    assertEquals(FileType.PNG, FileType.detect(header(png.orElseThrow())).orElseThrow());
    assertEquals(FileType.JPEG, FileType.detect(header(jpeg.orElseThrow())).orElseThrow());
  }

  @Test
  void smallImageIsNotEnlarged() {
    byte[] png = TestFiles.image("png", 50, 40);

    Optional<byte[]> thumbnail = generator.create(FileType.PNG, png);

    BufferedImage image = TestFiles.decode(thumbnail.orElseThrow());
    assertEquals(50, image.getWidth());
    assertEquals(40, image.getHeight());
  }

  @Test
  void veryNarrowImageKeepsAtLeastOnePixel() {
    byte[] png = TestFiles.image("png", 1000, 2);

    Optional<byte[]> thumbnail = generator.create(FileType.PNG, png);

    BufferedImage image = TestFiles.decode(thumbnail.orElseThrow());
    assertEquals(200, image.getWidth());
    assertTrue(image.getHeight() >= 1);
  }

  @Test
  void pdfGetsNoThumbnail() {
    Optional<byte[]> thumbnail = generator.create(FileType.PDF, TestFiles.PDF);

    assertTrue(thumbnail.isEmpty());
  }

  @Test
  void corruptImageGetsNoThumbnail() {
    Optional<byte[]> thumbnail = generator.create(FileType.PNG, TestFiles.PNG);

    assertTrue(thumbnail.isEmpty());
  }

  @Test
  void imageWithAnAbsurdPixelCountGetsNoThumbnail() {
    byte[] hugeHeader = pngHeaderDeclaring(100_000, 100_000);

    Optional<byte[]> thumbnail = generator.create(FileType.PNG, hugeHeader);

    assertTrue(thumbnail.isEmpty());
  }

  private byte[] header(byte[] content) {
    return java.util.Arrays.copyOf(content, FileType.MAX_SIGNATURE_LENGTH);
  }

  private byte[] pngHeaderDeclaring(int width, int height) {
    ByteBuffer data = ByteBuffer.allocate(13);
    data.putInt(width)
        .putInt(height)
        .put((byte) 8)
        .put((byte) 6)
        .put((byte) 0)
        .put((byte) 0)
        .put((byte) 0);
    byte[] ihdr = data.array();
    CRC32 crc = new CRC32();
    crc.update("IHDR".getBytes());
    crc.update(ihdr);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
    output.writeBytes(ByteBuffer.allocate(4).putInt(13).array());
    output.writeBytes("IHDR".getBytes());
    output.writeBytes(ihdr);
    output.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    return output.toByteArray();
  }
}
