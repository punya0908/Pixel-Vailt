import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SteganographyHelper {

    public static BufferedImage readImage(String path) throws IOException {
        return ImageIO.read(new File(path));
    }

    public static void writeImage(BufferedImage image, String format, String outputPath) throws IOException {
        ImageIO.write(image, format, new File(outputPath));
    }

    public static String getImageFormat(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "png" : filename.substring(dotIndex + 1);
    }
}
