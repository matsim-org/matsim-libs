package org.matsim.contrib.noise.data;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Used for noise shielding calculations.
 *
 * @author nkuehnel
 *
 */
public interface NoiseBarrier {

    Geometry getGeometry();

    double getHeight();
}
