package org.matsim.contrib.noise;

import org.opengis.geometry.DirectPosition;

public interface DEMContext {

    /**
     * Returns the elevation at the given position.
     */
    float getElevation(DirectPosition position);

}
