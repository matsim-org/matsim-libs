package org.matsim.contrib.drt.extension.alonso_mora.algorithm;

import java.util.Collection;

import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * Default implementation for creating a request for the algorithm by
 * Alonso-Mora et al. from a {#link DrtRequest}. It covers setting the maximum
 * allowable queue time for this request.
 * 
 * @author sebhoerl
 */
public class DefaultAlonsoMoraRequestFactory implements AlonsoMoraRequestFactory {
	private final double maximumQueueTime;

	public DefaultAlonsoMoraRequestFactory(double maximumQueueTime) {
		this.maximumQueueTime = maximumQueueTime;
	}

	@Override
	public AlonsoMoraRequest createRequest(Collection<DrtRequest> requests, double directArrvialTime, double earliestDepartureTime,
			double directRideDistance) {
		double latestAssignmentTime = earliestDepartureTime + maximumQueueTime;
		double latestPickupTime = requests.stream().mapToDouble(r -> r.getLatestStartTime()).min()
				.orElse(Double.POSITIVE_INFINITY);
		latestAssignmentTime = Math.min(latestAssignmentTime, latestPickupTime);

		return new DefaultAlonsoMoraRequest(requests, latestAssignmentTime, directArrvialTime, directRideDistance);
	}
}
