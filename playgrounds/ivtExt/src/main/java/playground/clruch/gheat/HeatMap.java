package playground.clruch.gheat;

import java.awt.image.BufferedImage;

import playground.clruch.gheat.graphics.ColorScheme;
import playground.clruch.gheat.graphics.DotImages;

public class HeatMap {
    public static boolean isInitialised = false;

    private HeatMap() {
    }

    public static final int SIZE = 256;
    public static final int MAX_ZOOM = 31;

    public static BufferedImage GetTile( //
            DataManager dataManager, //
            ColorScheme colorScheme, //
            int zoom, int x, int y) throws Exception {
        if (dataManager == null)
            throw new Exception("No 'Data manager' has been specified");
        return Tile.Generate( //
                colorScheme, //
                DotImages.get(zoom), //
                zoom, //
                x, //
                y, //
                dataManager.GetPointsForTile(x, y, DotImages.get(zoom).bufferedImageRGB, zoom));
    }
}
