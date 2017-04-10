package playground.clruch.jmapviewer.tilesources;

public class WatercolorTileSource extends CyclicTileSource {

    private static final String[] SERVER = { "a", "b", "c", "d" };

    public WatercolorTileSource() {
        super("Watercolor", "http://%s.tile.stamen.com/watercolor", "watercolor", SERVER);
    }

    @Override
    public int getMaxZoom() {
        return 18;
    }
}