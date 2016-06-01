package playground.polettif.boescpa.analysis.trips;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import playground.polettif.boescpa.analysis.spatialCutters.SpatialCutter;

/**
 * Returns, given a certain spatial cutter, if a trip is in the area or not.
 *
 * @author boescpa
 */
public class SpatialTripCutter {
    private final SpatialCutter spatialCutter;
    private final Network network;

    public SpatialTripCutter(SpatialCutter spatialCutter, Network network) {
        this.spatialCutter = spatialCutter;
        this.network = network;
    }

    /**
     * A trip is considered "in the area" if the start, the end or both are in the area.
     * Explicitly not considered are trips which only pass trough an area.
     *
     * @param trip
     * @return true if the trip is considered in the area. false else.
     */
    public boolean spatiallyConsideringTrip(Trip trip) {
        Link sLink = network.getLinks().get(trip.startLinkId);
        Link eLink = network.getLinks().get(trip.endLinkId); // could be null!

        return (spatialCutter.spatiallyConsideringLink(sLink) || (eLink != null && spatialCutter.spatiallyConsideringLink(eLink)));
    }
}
