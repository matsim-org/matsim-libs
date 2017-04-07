// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

import playground.clruch.jmapviewer.interfaces.TileCache;
import playground.clruch.jmapviewer.interfaces.TileSource;

/**
 * Holds one map tile. Additionally the code for loading the tile image and
 * painting it is also included in this class.
 *
 * @author Jan Peter Stotz
 */
public class Tile {

    /**
     * Hourglass image that is displayed until a map tile has been loaded, except for overlay sources
     */
    public static final BufferedImage LOADING_IMAGE = loadImage("images/hourglass.png");

    /**
     * Red cross image that is displayed after a loading error, except for overlay sources
     */
    public static final BufferedImage ERROR_IMAGE = loadImage("images/error.png");

    protected TileSource source;
    protected int xtile;
    protected int ytile;
    protected int zoom;
    protected BufferedImage image;
    protected String key;
    protected volatile boolean loaded; // field accessed by multiple threads without any monitors, needs to be volatile
    protected volatile boolean loading;
    protected volatile boolean error;
    protected String error_message;

    /** TileLoader-specific tile metadata */
    protected Map<String, String> metadata;

    /**
     * Creates a tile with empty image.
     *
     * @param source Tile source
     * @param xtile X coordinate
     * @param ytile Y coordinate
     * @param zoom Zoom level
     */
    public Tile(TileSource source, int xtile, int ytile, int zoom) {
        this(source, xtile, ytile, zoom, LOADING_IMAGE);
    }

    /**
     * Creates a tile with specified image.
     *
     * @param source Tile source
     * @param xtile X coordinate
     * @param ytile Y coordinate
     * @param zoom Zoom level
     * @param image Image content
     */
    public Tile(TileSource source, int xtile, int ytile, int zoom, BufferedImage image) {
        this.source = source;
        this.xtile = xtile;
        this.ytile = ytile;
        this.zoom = zoom;
        this.image = image;
        this.key = getTileKey(source, xtile, ytile, zoom);
    }

    private static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(MapComponent.class.getResourceAsStream(path));
        } catch (IOException | IllegalArgumentException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static class CachedCallable<V> implements Callable<V> {
        private V result;
        private Callable<V> callable;

        /**
         * Wraps callable so it is evaluated only once
         * @param callable to cache
         */
        CachedCallable(Callable<V> callable) {
            this.callable = callable;
        }

        @Override
        public synchronized V call() {
            try {
                if (result == null) {
                    result = callable.call();
                }
                return result;
            } catch (Exception e) {
                // this should not happen here
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Tries to get tiles of a lower or higher zoom level (one or two level
     * difference) from cache and use it as a placeholder until the tile has been loaded.
     * @param cache Tile cache
     */
    public void loadPlaceholderFromCache(TileCache cache) {
        /*
         *  use LazyTask as creation of BufferedImage is very expensive
         *  this way we can avoid object creation until we're sure it's needed
         */
        final CachedCallable<BufferedImage> tmpImage = new CachedCallable<>(new Callable<BufferedImage>() {
            @Override
            public BufferedImage call() throws Exception {
                return new BufferedImage(source.getTileSize(), source.getTileSize(), BufferedImage.TYPE_INT_ARGB);
            }
        });

        for (int zoomDiff = 1; zoomDiff < 5; zoomDiff++) {
            // first we check if there are already the 2^x tiles
            // of a higher detail level
            int zoomHigh = zoom + zoomDiff;
            if (zoomDiff < 3 && zoomHigh <= MapComponent.MAX_ZOOM) {
                int factor = 1 << zoomDiff;
                int xtileHigh = xtile << zoomDiff;
                int ytileHigh = ytile << zoomDiff;
                final double scale = 1.0 / factor;

                /*
                 * use LazyTask for graphics to avoid evaluation of tmpImage, until we have
                 * something to draw
                 */
                CachedCallable<Graphics2D> graphics = new CachedCallable<>(new Callable<Graphics2D>() {
                    @Override
                    public Graphics2D call() throws Exception {
                        Graphics2D g = (Graphics2D) tmpImage.call().getGraphics();
                        g.setTransform(AffineTransform.getScaleInstance(scale, scale));
                        return g;
                    }
                });

                int paintedTileCount = 0;
                for (int x = 0; x < factor; x++) {
                    for (int y = 0; y < factor; y++) {
                        Tile tile = cache.getTile(source, xtileHigh + x, ytileHigh + y, zoomHigh);
                        if (tile != null && tile.isLoaded()) {
                            paintedTileCount++;
                            tile.paint(graphics.call(), x * source.getTileSize(), y * source.getTileSize());
                        }
                    }
                }
                if (paintedTileCount == factor * factor) {
                    image = tmpImage.call();
                    return;
                }
            }

            int zoomLow = zoom - zoomDiff;
            if (zoomLow >= MapComponent.MIN_ZOOM) {
                int xtileLow = xtile >> zoomDiff;
                int ytileLow = ytile >> zoomDiff;
                final int factor = 1 << zoomDiff;
                final double scale = factor;
                CachedCallable<Graphics2D> graphics = new CachedCallable<>(new Callable<Graphics2D>() {
                    @Override
                    public Graphics2D call() throws Exception {
                        Graphics2D g = (Graphics2D) tmpImage.call().getGraphics();
                        AffineTransform at = new AffineTransform();
                        int translateX = (xtile % factor) * source.getTileSize();
                        int translateY = (ytile % factor) * source.getTileSize();
                        at.setTransform(scale, 0, 0, scale, -translateX, -translateY);
                        g.setTransform(at);
                        return g;
                    }

                });

                Tile tile = cache.getTile(source, xtileLow, ytileLow, zoomLow);
                if (tile != null && tile.isLoaded()) {
                    tile.paint(graphics.call(), 0, 0);
                    image = tmpImage.call();
                    return;
                }
            }
        }
    }

    public TileSource getSource() {
        return source;
    }

    /**
     * Returns the X coordinate.
     * @return tile number on the x axis of this tile
     */
    public int getXtile() {
        return xtile;
    }

    /**
     * Returns the Y coordinate.
     * @return tile number on the y axis of this tile
     */
    public int getYtile() {
        return ytile;
    }

    /**
     * Returns the zoom level.
     * @return zoom level of this tile
     */
    public int getZoom() {
        return zoom;
    }

    /**
     * @return tile indexes of the top left corner as TileXY object
     */
    public TileXY getTileXY() {
        return new TileXY(xtile, ytile);
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public void loadImage(InputStream input) throws IOException {
        image = ImageIO.read(input);
    }

    /**
     * @return key that identifies a tile
     */
    public String getKey() {
        return key;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public String getUrl() throws IOException {
        return source.getTileUrl(zoom, xtile, ytile);
    }

    /**
     * Paints the tile-image on the {@link Graphics} <code>g</code> at the
     * position <code>x</code>/<code>y</code>.
     *
     * @param g the Graphics object
     * @param x x-coordinate in <code>g</code>
     * @param y y-coordinate in <code>g</code>
     */
    public void paint(Graphics g, int x, int y) {
        if (image == null)
            return;
        g.drawImage(image, x, y, null);
    }

    /**
     * Paints the tile-image on the {@link Graphics} <code>g</code> at the
     * position <code>x</code>/<code>y</code>.
     *
     * @param g the Graphics object
     * @param x x-coordinate in <code>g</code>
     * @param y y-coordinate in <code>g</code>
     * @param width width that tile should have
     * @param height height that tile should have
     */
    public void paint(Graphics g, int x, int y, int width, int height) {
        if (image == null)
            return;
        g.drawImage(image, x, y, width, height, null);
    }

    @Override
    public String toString() {
        return "Tile " + key;
    }

    /**
     * Note that the hash code does not include the {@link #source}.
     * Therefore a hash based collection can only contain tiles
     * of one {@link #source}.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + xtile;
        result = prime * result + ytile;
        result = prime * result + zoom;
        return result;
    }

    /**
     * Compares this object with <code>obj</code> based on
     * the fields {@link #xtile}, {@link #ytile} and
     * {@link #zoom}.
     * The {@link #source} field is ignored.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tile other = (Tile) obj;
        if (xtile != other.xtile)
            return false;
        if (ytile != other.ytile)
            return false;
        if (zoom != other.zoom)
            return false;
        if (!getTileSource().equals(other.getTileSource())) {
            return false;
        }
        return true;
    }

    public static String getTileKey(TileSource source, int xtile, int ytile, int zoom) {
        return zoom + "/" + xtile + "/" + ytile + "@" + source.getName();
    }

    public String getStatus() {
        if (this.error)
            return "error";
        if (this.loaded)
            return "loaded";
        if (this.loading)
            return "loading";
        return "new";
    }

    public boolean hasError() {
        return error;
    }

    public String getErrorMessage() {
        return error_message;
    }

    public void setError(Exception e) {
        setError(e.toString());
    }

    public void setError(String message) {
        error = true;
        setImage(ERROR_IMAGE);
        error_message = message;
    }

    /**
     * Puts the given key/value pair to the metadata of the tile.
     * If value is null, the (possibly existing) key/value pair is removed from
     * the meta data.
     *
     * @param key Key
     * @param value Value
     */
    public void putValue(String key, String value) {
        if (value == null || value.isEmpty()) {
            if (metadata != null) {
                metadata.remove(key);
            }
            return;
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    /**
     * returns the metadata of the Tile
     *
     * @param key metadata key that should be returned
     * @return null if no such metadata exists, or the value of the metadata
     */
    public String getValue(String key) {
        if (metadata == null) return null;
        return metadata.get(key);
    }

    /**
     *
     * @return metadata of the tile
     */
    public Map<String, String> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    /**
     * indicate that loading process for this tile has started
     */
    public void initLoading() {
        error = false;
        loading = true;
    }

    /**
     * indicate that loading process for this tile has ended
     */
    public void finishLoading() {
        loading = false;
        loaded = true;
    }

    /**
     *
     * @return TileSource from which this tile comes
     */
    public TileSource getTileSource() {
        return source;
    }

    /**
     * indicate that loading process for this tile has been canceled
     */
    public void loadingCanceled() {
        loading = false;
        loaded = false;
    }

}
