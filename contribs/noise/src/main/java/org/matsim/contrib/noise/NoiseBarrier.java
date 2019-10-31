package org.matsim.contrib.noise;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;

/**
 * Used for noise shielding calculations.
 *
 * @author nkuehnel
 *
 */
interface NoiseBarrier {

    Geometry getGeometry();

    double getHeight();

    Id<NoiseBarrier> getId();
}
