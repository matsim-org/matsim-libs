package playground.boescpa.analysis.scenarioAnalyzer.spatialFilters;

import org.matsim.api.core.v01.network.Link;

/**
 * NoFilter returns TRUE for all trips and thus does not cut any trip.
 *
 * @author boescpa
 */
public class NoCutting implements SpatialEventCutter {
    @Override
    public boolean spatiallyConsideringLink(Link link) {
        return true;
    }

    public String toString() {
        return "No spatial event cutter loaded. Use full network.";
    }
}
