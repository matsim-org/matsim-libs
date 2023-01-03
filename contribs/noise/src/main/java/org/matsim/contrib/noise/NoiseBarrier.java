package org.matsim.contrib.noise;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;

/**
 * Used for noise shielding calculations.
 *
 * @author nkuehnel
 *
 */
interface NoiseBarrier {

    PreparedGeometry getGeometry();

    double getHeight();

    Id<NoiseBarrier> getId();
}
