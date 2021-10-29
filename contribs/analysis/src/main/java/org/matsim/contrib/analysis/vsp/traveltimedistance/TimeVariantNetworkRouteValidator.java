package org.matsim.contrib.analysis.vsp.traveltimedistance;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;

/**
 * Validator that performs the routing on a time variant network.
 */
public class TimeVariantNetworkRouteValidator implements TravelTimeDistanceValidator {

	public TimeVariantNetworkRouteValidator(Network network) {
		// TODO create time variant router
	}


	@Override
	public Tuple<Double, Double> getTravelTime(Coord fromCoord, Coord toCoord, double departureTime, String tripId) {
		return null;
	}

}
