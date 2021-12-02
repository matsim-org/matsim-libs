package org.matsim.contrib.drt.extension.alonso_mora.travel_time;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Simple implementation for travel time estimation. Given a the Euclidean
 * distance between two points, the travel time is estimated using a distance
 * factor and an average speed.
 * 
 * @author sebhoerl
 */
public class EuclideanTravelTimeEstimator implements TravelTimeEstimator {
	static public final String TYPE = "Euclidean";
	
	private final double speed; // m/s
	private final double distanceFactor;

	/**
	 * @param distanceFactor Euclidean distance is multiplied by this factor
	 * @param speed          Given in m/s
	 */
	public EuclideanTravelTimeEstimator(double distanceFactor, double speed) {
		this.distanceFactor = distanceFactor;
		this.speed = speed;
	}

	@Override
	public double estimateTravelTime(Link fromLink, Link toLink, double departureTime, double arrivalTimeThreshold) {
		return CoordUtils.calcEuclideanDistance(fromLink.getCoord(), toLink.getCoord()) * distanceFactor / speed;
	}
}
