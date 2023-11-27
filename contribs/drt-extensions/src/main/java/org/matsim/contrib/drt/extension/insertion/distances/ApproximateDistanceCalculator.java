package org.matsim.contrib.drt.extension.insertion.distances;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;

public class ApproximateDistanceCalculator implements DistanceCalculator {
	private final double distanceEstimationFactor;

	public ApproximateDistanceCalculator(double distanceEstimationFactor) {
		this.distanceEstimationFactor = distanceEstimationFactor;
	}

	@Override
	public double estimateDistance(double departureTime, Link fromLink, Link toLink) {
		return distanceEstimationFactor
				* CoordUtils.calcEuclideanDistance(fromLink.getFromNode().getCoord(), toLink.getFromNode().getCoord());
	}
}
