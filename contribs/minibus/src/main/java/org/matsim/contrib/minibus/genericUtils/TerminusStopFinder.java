package org.matsim.contrib.minibus.genericUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TerminusStopFinder {
	
	/**
	 * Find the 2nd terminus stop (1st terminus is at index 0 per definition).
	 * 
	 * Returns stop index instead of the stop, in order to cater for stops which are
	 * served multiple times
	 * 
	 * @param stops
	 * @return index of the stop which is half way on the route from start stop over
	 *         all stops back to the start stop
	 */
	public static final int findStopIndexWithLargestDistance(ArrayList<TransitStopFacility> stops) {
		double totalDistance = 0;
		Map<Integer, Double> distFromStart2StopIndex = new HashMap<>();
		TransitStopFacility previousStop = stops.get(0);

		for (int i = 0; i < stops.size(); i++) {
			TransitStopFacility currentStop = stops.get(i);
			totalDistance = totalDistance
					+ CoordUtils.calcEuclideanDistance(previousStop.getCoord(), currentStop.getCoord());
			distFromStart2StopIndex.put(i, totalDistance);
			previousStop = currentStop;
		}
		// add leg from last to first stop
		totalDistance = totalDistance
				+ CoordUtils.calcEuclideanDistance(previousStop.getCoord(), stops.get(0).getCoord());

		// first terminus is first stop in stops, other terminus is stop half way on the
		// circular route beginning at the first stop
		for (int i = 1; i < stops.size(); i++) {
			if (distFromStart2StopIndex.get(i) >= totalDistance / 2) {
				if (Math.abs(totalDistance / 2 - distFromStart2StopIndex.get(i - 1)) > Math
						.abs(totalDistance / 2 - distFromStart2StopIndex.get(i))) {
					return i;
				} else {
					return i - 1;
				}
			}
		}

		return 0;
	}

}
