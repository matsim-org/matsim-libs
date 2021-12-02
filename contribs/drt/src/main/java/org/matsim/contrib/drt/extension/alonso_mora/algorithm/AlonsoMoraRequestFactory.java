package org.matsim.contrib.drt.extension.alonso_mora.algorithm;

import java.util.Collection;

import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * Creates an aggregated request for the algorithm based on a list of
 * aggregateable individual DRT requests.
 * 
 * @author sebhoerl
 */
public interface AlonsoMoraRequestFactory {
	AlonsoMoraRequest createRequest(Collection<DrtRequest> requests, double directArrvialTime, double earliestDepartureTime,
			double directRideDistance);
}
