package org.matsim.contrib.drt.optimizer.insertion;

import java.util.Objects;

import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * One combination of pickup and dropoff stop of a request, as evaluated by the insertion search. The carried
 * {@link DrtRequest} has the same id and passengers as the submitted request, but the links and time windows of these
 * stops. Created by {@link RequestVariants#of(DrtRequest)}.
 *
 * @param request            request with the links and time windows of this combination of stops
 * @param accessWalkDuration walking duration from the origin to the pickup stop
 * @param egressWalkDuration walking duration from the dropoff stop to the destination
 * @param index              position in the list of variants; 0 is the submitted request with its own stops
 */
public record RequestVariant(DrtRequest request, double accessWalkDuration, double egressWalkDuration, int index) {
	public RequestVariant {
		Objects.requireNonNull(request, "request");
	}

	/**
	 * The submitted request itself, without walking information.
	 */
	public static RequestVariant of(DrtRequest request) {
		return new RequestVariant(request, 0, 0, 0);
	}

	public double walkDuration() {
		return accessWalkDuration + egressWalkDuration;
	}
}
