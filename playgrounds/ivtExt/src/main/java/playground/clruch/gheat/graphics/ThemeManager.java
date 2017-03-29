package playground.clruch.gheat.graphics;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public enum ThemeManager {
    ;
    private static Map<String, BufferedImage> dotsList;
    private static Map<String, BufferedImage> colorSchemeList;
    private static final String DOTS_FOLDER = "dots";
    private static final String COLOR_SCHMES_FOLDER = "color-schemes";

    public static void init(String directory) throws IOException {
        dotsList = new HashMap<String, BufferedImage>();
        colorSchemeList = new HashMap<String, BufferedImage>();
        for (File file : new File(directory, DOTS_FOLDER).listFiles()) {
            if (file.getName().toLowerCase().endsWith(".png"))
                dotsList.put(file.getName(), ImageIO.read(file));
        }
        for (File file : new File(directory, COLOR_SCHMES_FOLDER).listFiles()) {
            if (file.getName().toLowerCase().endsWith(".png"))
                colorSchemeList.put(file.getName(), ImageIO.read(file));
        }
    }

    public static BufferedImage GetDot(int zoom) {
        return dotsList.get("dot" + zoom + ".png");
    }

    // TODO input to fnction is ignored
    public static BufferedImage GetColorScheme(String schemeName) throws Exception {
        return ColorSchemes.ORANGE.getBufferedImage();
        // if (!colorSchemeList.containsKey(schemeName + ".png"))
        // throw new Exception("Color scheme '" + schemeName + " could not be found");
        // return colorSchemeList.get(schemeName + ".png");
    }
}
