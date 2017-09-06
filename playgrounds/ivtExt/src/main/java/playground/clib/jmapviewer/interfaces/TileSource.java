// License: GPL. For details, see Readme.txt file.
package playground.clib.jmapviewer.interfaces;

import java.awt.Point;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import playground.clib.jmapviewer.JMapViewer;
import playground.clib.jmapviewer.Tile;
import playground.clib.jmapviewer.TileXY;

/**
 *
 * @author Jan Peter Stotz
 */
public interface TileSource extends Attributed {

    /**
     * Specifies the maximum zoom value. The number of zoom levels is [0..
     * {@link #getMaxZoom()}].
     *
     * @return maximum zoom value that has to be smaller or equal to
     *         {@link JMapViewer#MAX_ZOOM}
     */
    int getMaxZoom();

    /**
     * Specifies the minimum zoom value. This value is usually 0.
     * Only for maps that cover a certain region up to a limited zoom level
     * this method should return a value different than 0.
     *
     * @return minimum zoom value - usually 0
     */
    int getMinZoom();

    /**
     * A tile layer name as displayed to the user.
     *
     * @return Name of the tile layer
     */
    String getName();

    /**
     * A unique id for this tile source.
     *
     * Unlike the name it has to be unique and has to consist only of characters
     * valid for filenames.
     *
     * @return the id
     */
    String getId();

    /**
     * Constructs the tile url.
     *
     * @param zoom zoom level
     * @param tilex X coordinate
     * @param tiley Y coordinate
     * @return fully qualified url for downloading the specified tile image
     * @throws IOException if any I/O error occurs
     */
    String getTileUrl(int zoom, int tilex, int tiley) throws IOException;

    /**
     * Creates tile identifier that is unique among all tile sources, but the same tile will always
     * get the same identifier. Used for creation of cache key.
     *
     * @param zoom zoom level
     * @param tilex X coordinate
     * @param tiley Y coordinate
     * @return tile identifier
     */
    String getTileId(int zoom, int tilex, int tiley);

    /**
     * Specifies how large each tile is.
     * @return The size of a single tile in pixels. -1 if default size should be used
     */
    int getTileSize();

    /**
     * @return default tile size, for this tile source
     * TODO: @since
     */
    int getDefaultTileSize();

    /**
     * Gets the distance using Spherical law of cosines.
     * @param la1 latitude of first point
     * @param lo1 longitude of first point
     * @param la2 latitude of second point
     * @param lo2 longitude of second point
     * @return the distance betwen first and second point, in m.
     */
    double getDistance(double la1, double lo1, double la2, double lo2);

    /**
     * @param lon longitude
     * @param lat latitude
     * @param zoom zoom level
     * @return transforms longitude and latitude to pixel space (as if all tiles at specified zoom level where joined)
     */
    Point latLonToXY(double lat, double lon, int zoom);

    /**
     * @param point point
     * @param zoom zoom level
     * @return transforms longitude and latitude to pixel space (as if all tiles at specified zoom level where joined)
     */
    Point latLonToXY(ICoordinate point, int zoom);

    /**
     * @param point point
     * @param zoom zoom level
     * @return WGS84 Coordinates of given point
     */
    ICoordinate xyToLatLon(Point point, int zoom);

    /**
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param zoom zoom level
     * @return WGS84 Coordinates of given point
     */
    ICoordinate xyToLatLon(int x, int y, int zoom);

    /**
     * @param lon longitude
     * @param lat latitude
     * @param zoom zoom level
     * @return x and y tile indices
     */
    TileXY latLonToTileXY(double lat, double lon, int zoom);

    /**
     *
     * @param point point
     * @param zoom zoom level
     * @return x and y tile indices
     */
    TileXY latLonToTileXY(ICoordinate point, int zoom);

    /**
     * @param xy X/Y coordinates
     * @param zoom zoom level
     * @return WGS84 coordinates of given tile
     */
    ICoordinate tileXYToLatLon(TileXY xy, int zoom);

    /**
     *
     * @param tile Tile
     * @return WGS84 coordinates of given tile
     */
    ICoordinate tileXYToLatLon(Tile tile);

    /**
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param zoom zoom level
     * @return WGS84 coordinates of given tile
     */
    ICoordinate tileXYToLatLon(int x, int y, int zoom);

    /**
     * @param zoom zoom level
     * @return maximum X index of tile for specified zoom level
     */
    int getTileXMax(int zoom);

    /**
     *
     * @param zoom zoom level
     * @return minimum X index of tile for specified zoom level
     */
    int getTileXMin(int zoom);

    /**
     *
     * @param zoom zoom level
     * @return maximum Y index of tile for specified zoom level
     */
    int getTileYMax(int zoom);

    /**
     * @param zoom zoom level
     * @return minimum Y index of tile for specified zoom level
     */
    int getTileYMin(int zoom);

    /**
     * Determines, if the returned data from TileSource represent "no tile at this zoom level" situation. Detection
     * algorithms differ per TileSource, so each TileSource should implement each own specific way.
     *
     * @param headers HTTP headers from response from TileSource server
     * @param statusCode HTTP status code
     * @param content byte array representing the data returned from the server
     * @return true, if "no tile at this zoom level" situation detected
     */
    boolean isNoTileAtZoom(Map<String, List<String>> headers, int statusCode, byte[] content);

    /**
     * Extracts metadata about the tile based on HTTP headers
     *
     * @param headers HTTP headers from Tile Source server
     * @return tile metadata
     */
    Map<String, String> getMetadata(Map<String, List<String>> headers);
}
