// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer;

/**
 * This class implements the Mercator Projection as it is used by OpenStreetMap
 * (and google). It provides methods to translate coordinates from 'map space'
 * into latitude and longitude (on the WGS84 ellipsoid) and vice versa. Map
 * space is measured in pixels. The origin of the map space is the top left
 * corner. The map space origin (0,0) has latitude ~85 and longitude -180.
 * @author Jan Peter Stotz
 * @author Jason Huntley
 */
public class OsmMercator {

    /**
     * default tile size
     */
    public static final int DEFAUL_TILE_SIZE = 256;
    /** maximum latitude (north) for mercator display */
    public static final double MAX_LAT = 85.05112877980659;
    /** minimum latitude (south) for mercator display */
    public static final double MIN_LAT = -85.05112877980659;
    /** equatorial earth radius for EPSG:3857 (Mercator) */
    public static final double EARTH_RADIUS = 6_378_137;

    /**
     * instance with tile size of 256 for easy conversions
     */
    public static final OsmMercator MERCATOR_256 = new OsmMercator();

    /** tile size of the displayed tiles */
    private int tileSize = DEFAUL_TILE_SIZE;

    /**
     * Creates instance with default tile size of 256
     */
    public OsmMercator() {
    }

    /**
     * Creates instance with provided tile size.
     * @param tileSize tile size in pixels
     */
    public OsmMercator(int tileSize) {
        this.tileSize = tileSize;
    }

    public double radius(int aZoomlevel) {
        return (tileSize * (1 << aZoomlevel)) / (2.0 * Math.PI);
    }

    /**
     * Returns the absolut number of pixels in y or x, defined as: 2^Zoomlevel *
     * tileSize where tileSize is the width of a tile in pixels
     *
     * @param aZoomlevel zoom level to request pixel data
     * @return number of pixels
     */
    public int getMaxPixels(int aZoomlevel) {
        return tileSize * (1 << aZoomlevel);
    }

    public int falseEasting(int aZoomlevel) {
        return getMaxPixels(aZoomlevel) / 2;
    }

    public int falseNorthing(int aZoomlevel) {
        return -1 * getMaxPixels(aZoomlevel) / 2;
    }

    /**
     * Transform pixelspace to coordinates and get the distance.
     *
     * @param x1 the first x coordinate
     * @param y1 the first y coordinate
     * @param x2 the second x coordinate
     * @param y2 the second y coordinate
     *
     * @param zoomLevel the zoom level
     * @return the distance
     */
    public double getDistance(int x1, int y1, int x2, int y2, int zoomLevel) {
        double la1 = yToLat(y1, zoomLevel);
        double lo1 = xToLon(x1, zoomLevel);
        double la2 = yToLat(y2, zoomLevel);
        double lo2 = xToLon(x2, zoomLevel);

        return getDistance(la1, lo1, la2, lo2);
    }

    /**
     * Gets the distance using Spherical law of cosines.
     *
     * @param la1 the Latitude in degrees
     * @param lo1 the Longitude in degrees
     * @param la2 the Latitude from 2nd coordinate in degrees
     * @param lo2 the Longitude from 2nd coordinate in degrees
     * @return the distance
     */
    public double getDistance(double la1, double lo1, double la2, double lo2) {
        double aStartLat = Math.toRadians(la1);
        double aStartLong = Math.toRadians(lo1);
        double aEndLat = Math.toRadians(la2);
        double aEndLong = Math.toRadians(lo2);

        double distance = Math.acos(Math.sin(aStartLat) * Math.sin(aEndLat)
                + Math.cos(aStartLat) * Math.cos(aEndLat)
                * Math.cos(aEndLong - aStartLong));

        return EARTH_RADIUS * distance;
    }

    /**
     * Transform longitude to pixelspace
     *
     * <p>
     * Mathematical optimization<br>
     * <code>
     * x = radius(aZoomlevel) * toRadians(aLongitude) + falseEasting(aZoomLevel)<br>
     * x = getMaxPixels(aZoomlevel) / (2 * PI) * (aLongitude * PI) / 180 + getMaxPixels(aZoomlevel) / 2<br>
     * x = getMaxPixels(aZoomlevel) * aLongitude / 360 + 180 * getMaxPixels(aZoomlevel) / 360<br>
     * x = getMaxPixels(aZoomlevel) * (aLongitude + 180) / 360<br>
     * </code>
     * </p>
     *
     * @param aLongitude
     *            [-180..180]
     * @param aZoomlevel zoom level
     * @return [0..2^Zoomlevel*TILE_SIZE[
     */
    public double lonToX(double aLongitude, int aZoomlevel) {
        int mp = getMaxPixels(aZoomlevel);
        double x = (mp * (aLongitude + 180L)) / 360L;
        return Math.min(x, mp);
    }

    /**
     * Transforms latitude to pixelspace
     * <p>
     * Mathematical optimization<br>
     * <code>
     * log(u) := log((1.0 + sin(toRadians(aLat))) / (1.0 - sin(toRadians(aLat))<br>
     *
     * y = -1 * (radius(aZoomlevel) / 2 * log(u)))) - falseNorthing(aZoomlevel))<br>
     * y = -1 * (getMaxPixel(aZoomlevel) / 2 * PI / 2 * log(u)) - -1 * getMaxPixel(aZoomLevel) / 2<br>
     * y = getMaxPixel(aZoomlevel) / (-4 * PI) * log(u)) + getMaxPixel(aZoomLevel) / 2<br>
     * y = getMaxPixel(aZoomlevel) * ((log(u) / (-4 * PI)) + 1/2)<br>
     * </code>
     * </p>
     * @param aLat
     *            [-90...90]
     * @param aZoomlevel zoom level
     * @return [0..2^Zoomlevel*TILE_SIZE[
     */
    public double latToY(double aLat, int aZoomlevel) {
        if (aLat < MIN_LAT)
            aLat = MIN_LAT;
        else if (aLat > MAX_LAT)
            aLat = MAX_LAT;
        double sinLat = Math.sin(Math.toRadians(aLat));
        double log = Math.log((1.0 + sinLat) / (1.0 - sinLat));
        int mp = getMaxPixels(aZoomlevel);
        double y = mp * (0.5 - (log / (4.0 * Math.PI)));
        return Math.min(y, mp - 1);
    }

    /**
     * Transforms pixel coordinate X to longitude
     *
     * <p>
     * Mathematical optimization<br>
     * <code>
     * lon = toDegree((aX - falseEasting(aZoomlevel)) / radius(aZoomlevel))<br>
     * lon = 180 / PI * ((aX - getMaxPixels(aZoomlevel) / 2) / getMaxPixels(aZoomlevel) / (2 * PI)<br>
     * lon = 180 * ((aX - getMaxPixels(aZoomlevel) / 2) / getMaxPixels(aZoomlevel))<br>
     * lon = 360 / getMaxPixels(aZoomlevel) * (aX - getMaxPixels(aZoomlevel) / 2)<br>
     * lon = 360 * aX / getMaxPixels(aZoomlevel) - 180<br>
     * </code>
     * </p>
     * @param aX
     *            [0..2^Zoomlevel*TILE_WIDTH[
     * @param aZoomlevel zoom level
     * @return ]-180..180[
     */
    public double xToLon(int aX, int aZoomlevel) {
        return ((360d * aX) / getMaxPixels(aZoomlevel)) - 180.0;
    }

    /**
     * Transforms pixel coordinate Y to latitude
     *
     * @param aY
     *            [0..2^Zoomlevel*TILE_WIDTH[
     * @param aZoomlevel zoom level
     * @return [MIN_LAT..MAX_LAT] is about [-85..85]
     */
    public double yToLat(int aY, int aZoomlevel) {
        aY += falseNorthing(aZoomlevel);
        double latitude = (Math.PI / 2) - (2 * Math.atan(Math.exp(-1.0 * aY / radius(aZoomlevel))));
        return -1 * Math.toDegrees(latitude);
    }

}
