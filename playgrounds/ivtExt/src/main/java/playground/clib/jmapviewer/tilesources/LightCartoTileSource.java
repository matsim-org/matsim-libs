// code by jph
package playground.clib.jmapviewer.tilesources;

public class LightCartoTileSource extends CartoTileSource {

    public LightCartoTileSource() {
        super("CartoLight", "http://%s.basemaps.cartocdn.com/light_all", "CartoLight");
    }
}