package org.matsim.contrib.drt.extension.insertion.distances;

public interface DistanceApproximator extends DistanceCalculator {
	static public DistanceApproximator NULL = (departureTime, fromLink, toLink) -> {
		throw new IllegalStateException(
				"The NULL DistanceApproximator is only used as a tag for not using any approximation.");
	};
}
