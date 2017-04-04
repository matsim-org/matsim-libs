// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.interfaces;

import playground.clruch.jmapviewer.Tile;

/**
 * Interface for implementing a tile loader. Tiles are usually loaded via HTTP
 * or from a file.
 *
 * @author Jan Peter Stotz
 */
public interface TileLoader {

    /**
     * A typical implementation of this function should create and return a
     * new {@link TileJob} instance that performs the load action.
     *
     * @param tile the tile to be loaded
     * @return {@link TileJob} implementation that performs the desired load
     *          action.
     */
    TileJob createTileLoaderJob(Tile tile);

    /**
     * cancels all outstanding tasks in the queue. This should rollback the state of the tiles in the queue
     * to loading = false / loaded = false
     */
    void cancelOutstandingTasks();
}
