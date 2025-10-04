import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\nyour secrets are safe here\n");
            System.out.println("1 - Encode text into image");
            System.out.println("2 - Decode text from image");
            System.out.println("3 - Convert text to binary");
            System.out.println("4 - Convert binary to text");
            System.out.println("0 - Exit");
            System.out.print("Enter choice: ");
            int choice = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    System.out.print("Enter image path: ");
                    // C:\Users\HP\Downloads\cat1.jpg
                    String imgPath = sc.nextLine();
                    System.out.print("Enter secret text: ");
                    String secret = sc.nextLine();
                    String binaryData = TextToBinary.convert(secret);
                    System.out.print("File name: ");
                    String fileName = sc.nextLine().trim();
                    if (!fileName.toLowerCase().endsWith(".png")) {
                        fileName += ".png";
                    }
                    String outputPath = "C:\\Users\\HP\\Downloads\\" + fileName;
                    int headerBits = 64; // this is for msg len bits
                    EncodeBinary.encode(imgPath, binaryData, outputPath, headerBits);
                    System.out.println("Image saved to: " + outputPath);
                    break;
                case 2: // 5 page doc bhi chal rhe
                    System.out.print("Enter image path: ");
                    String decodeImgPath = sc.nextLine();

                    headerBits = 64;
                    try {
                        String decodedBinary = DecodeBinary.decode(decodeImgPath, headerBits);
                        // System.out.println("Decoded binary length: " + decodedBinary.length());
                        // System.out.println("Decoded binary (first 64 chars): " + decodedBinary.substring(0, Math.min(64, decodedBinary.length())));
                        
                        String decodedText = BinaryToText.convert(decodedBinary);
                        System.out.println("Decoded text: " + decodedText);
                    } catch (RuntimeException e) {
                        if (e.getMessage() != null && e.getMessage().equals("There is no hidden message")) {
                            System.out.println("There is no hidden message");
                        } else {
                            System.out.println("There is no hidden message");
                        }
                    } catch (Exception e) {
                        // Any other exception also means no hidden message
                        System.out.println("There is no hidden message");
                    }
                    break;


                case 3:
                    System.out.print("Enter text: ");
                    String text = sc.nextLine();
                    System.out.println("Binary: " + TextToBinary.convert(text));
                    break;

                case 4:
                    System.out.print("Enter binary: ");
                    String binary = sc.nextLine();
                    System.out.println("Text: " + BinaryToText.convert(binary));
                    break;

                case 0:
                    // System.out.println("Exiting...");
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
}
