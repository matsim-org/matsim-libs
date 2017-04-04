package playground.clruch.gheat.graphics;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public enum DotImages {
    ;
    // ---
    private static final List<BufferedImage> dotsList = new ArrayList<>();

    static {
        for (int zoom = 0; zoom <= 30; ++zoom) 
            dotsList.add(new DotImage(zoom).bufferedImage);
    }

    public static BufferedImage get(int zoom) {
        return dotsList.get(zoom);
    }

}
