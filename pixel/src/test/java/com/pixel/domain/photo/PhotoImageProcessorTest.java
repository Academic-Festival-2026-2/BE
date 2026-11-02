package com.pixel.domain.photo;

import static org.junit.jupiter.api.Assertions.*;

import com.pixel.domain.photo.util.PhotoImageProcessor;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhotoImageProcessorTest {
  private static final int[] COLORS = {0xff0000, 0x00ff00, 0x0000ff, 0xffff00};

  @ParameterizedTest
  @CsvSource({"1,0123", "2,1032", "3,3210", "4,2301", "5,0213", "6,2031",
      "7,3120", "8,1302"})
  void actualExifRotationAndMirroringStripPrivateMetadata(int orientation, String corners)
      throws Exception {
    byte[] input = exifJpeg(orientation);
    var before = ImageMetadataReader.readMetadata(new ByteArrayInputStream(input));
    assertEquals(orientation, before.getFirstDirectoryOfType(ExifIFD0Directory.class)
        .getInt(ExifIFD0Directory.TAG_ORIENTATION));
    assertNotNull(before.getFirstDirectoryOfType(GpsDirectory.class).getGeoLocation());
    var result = new PhotoImageProcessor().process(input);
    for (byte[] bytes : new byte[][] {result.originalBytes(), result.thumbnailBytes()}) {
      var after = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
      assertNull(after.getFirstDirectoryOfType(GpsDirectory.class));
      assertNull(after.getFirstDirectoryOfType(ExifIFD0Directory.class));
      var image = ImageIO.read(new ByteArrayInputStream(bytes));
      assertEquals(orientation >= 5 ? 40 : 80, image.getWidth());
      assertEquals(orientation >= 5 ? 80 : 40, image.getHeight());
      int[][] points = {{5, 5}, {image.getWidth()-6, 5}, {5, image.getHeight()-6},
          {image.getWidth()-6, image.getHeight()-6}};
      for (int i = 0; i < 4; i++) {
        int expected = COLORS[corners.charAt(i)-'0'];
        int actual = image.getRGB(points[i][0], points[i][1]);
        for (int shift : new int[] {0, 8, 16}) {
          assertTrue(Math.abs(((expected >> shift)&255)-((actual >> shift)&255)) < 40);
        }
      }
    }
  }

  @org.junit.jupiter.api.Test
  void defaultOrientationReusesDecodedImage() throws Exception {
    var source = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
    var method = PhotoImageProcessor.class.getDeclaredMethod(
        "orient", BufferedImage.class, int.class, boolean.class);
    method.setAccessible(true);
    assertSame(source, method.invoke(new PhotoImageProcessor(), source, 1, false));
  }

  @org.junit.jupiter.api.Test
  void jpegUsesExplicitHighQualityQuantization() throws Exception {
    byte[] encoded = new PhotoImageProcessor().process(exifJpeg(1)).originalBytes();
    var reader = ImageIO.getImageReadersByFormatName("jpeg").next();
    try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(encoded))) {
      reader.setInput(input);
      var tree = reader.getImageMetadata(0).getAsTree("javax_imageio_jpeg_image_1.0");
      var tables = ((org.w3c.dom.Element) tree).getElementsByTagName("dqtable");
      var table = (javax.imageio.plugins.jpeg.JPEGQTable)
          ((javax.imageio.metadata.IIOMetadataNode) tables.item(0)).getUserObject();
      // 품질 0.90의 표준 휘도 양자화 첫 값은 3이며 기본 0.75의 값 8보다 작다.
      assertEquals(3, table.getTable()[0]);
    } finally {
      reader.dispose();
    }
  }

  // APP1 EXIF 디렉터리, GPS 좌표와 기기 제조사 정보를 포함한 실제 JPEG 바이트를 생성한다.
  static byte[] exifJpeg(int orientation) throws Exception {
    BufferedImage image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < 40; y++) {
      for (int x = 0; x < 80; x++) image.setRGB(x, y, COLORS[(y / 20)*2 + x / 40]);
    }
    var jpeg = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", jpeg);
    ByteBuffer t = ByteBuffer.allocate(160).order(ByteOrder.LITTLE_ENDIAN);
    t.put((byte)'I').put((byte)'I').putShort((short)42).putInt(8);
    t.putShort((short)3);
    entry(t, 0x112, 3, 1, orientation);
    entry(t, 0x10f, 2, 8, 152);
    entry(t, 0x8825, 4, 1, 50);
    t.putInt(0).putShort((short)4);
    entry(t, 1, 2, 2, 'N');
    entry(t, 2, 5, 3, 104);
    entry(t, 3, 2, 2, 'E');
    entry(t, 4, 5, 3, 128);
    t.putInt(0);
    for (int n : new int[] {37, 30, 0, 126, 42, 0}) t.putInt(n).putInt(1);
    t.put(new byte[] {'T','E','S','T','C','A','M',0});
    var out = new ByteArrayOutputStream();
    out.write(new byte[] {(byte)255, (byte)216, (byte)255, (byte)225, 0, (byte)168});
    out.write(new byte[] {'E','x','i','f',0,0});
    out.write(t.array());
    out.write(jpeg.toByteArray(), 2, jpeg.size()-2);
    return out.toByteArray();
  }

  private static void entry(ByteBuffer t, int tag, int type, int count, int value) {
    t.putShort((short)tag).putShort((short)type).putInt(count).putInt(value);
  }
}
