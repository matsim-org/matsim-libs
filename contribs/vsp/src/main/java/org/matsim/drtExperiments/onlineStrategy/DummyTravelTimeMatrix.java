package org.matsim.drtExperiments.onlineStrategy;

import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;

/**
 * When running fully offline optimization, bind this travel time matrix to online solver (i.e., which will not be used)
 */
public class DummyTravelTimeMatrix implements TravelTimeMatrix {
    @Override
    public int getTravelTime(Node node, Node node1, double v) {
        return 0;
    }
}
