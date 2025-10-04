public class BinaryToText {
    public static String convert(String binary) {
        try {
            // binary to Huffman first
            if (binary.length() % 8 != 0) {
                throw new RuntimeException("There is no hidden message");
            }
            
            StringBuilder huffmanData = new StringBuilder();
            for (int i = 0; i < binary.length(); i += 8) {
                String byteStr = binary.substring(i, i + 8);
                int charValue = Integer.parseInt(byteStr, 2);
                huffmanData.append((char) charValue);
            }
            
            // Now decompress using Huffman
            return HuffmanCoding.decompress(huffmanData.toString());
        } catch (Exception e) {
            throw new RuntimeException("There is no hidden message");
        }
    }
}
