// License: GPL. For details, see Readme.txt file.
package playground.clib.jmapviewer;

import playground.clib.jmapviewer.interfaces.TileCache;
import playground.clib.jmapviewer.interfaces.TileLoader;
import playground.clib.jmapviewer.interfaces.TileLoaderListener;
import playground.clib.jmapviewer.interfaces.TileSource;

public class TileController {
    protected TileLoader tileLoader;
    protected TileCache tileCache;
    protected TileSource tileSource;

    public TileController(TileSource source, TileCache tileCache, TileLoaderListener listener) {
        this.tileSource = source;
        this.tileLoader = new OsmTileLoader(listener);
        this.tileCache = tileCache;
    }

    /**
     * retrieves a tile from the cache. If the tile is not present in the cache
     * a load job is added to the working queue of {@link TileLoader}.
     *
     * @param tilex the X position of the tile
     * @param tiley the Y position of the tile
     * @param zoom the zoom level of the tile
     * @return specified tile from the cache or <code>null</code> if the tile
     *         was not found in the cache.
     */
    public Tile getTile(int tilex, int tiley, int zoom) {
        int max = 1 << zoom;
        if (tilex < 0 || tilex >= max || tiley < 0 || tiley >= max)
            return null;
        Tile tile = tileCache.getTile(tileSource, tilex, tiley, zoom);
        if (tile == null) {
            tile = new Tile(tileSource, tilex, tiley, zoom);
            tileCache.addTile(tile);
            tile.loadPlaceholderFromCache(tileCache);
        }
        if (tile.error) {
            tile.loadPlaceholderFromCache(tileCache);
        }
        if (!tile.isLoaded()) {
            tileLoader.createTileLoaderJob(tile).submit();
        }
        return tile;
    }

    public TileCache getTileCache() {
        return tileCache;
    }

    public void setTileCache(TileCache tileCache) {
        this.tileCache = tileCache;
    }

    public TileLoader getTileLoader() {
        return tileLoader;
    }

    public void setTileLoader(TileLoader tileLoader) {
        this.tileLoader = tileLoader;
    }

    public TileSource getTileLayerSource() {
        return tileSource;
    }

    public TileSource getTileSource() {
        return tileSource;
    }

    public void setTileSource(TileSource tileSource) {
        this.tileSource = tileSource;
    }

    /**
     * Removes all jobs from the queue that are currently not being processed.
     *
     */
    public void cancelOutstandingJobs() {
        tileLoader.cancelOutstandingTasks();
    }
}
