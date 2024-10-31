package org.matsim.api;


/**
 * Factory for creating {@link LP}s.
 */
public interface LPProvider {

    /**
     * Create the partition of a particular lp.
     *
     * @param part id of part to create
     */
    LP create(int part);

}
