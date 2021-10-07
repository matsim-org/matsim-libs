package org.matsim.contrib.freight.jsprit;

import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

/**
 * @author: Steffen Axer
 */
public interface VRPTransportCosts extends VehicleRoutingTransportCosts {
    LeastCostPathCalculator getRouter();
    Network getNetwork();
    TravelTime getTravelTime();
}
