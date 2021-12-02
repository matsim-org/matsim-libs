package org.matsim.contrib.drt.extension.alonso_mora.travel_time;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.insertion.DetourTimeEstimator;

/**
 * This implementation of the TravelTimeEstimator for the Alonso-Mora algorithm
 * makes use of the standard DRT functionality in estimating delays. It wraps
 * around DRT's DetourTimeEstimator. Note that usually, DetourTimeEstimator is
 * zone based, which may not be ideal when operating the Alonso-Mora algorithm
 * as departure and arrival times will be strongly rounded. Should be used in
 * parallel with our functionality to mitigate constraint violations.
 * 
 * @author sebhoerl
 */
public class DrtDetourTravelTimeEstimator implements TravelTimeEstimator {
	static public final String TYPE = "DrtDetour";
	
	private final DetourTimeEstimator delegate;

	public DrtDetourTravelTimeEstimator(DetourTimeEstimator delegate) {
		this.delegate = delegate;
	}

	@Override
	public double estimateTravelTime(Link fromLink, Link toLink, double departureTime, double arrivalTimeThreshold) {
		return delegate.estimateTime(fromLink, toLink);
	}
}
