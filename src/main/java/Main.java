import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sourceforge.tess4j.*;

import javax.imageio.ImageIO;

public class Main {
    private static final int SIZE = 37;
    public static void main(String[] args) {
        File imageFile = new File("src/main/resources/blandwh.png");
        Set<String> set = Collections.synchronizedSet(new TreeSet<>());
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(1369);
        for (int x = 0; x < SIZE; ++x) {
            for (int y = 0; y < SIZE; ++y) {
                list.add(x * SIZE + y, Map.entry(x, y));
            }
        }
        String[][] strings = new String[SIZE][SIZE];
        list.parallelStream().forEach(entry -> {
            try {
                ITesseract instance = new Tesseract();
                instance.setDatapath("src/main/resources/tessdata");
                String result = instance.doOCR(imageFile, new Rectangle(entry.getKey() * 100, entry.getValue() * 100, 100, 100))
                        .replaceAll("\\s+", " ").toLowerCase();
                strings[entry.getKey()][entry.getValue()] = result;
                set.add(result);
            } catch (TesseractException e) {
                System.err.println(e.getMessage());
            }
        });

        Pattern whitePattern = Pattern.compile(".*(\0x2b|wh.*t.*|).*");
        Pattern blackPattern = Pattern.compile(".*((not|n00).?wh|dark|ness|black|olack).*");
        set.forEach(System.out::println);
        Map<String, Boolean> stringToColor = set.stream().map(key -> Map.entry(key, key.startsWith("(") || key.matches(".*\\d.*")
                ? (key.length() >= 12) : whitePattern.matcher(key).matches() && !blackPattern.matcher(key).matches())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < SIZE; ++x) {
            for (int y = 0; y < SIZE; ++y) {
                image.setRGB(x, y, stringToColor.get(strings[x][y]) ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
            }
        }
        File outputfile = new File("result.png");
        try {
            ImageIO.write(image, "png", outputfile);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}