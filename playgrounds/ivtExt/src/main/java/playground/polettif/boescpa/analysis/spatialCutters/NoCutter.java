package playground.polettif.boescpa.analysis.spatialCutters;

import org.matsim.api.core.v01.network.Link;

/**
 * NoFilter returns TRUE for all trips and thus does not cut any trip.
 *
 * @author boescpa
 */
public class NoCutter implements SpatialCutter {
    @Override
    public boolean spatiallyConsideringLink(Link link) {
        return true;
    }

    public String toString() {
        return "No spatial cutter loaded. Use full network.";
    }
}
