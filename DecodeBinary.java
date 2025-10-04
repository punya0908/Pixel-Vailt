import java.awt.image.BufferedImage;
import java.math.BigInteger;

public class DecodeBinary {

    public static String decode(String imgPath, int headerBits) throws Exception {
        try {
            BufferedImage image = SteganographyHelper.readImage(imgPath);

            // Calculate maximum possible bits in the image
            long maxPossibleBits = (long) image.getWidth() * image.getHeight() * 3; // 3 bits per pixel (R,G,B)
            
            // Check if image can even hold the header
            if (maxPossibleBits < headerBits) {
                throw new RuntimeException("There is no hidden message");
            }

            // Read all bits sequentially from R, G, B channels (matching EncodeBinary)
            StringBuilder allBits = new StringBuilder();
            long totalBitsRead = 0;

            outerLoop:
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // Read R, G, B in order (matching encode logic)
                    allBits.append(r & 1);
                    totalBitsRead++;
                    
                    allBits.append(g & 1);
                    totalBitsRead++;
                    
                    allBits.append(b & 1);
                    totalBitsRead++;

                    // Check if we have read at least the header
                    if (totalBitsRead >= headerBits) {
                        // Parse header to get message length
                        String headerBinary = allBits.substring(0, headerBits);
                        long messageBitLength = Long.parseLong(headerBinary, 2);
                        
                        // Check if we have read enough for header + message
                        long totalNeededBits = headerBits + messageBitLength;
                        if (totalBitsRead >= totalNeededBits) {
                            break outerLoop;
                        }
                    }
                }
            }

            // Extract header and parse message length
            if (allBits.length() < headerBits) {
                throw new RuntimeException("There is no hidden message");
            }
            
            String headerBinary = allBits.substring(0, headerBits);
            long messageBitLength;
            
            try {
                // Use BigInteger to safely parse large binary strings
                BigInteger headerValue = new BigInteger(headerBinary, 2);
                
                // Check if the value is within reasonable bounds for a message length
                // Maximum reasonable message length is Long.MAX_VALUE
                if (headerValue.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                    throw new RuntimeException("There is no hidden message");
                }
                
                // Check if it's negative (which shouldn't happen with unsigned interpretation)
                if (headerValue.compareTo(BigInteger.ZERO) <= 0) {
                    throw new RuntimeException("There is no hidden message");
                }
                
                messageBitLength = headerValue.longValue();
            } catch (NumberFormatException e) {
                throw new RuntimeException("There is no hidden message");
            }
            
            // Validate message length - check for reasonable bounds
            if (messageBitLength <= 0) {
                throw new RuntimeException("There is no hidden message");
            }
            
            // Check if message length is impossibly large for this image
            long remainingBits = maxPossibleBits - headerBits;
            if (messageBitLength > remainingBits) {
                throw new RuntimeException("There is no hidden message");
            }
            
            // Check if message length is suspiciously large (likely random data)
            // A reasonable text message should be much smaller than the image capacity
            if (messageBitLength > remainingBits / 2) {
                throw new RuntimeException("There is no hidden message");
            }
            
            // Extract message binary (skip header)
            long totalExpectedBits = headerBits + messageBitLength;
            if (allBits.length() < totalExpectedBits) {
                throw new RuntimeException("There is no hidden message");
            }
            
            // Use long arithmetic for the substring bounds
            int startIndex = headerBits;
            int endIndex = (int)(headerBits + messageBitLength);
            
            // Check if the message length exceeds Integer.MAX_VALUE for substring operations
            if (messageBitLength > Integer.MAX_VALUE - headerBits) {
                throw new RuntimeException("Message too large for current implementation. Message length: " + messageBitLength);
            }
            
            String messageBinary = allBits.substring(startIndex, endIndex);
            
            // Additional validation: check if binary length is divisible by 8 (valid for text)
            if (messageBinary.length() % 8 != 0) {
                throw new RuntimeException("There is no hidden message");
            }
            
            // Basic sanity check: ensure some portion of the decoded bytes represent printable characters
            int printableCount = 0;
            int totalBytes = messageBinary.length() / 8;
            
            for (int i = 0; i < Math.min(totalBytes, 50); i++) { // Check first 50 characters or all if less
                String byteStr = messageBinary.substring(i * 8, (i + 1) * 8);
                int charValue = Integer.parseInt(byteStr, 2);
                
                // Count printable ASCII characters (32-126) and common whitespace
                if ((charValue >= 32 && charValue <= 126) || charValue == 9 || charValue == 10 || charValue == 13) {
                    printableCount++;
                }
            }
            
            // If less than 70% of checked characters are printable, it's likely not a real message
            int checkedBytes = Math.min(totalBytes, 50);
            if (checkedBytes > 0 && (double)printableCount / checkedBytes < 0.7) {
                throw new RuntimeException("There is no hidden message");
            }
            
            return messageBinary;
            
        } catch (RuntimeException e) {
            // Re-throw our custom "no hidden message" exceptions
            throw e;
        } catch (Exception e) {
            // Any other unexpected exception means no hidden message
            throw new RuntimeException("There is no hidden message");
        }
    }
}

// import java.awt.image.BufferedImage;

// public class DecodeBinary {
//     public static String decode(String imgPath, int bitLength) throws Exception {
//         BufferedImage image = SteganographyHelper.readImage(imgPath);

//         StringBuilder binaryData = new StringBuilder();
//         outerLoop:
//         for (int y = 0; y < image.getHeight(); y++) {
//             for (int x = 0; x < image.getWidth(); x++) {
//                 if (binaryData.length() >= bitLength) break outerLoop;

//                 int rgb = image.getRGB(x, y);
//                 int b = rgb & 0xFF;
//                 binaryData.append(b & 1);
//             }
//         }

//         return binaryData.toString();
//     }
// }