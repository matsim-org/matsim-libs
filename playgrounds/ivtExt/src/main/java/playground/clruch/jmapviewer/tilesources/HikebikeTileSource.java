package playground.clruch.jmapviewer.tilesources;

public class HikebikeTileSource extends CyclicTileSource {

    private static final String[] SERVER = { "a", "b", "c" };

    public HikebikeTileSource() {
        super("Hikebike", "http://%s.tiles.wmflabs.org/hikebike", "hikebike", SERVER);
    }

}