import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class SteganographyApp {

    /* -------------------------- Helpers for image I/O ------------------------- */
    static class StegoHelper {
        static BufferedImage readImage(String path) throws IOException { return ImageIO.read(new File(path)); }
        static void writeImage(BufferedImage img, String format, String out) throws IOException { ImageIO.write(img, format, new File(out)); }
        static String getFormat(String fn) { int d = fn.lastIndexOf('.'); return d == -1 ? "png" : fn.substring(d + 1); }
    }

    /* -------------------------- Huffman coding (compress/decompress) ------------------------- */
    static class Huffman {
        static class Node { char ch; int f; Node l, r; Node(char c, int fr){ch=c;f=fr;} Node(char c,int fr,Node L,Node R){ch=c;f=fr;l=L;r=R;} }

        public static String compress(String text){
            if(text.isEmpty()) return "|"; // empty sentinel
            Map<Character,Integer> freq=new HashMap<>();
            for(char c:text.toCharArray()) freq.put(c, freq.getOrDefault(c,0)+1);
            PriorityQueue<Node> pq=new PriorityQueue<>(Comparator.comparingInt(a->a.f));
            for(Map.Entry<Character,Integer> e:freq.entrySet()) pq.add(new Node(e.getKey(), e.getValue()));
            while(pq.size()>1){Node a=pq.poll(), b=pq.poll(); pq.add(new Node('\0', a.f+b.f, a, b));}
            Node root=pq.peek();
            Map<Character,String> codes=new HashMap<>();
            if(freq.size()==1){ codes.put(root.ch, "0"); }
            else generateCodes(root, "", codes);
            StringBuilder sb=new StringBuilder(); sb.append(serialize(root)).append("|");
            for(char c:text.toCharArray()) sb.append(codes.get(c));
            return sb.toString();
        }

        public static String decompress(String data){
            if(data.equals("|")) return "";
            String[] parts=data.split("\\|",2);
            if(parts.length!=2) return "";
            Node root = deserialize(parts[0], new int[1]); if(root==null) return "";
            StringBuilder out=new StringBuilder();
            if(root.l==null && root.r==null){ // single-char tree
                for(int i=0;i<parts[1].length();i++) out.append(root.ch);
                return out.toString();
            }
            Node cur=root;
            for(char bit:parts[1].toCharArray()){
                cur = (bit=='0')?cur.l:cur.r;
                if(cur!=null && cur.l==null && cur.r==null){ out.append(cur.ch); cur=root; }
            }
            return out.toString();
        }

        static void generateCodes(Node n, String code, Map<Character,String> codes){
            if(n==null) return; if(n.l==null && n.r==null){ codes.put(n.ch, code.isEmpty()?"0":code); return; }
            generateCodes(n.l, code+"0", codes); generateCodes(n.r, code+"1", codes);
        }

        static String serialize(Node n){
            if(n.l==null && n.r==null) return "1" + String.format("%8s", Integer.toBinaryString(n.ch)).replace(' ', '0');
            return "0" + serialize(n.l) + serialize(n.r);
        }

        static Node deserialize(String d, int[] pos){
            if(pos[0]>=d.length()) return null;
            char t = d.charAt(pos[0]++);
            if(t=='1'){ int start=pos[0]; pos[0]+=8; return new Node((char)Integer.parseInt(d.substring(start,pos[0]),2),0); }
            Node left = deserialize(d,pos); Node right = deserialize(d,pos); return new Node('\0',0,left,right);
        }
    }

    /* -------------------------- Text <-> Binary using Huffman ------------------------- */
    static class TextBinary {
        static String toBinary(String text){
            String h = Huffman.compress(text);
            StringBuilder b = new StringBuilder();
            for(char c: h.toCharArray()) b.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ','0'));
            return b.toString();
        }

        static String fromBinary(String binary){
            if(binary.length()%8!=0) throw new RuntimeException("There is no hidden message");
            StringBuilder h=new StringBuilder();
            for(int i=0;i<binary.length(); i+=8){ String byteStr = binary.substring(i,i+8); int val=Integer.parseInt(byteStr,2); h.append((char)val); }
            return Huffman.decompress(h.toString());
        }
    }

    /* -------------------------- Encode binary into image (LSB) ------------------------- */
    static class Encoder {
        public static void encode(String imgPath, String binaryData, String outPath, int headerBits) throws Exception{
            if(headerBits!=64 && headerBits!=128) throw new IllegalArgumentException("headerBits must be 64 or 128");
            BufferedImage img = StegoHelper.readImage(imgPath);
            long msgLen = binaryData.length();
            BigInteger max = BigInteger.ONE.shiftLeft(headerBits).subtract(BigInteger.ONE);
            BigInteger ml = BigInteger.valueOf(msgLen);
            if(ml.compareTo(max)>0) throw new IllegalArgumentException("Message too long for header");
            String header = bigIntToFixedBinary(ml, headerBits);
            String full = header + binaryData;
            long di=0;
            outer: for(int y=0;y<img.getHeight();y++){
                for(int x=0;x<img.getWidth();x++){
                    if(di>=full.length()) break outer;
                    int rgb=img.getRGB(x,y); int r=(rgb>>16)&0xFF, g=(rgb>>8)&0xFF, b=rgb&0xFF;
                    if(di<full.length()){ r=(r&0xFE)|(full.charAt((int)di)-'0'); di++; }
                    if(di<full.length()){ g=(g&0xFE)|(full.charAt((int)di)-'0'); di++; }
                    if(di<full.length()){ b=(b&0xFE)|(full.charAt((int)di)-'0'); di++; }
                    img.setRGB(x,y,(r<<16)|(g<<8)|b);
                }
            }
            String fmt = StegoHelper.getFormat(outPath);
            StegoHelper.writeImage(img, fmt, outPath);
        }

        private static String bigIntToFixedBinary(BigInteger v, int w){ String s = v.toString(2); if(s.length()>w) throw new IllegalArgumentException("Message too long for header size"); return "0".repeat(w - s.length()) + s; }
    }

    /* -------------------------- Decode binary from image (reads header then payload) ------------------------- */
    static class Decoder {
        public static String decode(String imgPath, int headerBits) throws Exception{
            BufferedImage image = StegoHelper.readImage(imgPath);
            long maxPossible = (long)image.getWidth()*image.getHeight()*3L;
            if(maxPossible < headerBits) throw new RuntimeException("There is no hidden message");
            StringBuilder allBits = new StringBuilder(); long totalBitsRead=0;
            outerLoop: for(int y=0;y<image.getHeight();y++){
                for(int x=0;x<image.getWidth();x++){
                    int rgb=image.getRGB(x,y); int r=(rgb>>16)&0xFF; int g=(rgb>>8)&0xFF; int b=rgb&0xFF;
                    allBits.append(r&1); totalBitsRead++;
                    allBits.append(g&1); totalBitsRead++;
                    allBits.append(b&1); totalBitsRead++;
                    if(totalBitsRead>=headerBits){ String header = allBits.substring(0, headerBits); long msgBits = new BigInteger(header,2).longValue(); long totalNeeded = headerBits + msgBits; if(totalBitsRead>=totalNeeded) break outerLoop; }
                }
            }
            if(allBits.length() < headerBits) throw new RuntimeException("There is no hidden message");
            String headerBinary = allBits.substring(0, headerBits);
            BigInteger headerVal = new BigInteger(headerBinary,2);
            if(headerVal.compareTo(BigInteger.ZERO)<=0) throw new RuntimeException("There is no hidden message");
            if(headerVal.compareTo(BigInteger.valueOf(Long.MAX_VALUE))>0) throw new RuntimeException("There is no hidden message");
            long messageBitLength = headerVal.longValue();
            if(messageBitLength <=0) throw new RuntimeException("There is no hidden message");
            long remainingBits = maxPossible - headerBits;
            if(messageBitLength > remainingBits) throw new RuntimeException("There is no hidden message");
            if(messageBitLength > remainingBits/2) throw new RuntimeException("There is no hidden message");
            long totalExpected = headerBits + messageBitLength;
            if(allBits.length() < totalExpected) throw new RuntimeException("There is no hidden message");
            if(messageBitLength > Integer.MAX_VALUE - headerBits) throw new RuntimeException("Message too large for current implementation. Message length: " + messageBitLength);
            int start=(int)headerBits; int end=(int)(headerBits + messageBitLength);
            String messageBinary = allBits.substring(start, end);
            if(messageBinary.length()%8!=0) throw new RuntimeException("There is no hidden message");
            int printable=0; int totalBytes = messageBinary.length()/8; for(int i=0;i<Math.min(totalBytes,50);i++){String byteStr=messageBinary.substring(i*8,(i+1)*8); int v=Integer.parseInt(byteStr,2); if((v>=32&&v<=126)||v==9||v==10||v==13) printable++; }
            int checked = Math.min(totalBytes,50); if(checked>0 && (double)printable/checked < 0.7) throw new RuntimeException("There is no hidden message");
            return messageBinary;
        }
    }

    /* -------------------------- CLI main (keeps sequential behaviour) ------------------------- */
    public static void main(String[] args) throws Exception{
        Scanner sc = new Scanner(System.in);
        while(true){
            System.out.println("\nyour secrets are safe here\n");
            System.out.println("1 - Encode text into image");
            System.out.println("2 - Decode text from image");
            System.out.println("3 - Convert text to binary");
            System.out.println("4 - Convert binary to text");
            System.out.println("0 - Exit");
            System.out.print("Enter choice: ");
            int choice;
            try{ choice = Integer.parseInt(sc.nextLine().trim()); } catch(Exception e){ System.out.println("Invalid choice."); continue; }
            switch(choice){
                case 1: try{
                    System.out.print("Enter image path: "); String imgPath = sc.nextLine().trim();
                    System.out.print("Enter secret text: "); String secret = sc.nextLine();
                    String binaryData = TextBinary.toBinary(secret);
                    System.out.print("File name: "); String fileName = sc.nextLine().trim(); if(!fileName.toLowerCase().endsWith(".png")) fileName += ".png";
                    String outputPath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
                    int headerBits = 64;
                    Encoder.encode(imgPath, binaryData, outputPath, headerBits);
                    System.out.println("Image saved to: " + outputPath);
                } catch(Exception e){ System.out.println("Failed to encode: " + e.getMessage()); } break;

                case 2: try{
                    System.out.print("Enter image path: "); String decodeImgPath = sc.nextLine().trim(); int headerBits = 64;
                    String decodedBinary = Decoder.decode(decodeImgPath, headerBits);
                    String decodedText = TextBinary.fromBinary(decodedBinary);
                    System.out.println("Decoded text: " + decodedText);
                } catch(RuntimeException e){ System.out.println("There is no hidden message"); } catch(Exception e){ System.out.println("There is no hidden message"); } break;

                case 3: System.out.print("Enter text: "); String text = sc.nextLine(); System.out.println("Binary: " + TextBinary.toBinary(text)); break;
                case 4: try{ System.out.print("Enter binary: "); String binary = sc.nextLine(); System.out.println("Text: " + TextBinary.fromBinary(binary)); } catch(Exception e){ System.out.println("There is no hidden message"); } break;
                case 0: return;
                default: System.out.println("Invalid choice.");
            }
            sc.close();
        }
    }
}
