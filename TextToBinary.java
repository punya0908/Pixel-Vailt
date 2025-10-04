public class TextToBinary {
    public static String convert(String text) {
        // Get Huffman compressed format
        String huffmanData = HuffmanCoding.compress(text);
        
        // Convert to pure binary for steganography
        StringBuilder binaryResult = new StringBuilder();
        for (char c : huffmanData.toCharArray()) {
            binaryResult.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
        }
        return binaryResult.toString();
    }
}
