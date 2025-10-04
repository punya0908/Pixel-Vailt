import java.awt.image.BufferedImage;
import java.math.BigInteger;

public class EncodeBinary {

    public static void encode(String imgPath, String binaryData, String outputPath, int headerBits) throws Exception {
        if (headerBits != 64 && headerBits != 128) {
            throw new IllegalArgumentException("headerBits must be 64 or 128");
        }

        BufferedImage image = SteganographyHelper.readImage(imgPath);

        // Create header: binary representation of message length
        long messageLength = binaryData.length();
        
        // Validate that message length can fit in the header using BigInteger
        BigInteger maxLengthBI = BigInteger.ONE.shiftLeft(headerBits).subtract(BigInteger.ONE); // 2^headerBits - 1
        BigInteger messageLengthBI = BigInteger.valueOf(messageLength);
        
        if (messageLengthBI.compareTo(maxLengthBI) > 0) {
            throw new IllegalArgumentException("Message too long for " + headerBits + "-bit header. " +
                "Max length: " + maxLengthBI + ", Actual length: " + messageLength);
        }
        
        String headerBinary = bigIntToFixedBinary(messageLengthBI, headerBits);

        // Combine header + message
        String fullData = headerBinary + binaryData;

        long dataIndex = 0;
        outerLoop:
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (dataIndex >= fullData.length()) break outerLoop;

                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Modify LSBs in R, G, B in order
                if (dataIndex < fullData.length()) {
                    r = (r & 0xFE) | (fullData.charAt((int)dataIndex) - '0');
                    dataIndex++;
                }
                if (dataIndex < fullData.length()) {
                    g = (g & 0xFE) | (fullData.charAt((int)dataIndex) - '0');
                    dataIndex++;
                }
                if (dataIndex < fullData.length()) {
                    b = (b & 0xFE) | (fullData.charAt((int)dataIndex) - '0');
                    dataIndex++;
                }

                rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }

        String format = SteganographyHelper.getImageFormat(outputPath);
        SteganographyHelper.writeImage(image, format, outputPath);
    }

    // Helper: convert BigInteger to fixed-width binary string
    private static String bigIntToFixedBinary(BigInteger value, int width) {
        String s = value.toString(2);
        if (s.length() > width) throw new IllegalArgumentException("Message too long for header size");
        return "0".repeat(width - s.length()) + s;
    }
}

// import java.awt.image.BufferedImage;

// public class EncodeBinary {
//     public static void encode(String imgPath, String binaryData, String outputPath) throws Exception {
//         BufferedImage image = SteganographyHelper.readImage(imgPath);

//         int dataIndex = 0;
//         outerLoop:
//         for (int y = 0; y < image.getHeight(); y++) {
//             for (int x = 0; x < image.getWidth(); x++) {
//                 if (dataIndex >= binaryData.length()) break outerLoop;

//                 int rgb = image.getRGB(x, y);
//                 int r = (rgb >> 16) & 0xFF;
//                 int g = (rgb >> 8) & 0xFF;
//                 int b = rgb & 0xFF;

//                 // Change LSB of Blue channel
//                 b = (b & 0xFE) | (binaryData.charAt(dataIndex) - '0');
//                 dataIndex++;

//                 rgb = (r << 16) | (g << 8) | b;
//                 image.setRGB(x, y, rgb);
//             }
//         }

//         String format = SteganographyHelper.getImageFormat(outputPath);
//         SteganographyHelper.writeImage(image, format, outputPath);
//     }
// }