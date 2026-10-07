package com.edstem.interviewprep.fileupload;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

public final class TestFiles {

  public static final byte[] PNG =
      withHeader(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
  public static final byte[] JPEG =
      withHeader(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
  public static final byte[] PDF = withHeader("%PDF-1.7".getBytes());
  public static final byte[] EXECUTABLE = withHeader("MZ".getBytes());

  private TestFiles() {}

  public static byte[] withHeader(byte[] header) {
    byte[] content = Arrays.copyOf(header, 64);
    Arrays.fill(content, header.length, content.length, (byte) 1);
    return content;
  }

  public static byte[] sized(byte[] header, int size) {
    byte[] content = new byte[size];
    System.arraycopy(header, 0, content, 0, header.length);
    return content;
  }

  public static byte[] image(String format, int width, int height) {
    int imageType = format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
    BufferedImage image = new BufferedImage(width, height, imageType);
    var graphics = image.createGraphics();
    graphics.setColor(Color.RED);
    graphics.fillRect(0, 0, width, height);
    graphics.setColor(Color.BLUE);
    graphics.fillRect(0, 0, width / 2, height / 2);
    graphics.dispose();
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      ImageIO.write(image, format, output);
      return output.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static BufferedImage decode(byte[] content) {
    try {
      return ImageIO.read(new java.io.ByteArrayInputStream(content));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
