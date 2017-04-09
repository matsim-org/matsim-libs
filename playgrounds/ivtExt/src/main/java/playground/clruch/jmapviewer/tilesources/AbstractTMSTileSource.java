// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.tilesources;

import java.awt.Point;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import playground.clruch.jmapviewer.OsmMercator;
import playground.clruch.jmapviewer.Tile;
import playground.clruch.jmapviewer.TileXY;
import playground.clruch.jmapviewer.interfaces.ICoordinate;

/**
 * Class generalizing all tile based tile sources
 *
 * @author Wiktor Niesiobędzki
 *
 */
abstract class AbstractTMSTileSource extends AbstractTileSource {

    protected String name;
    protected String baseUrl;
    protected String id;
    private final Map<String, Set<String>> noTileHeaders;
    private final Map<String, Set<String>> noTileChecksums;
    private final Map<String, String> metadataHeaders;
    protected int tileSize;

    /**
     * Creates an instance based on TileSource information
     *
     * @param info
     *            description of the Tile Source
     */
    public AbstractTMSTileSource(TileSourceInfo info) {
        this.name = info.getName();
        this.baseUrl = info.getUrl();
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.id = info.getUrl();
        this.noTileHeaders = info.getNoTileHeaders();
        this.noTileChecksums = info.getNoTileChecksums();
        this.metadataHeaders = info.getMetadataHeaders();
        this.tileSize = info.getTileSize();
    }

    /**
     * @return default tile size to use, when not set in Imagery Preferences
     */
    @Override
    public int getDefaultTileSize() {
        return OsmMercator.DEFAUL_TILE_SIZE;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getMaxZoom() {
        return 21;
    }

    @Override
    public int getMinZoom() {
        return 0;
    }

    /**
     * @return image extension, used for URL creation
     */
    public String getExtension() {
        return "png";
    }

    /**
     * @param zoom
     *            level of the tile
     * @param tilex
     *            tile number in x axis
     * @param tiley
     *            tile number in y axis
     * @return String containg path part of URL of the tile
     * @throws IOException
     *             when subclass cannot return the tile URL
     */
    public String getTilePath(int zoom, int tilex, int tiley) throws IOException {
        return "/" + zoom + "/" + tilex + "/" + tiley + "." + getExtension();
    }

    /**
     * @return Base part of the URL of the tile source
     */
    public String getBaseUrl() {
        return this.baseUrl;
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) throws IOException {
        return this.getBaseUrl() + getTilePath(zoom, tilex, tiley);
    }

    @Override
    public String toString() {
        return getName();
    }

    /*
     * Most tilesources use OsmMercator projection.
     */
    @Override
    public int getTileSize() {
        if (tileSize <= 0) {
            return getDefaultTileSize();
        }
        return tileSize;
    }

    @Override
    public Point latLonToXY(ICoordinate point, int zoom) {
        return latLonToXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public ICoordinate xyToLatLon(Point point, int zoom) {
        return xyToLatLon(point.x, point.y, zoom);
    }

    @Override
    public TileXY latLonToTileXY(ICoordinate point, int zoom) {
        return latLonToTileXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public ICoordinate tileXYToLatLon(TileXY xy, int zoom) {
        return tileXYToLatLon(xy.getXIndex(), xy.getYIndex(), zoom);
    }

    @Override
    public ICoordinate tileXYToLatLon(Tile tile) {
        return tileXYToLatLon(tile.getXtile(), tile.getYtile(), tile.getZoom());
    }

    @Override
    public int getTileXMax(int zoom) {
        return getTileMax(zoom);
    }

    @Override
    public int getTileXMin(int zoom) {
        return 0;
    }

    @Override
    public int getTileYMax(int zoom) {
        return getTileMax(zoom);
    }

    @Override
    public int getTileYMin(int zoom) {
        return 0;
    }

    @Override
    public boolean isNoTileAtZoom(Map<String, List<String>> headers, int statusCode, byte[] content) {
        if (noTileHeaders != null && headers != null) {
            for (Entry<String, Set<String>> searchEntry : noTileHeaders.entrySet()) {
                List<String> headerVals = headers.get(searchEntry.getKey());
                if (headerVals != null) {
                    for (String headerValue : headerVals) {
                        for (String val : searchEntry.getValue()) {
                            if (headerValue.matches(val)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        if (noTileChecksums != null && content != null) {
            for (Entry<String, Set<String>> searchEntry : noTileChecksums.entrySet()) {
                MessageDigest md = null;
                try {
                    md = MessageDigest.getInstance(searchEntry.getKey());
                } catch (NoSuchAlgorithmException e) {
                    break;
                }
                byte[] byteDigest = md.digest(content);
                final int len = byteDigest.length;

                char[] hexChars = new char[len * 2];
                for (int i = 0, j = 0; i < len; i++) {
                    final int v = byteDigest[i];
                    int vn = (v & 0xf0) >> 4;
                    hexChars[j++] = (char) (vn + (vn >= 10 ? 'a' - 10 : '0'));
                    vn = (v & 0xf);
                    hexChars[j++] = (char) (vn + (vn >= 10 ? 'a' - 10 : '0'));
                }
                for (String val : searchEntry.getValue()) {
                    if (new String(hexChars).equalsIgnoreCase(val)) {
                        return true;
                    }
                }
            }
        }
        return super.isNoTileAtZoom(headers, statusCode, content);
    }

    @Override
    public Map<String, String> getMetadata(Map<String, List<String>> headers) {
        Map<String, String> ret = new HashMap<>();
        if (metadataHeaders != null && headers != null) {
            for (Entry<String, String> searchEntry : metadataHeaders.entrySet()) {
                List<String> headerVals = headers.get(searchEntry.getKey());
                if (headerVals != null) {
                    for (String headerValue : headerVals) {
                        ret.put(searchEntry.getValue(), headerValue);
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public String getTileId(int zoom, int tilex, int tiley) {
        return this.baseUrl + "/" + zoom + "/" + tilex + "/" + tiley;
    }

    private static int getTileMax(int zoom) {
        return (int) Math.pow(2.0, zoom) - 1;
    }
}
