package playground.clruch.jmapviewer.tilesources;

public class BlackWhiteTileSource extends CyclicTileSource {

    private static final String[] SERVER = { "a", "b", "c", "d" };

    public BlackWhiteTileSource() {
        super("BlackWhite", "http://%s.tile.stamen.com/toner", "blackwhite", SERVER);
    }

    @Override
    public int getMaxZoom() {
        return 16;
    }
}