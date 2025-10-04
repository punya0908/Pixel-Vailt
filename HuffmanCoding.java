import java.util.*;

public class HuffmanCoding {
    static class Node {
        char ch; int freq; Node left, right;
        Node(char c, int f) { ch = c; freq = f; }
        Node(char c, int f, Node l, Node r) { ch = c; freq = f; left = l; right = r; }
    }

    public static String compress(String text) {
        if (text.isEmpty()) return "|";
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : text.toCharArray()) freq.put(c, freq.getOrDefault(c, 0) + 1);
        
        PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> a.freq - b.freq);
        freq.forEach((c, f) -> pq.offer(new Node(c, f)));
        
        // Build Huffman tree properly
        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            pq.offer(new Node('\0', left.freq + right.freq, left, right));
        }
        
        Map<Character, String> codes = new HashMap<>();
        Node root = pq.peek();
        if (freq.size() == 1) {
            // Special case: single character
            codes.put(root.ch, "0");
        } else {
            generateCodes(root, "", codes);
        }
        
        StringBuilder result = new StringBuilder(serialize(root) + "|");
        for (char c : text.toCharArray()) result.append(codes.get(c));
        return result.toString();
    }

    public static String decompress(String data) {
        if (data.equals("|")) return "";
        String[] parts = data.split("\\|", 2);
        if (parts.length != 2) return "";
        
        Node root = deserialize(parts[0], new int[1]);
        if (root == null) return "";
        
        StringBuilder result = new StringBuilder();
        Node curr = root;
        
        // Handle single character tree
        if (root.left == null && root.right == null) {
            for (int i = 0; i < parts[1].length(); i++) {
                result.append(root.ch);
            }
            return result.toString();
        }
        
        // Handle normal tree
        for (char bit : parts[1].toCharArray()) {
            curr = bit == '0' ? curr.left : curr.right;
            if (curr != null && curr.left == null && curr.right == null) {
                result.append(curr.ch);
                curr = root;
            }
        }
        return result.toString();
    }

    static void generateCodes(Node n, String code, Map<Character, String> codes) {
        if (n == null) return;
        if (n.left == null && n.right == null) {
            codes.put(n.ch, code.isEmpty() ? "0" : code);
            return;
        }
        generateCodes(n.left, code + "0", codes);
        generateCodes(n.right, code + "1", codes);
    }

    static String serialize(Node n) {
        return n.left == null ? "1" + String.format("%8s", Integer.toBinaryString(n.ch)).replace(' ', '0')
                              : "0" + serialize(n.left) + serialize(n.right);
    }

    static Node deserialize(String data, int[] pos) {
        if (pos[0] >= data.length()) return null;
        return data.charAt(pos[0]++) == '1' 
            ? new Node((char) Integer.parseInt(data.substring(pos[0], pos[0] += 8), 2), 0)
            : new Node('\0', 0, deserialize(data, pos), deserialize(data, pos));
    }
}
