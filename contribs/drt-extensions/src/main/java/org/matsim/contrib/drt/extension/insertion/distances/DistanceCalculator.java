package org.matsim.contrib.drt.extension.insertion.distances;

import org.matsim.api.core.v01.network.Link;

public interface DistanceCalculator {
	double calculateDistance(double departureTime, Link fromLink, Link toLink);
}
