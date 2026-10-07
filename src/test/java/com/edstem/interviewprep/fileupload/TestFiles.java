package com.edstem.interviewprep.fileupload;

import java.util.Arrays;

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
}
