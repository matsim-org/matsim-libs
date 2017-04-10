package playground.clruch.jmapviewer.tilesources;

public class LandscapeTileSource extends CyclicTileSource {

    private static final String[] SERVER = { "a", "b", "c" };

    public LandscapeTileSource() {
        super("Landscape", "https://%s.tile.thunderforest.com/landscape", "landscape", SERVER);
    }

}