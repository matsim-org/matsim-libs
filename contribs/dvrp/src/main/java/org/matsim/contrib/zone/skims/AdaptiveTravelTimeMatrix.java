package org.matsim.contrib.zone.skims;

import org.matsim.api.core.v01.network.Node;

/**
 * @author steffenaxer
 */
public interface AdaptiveTravelTimeMatrix {
	 double getTravelTime(Node fromNode, Node toNode, double departureTime);
	 void setTravelTime(Node fromNode, Node toNode, double travelTime, double departureTime);
}
