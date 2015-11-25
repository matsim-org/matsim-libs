package playground.boescpa.analysis.trips.tripCreation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import playground.boescpa.analysis.spatialCutters.SpatialCutter;

/**
 * Returns, given a certain spatial cutter, if a trip is in the area or not.
 *
 * @author boescpa
 */
public class SpatialTripCutter {
    private final SpatialCutter spatialCutter;

    public SpatialTripCutter(SpatialCutter spatialCutter) {
        this.spatialCutter = spatialCutter;
    }

    /**
     * A trip is considered "in the area" if the start, the end or both are in the area.
     * Explicitly not considered are trips which only pass trough an area.
     *
     * @param network
     * @param startLink
     * @param endLink
     * @return true if the trip is considered in the area. false else.
     */
    public boolean spatiallyConsideringTrip(Network network, Id startLink, Id endLink) {
        Link sLink = network.getLinks().get(startLink);
        Link eLink = network.getLinks().get(endLink); // could be null!

        return (spatialCutter.spatiallyConsideringLink(sLink) || (eLink != null && spatialCutter.spatiallyConsideringLink(eLink)));
    }
}
