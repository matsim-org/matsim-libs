// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.tilesources;

import java.awt.Point;

import playground.clruch.jmapviewer.Coordinate;
import playground.clruch.jmapviewer.OsmMercator;
import playground.clruch.jmapviewer.TileXY;
import playground.clruch.jmapviewer.interfaces.ICoordinate;

/**
 * TMS tile source.
 */
class TMSTileSource extends AbstractTMSTileSource {

    protected int maxZoom;
    protected int minZoom;
    protected OsmMercator osmMercator;

    /**
     * Constructs a new {@code TMSTileSource}.
     * 
     * @param info
     *            tile source information
     */
    public TMSTileSource(TileSourceInfo info) {
        super(info);
        minZoom = info.getMinZoom();
        maxZoom = info.getMaxZoom();
        this.osmMercator = new OsmMercator(this.getTileSize());
    }

    @Override
    public int getMinZoom() {
        return (minZoom == 0) ? super.getMinZoom() : minZoom;
    }

    @Override
    public int getMaxZoom() {
        return (maxZoom == 0) ? super.getMaxZoom() : maxZoom;
    }

    @Override
    public double getDistance(double lat1, double lon1, double lat2, double lon2) {
        return osmMercator.getDistance(lat1, lon1, lat2, lon2);
    }

    @Override
    public Point latLonToXY(double lat, double lon, int zoom) {
        return new Point((int) osmMercator.lonToX(lon, zoom), (int) osmMercator.latToY(lat, zoom));
    }

    @Override
    public ICoordinate xyToLatLon(int x, int y, int zoom) {
        return new Coordinate(osmMercator.yToLat(y, zoom), osmMercator.xToLon(x, zoom));
    }

    @Override
    public TileXY latLonToTileXY(double lat, double lon, int zoom) {
        return new TileXY(osmMercator.lonToX(lon, zoom) / getTileSize(), osmMercator.latToY(lat, zoom) / getTileSize());
    }

    @Override
    public ICoordinate tileXYToLatLon(int x, int y, int zoom) {
        return new Coordinate(osmMercator.yToLat(y * getTileSize(), zoom), osmMercator.xToLon(x * getTileSize(), zoom));
    }
}
