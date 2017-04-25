package playground.clruch.jmapviewer.tilesources;

public class DarkCartoTileSource extends CartoTileSource {

    public DarkCartoTileSource() {
        super("CartoDark", "http://%s.basemaps.cartocdn.com/dark_all", "cartodark");
    }

}