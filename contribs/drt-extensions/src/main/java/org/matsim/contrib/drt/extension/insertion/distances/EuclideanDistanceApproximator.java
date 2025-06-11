package org.matsim.contrib.drt.extension.insertion.distances;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;

public class EuclideanDistanceApproximator implements DistanceApproximator {
	private final double distanceEstimationFactor;

	public EuclideanDistanceApproximator(double distanceEstimationFactor) {
		this.distanceEstimationFactor = distanceEstimationFactor;
	}

	@Override
	public double calculateDistance(double departureTime, Link fromLink, Link toLink) {
		return distanceEstimationFactor
				* CoordUtils.calcEuclideanDistance(fromLink.getFromNode().getCoord(), toLink.getFromNode().getCoord());
	}
}
