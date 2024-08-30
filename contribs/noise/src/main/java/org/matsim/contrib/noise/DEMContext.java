package org.matsim.contrib.noise;


import org.geotools.api.geometry.Position;

public interface DEMContext {

    /**
     * Returns the elevation at the given position.
     */
    float getElevation(Position position);

}
