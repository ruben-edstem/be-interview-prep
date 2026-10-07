package com.edstem.interviewprep.fileupload.service;

import com.edstem.interviewprep.fileupload.entity.FileType;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ThumbnailGenerator {

  public static final int MAX_SIDE_PIXELS = 200;
  public static final long MAX_SOURCE_PIXELS = 25_000_000L;

  public Optional<byte[]> create(FileType type, byte[] content) {
    if (!type.isImage()) {
      return Optional.empty();
    }
    try (ImageInputStream input =
        ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
      BufferedImage source = read(type, input);
      return Optional.of(encode(shrink(source, type), type)).filter(bytes -> bytes.length > 0);
    } catch (IOException | RuntimeException e) {
      log.warn("Could not create a {} thumbnail", type, e);
      return Optional.empty();
    }
  }

  private BufferedImage read(FileType type, ImageInputStream input) throws IOException {
    ImageReader reader = ImageIO.getImageReadersByFormatName(type.getImageFormat()).next();
    try {
      reader.setInput(input, true, true);
      long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
      if (pixels > MAX_SOURCE_PIXELS) {
        throw new IllegalArgumentException("Image has " + pixels + " pixels");
      }
      return reader.read(0);
    } finally {
      reader.dispose();
    }
  }

  private BufferedImage shrink(BufferedImage source, FileType type) {
    int width = source.getWidth();
    int height = source.getHeight();
    double scale = Math.min(1.0, (double) MAX_SIDE_PIXELS / Math.max(width, height));
    int targetWidth = Math.max(1, (int) Math.round(width * scale));
    int targetHeight = Math.max(1, (int) Math.round(height * scale));
    int imageType = type == FileType.PNG ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

    BufferedImage current = source;
    while (current.getWidth() / 2 >= targetWidth && current.getHeight() / 2 >= targetHeight) {
      current = resize(current, current.getWidth() / 2, current.getHeight() / 2, imageType);
    }
    return resize(current, targetWidth, targetHeight, imageType);
  }

  private BufferedImage resize(BufferedImage source, int width, int height, int imageType) {
    BufferedImage target = new BufferedImage(width, height, imageType);
    var graphics = target.createGraphics();
    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.drawImage(source, 0, 0, width, height, null);
    } finally {
      graphics.dispose();
    }
    return target;
  }

  private byte[] encode(BufferedImage image, FileType type) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, type.getImageFormat(), output);
    return output.toByteArray();
  }
}
