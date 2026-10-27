package org.matsim.contrib.drt.optimizer.insertion;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.passenger.DrtStopCandidate;
import org.matsim.contrib.drt.passenger.DrtStopCandidates;

/**
 * Turns the stop candidates of a request into the request variants evaluated by the insertion search.
 */
public final class RequestVariants {
	private RequestVariants() {
	}

	/**
	 * Returns all variants of the request as an immutable list. Index 0 is the request itself (its own from/to link);
	 * then one variant per further combination of pickup and dropoff stop, skipping combinations on the same link.
	 * Without stop candidates, a single variant wrapping the request.
	 * <p>
	 * Time windows of a variant with pickup stop a and dropoff stop e (walk = walking duration):
	 * <ul>
	 * <li>departure at the origin = earliest start of the request - walk(own pickup stop)</li>
	 * <li>latest arrival at the destination = latest arrival of the request + walk(own dropoff stop)</li>
	 * <li>earliest start = departure at the origin + walk(a); max wait counts from there (arrival at the stop)</li>
	 * <li>latest arrival = latest arrival at the destination - walk(e)</li>
	 * <li>max travel duration = latest arrival - earliest start (budget between the two stops)</li>
	 * </ul>
	 * Other constraints are those of the request.
	 */
	public static List<RequestVariant> of(DrtRequest request) {
		var candidates = request.getStopCandidates();
		if (candidates.isEmpty()) {
			return List.of(RequestVariant.of(request));
		}
		DrtStopCandidates c = candidates.get();

		List<DrtStopCandidate> access = new ArrayList<>();
		access.add(new DrtStopCandidate(request.getFromLink(), c.accessWalkDuration()));
		access.addAll(c.alternativeAccess());
		List<DrtStopCandidate> egress = new ArrayList<>();
		egress.add(new DrtStopCandidate(request.getToLink(), c.egressWalkDuration()));
		egress.addAll(c.alternativeEgress());

		double departureAtOrigin = request.getEarliestStartTime() - c.accessWalkDuration();
		double latestArrivalAtDestination = request.getLatestArrivalTime() + c.egressWalkDuration();

		List<RequestVariant> variants = new ArrayList<>();
		for (int a = 0; a < access.size(); a++) {
			for (int e = 0; e < egress.size(); e++) {
				DrtStopCandidate pickup = access.get(a);
				DrtStopCandidate dropoff = egress.get(e);
				if (a == 0 && e == 0) {
					variants.add(new RequestVariant(request, pickup.walkDuration(), dropoff.walkDuration(), 0));
					continue;
				}
				if (pickup.link() == dropoff.link()) {
					continue;
				}
				double earliestStart = departureAtOrigin + pickup.walkDuration();
				double latestArrival = latestArrivalAtDestination - dropoff.walkDuration();
				DrtRequest variantRequest = DrtRequest.newBuilder(request)
						.stopCandidates(null)
						.fromLink(pickup.link())
						.toLink(dropoff.link())
						.earliestDepartureTime(earliestStart)
						.constraints(withMaxTravelDuration(request.getConstraints(), latestArrival - earliestStart))
						.build();
				variants.add(new RequestVariant(variantRequest, pickup.walkDuration(), dropoff.walkDuration(),
						variants.size()));
			}
		}
		return List.copyOf(variants);
	}

	private static DrtRouteConstraints withMaxTravelDuration(DrtRouteConstraints c, double maxTravelDuration) {
		return new DrtRouteConstraints(maxTravelDuration, c.maxRideDuration(), c.maxWaitDuration(), c.maxPickupDelay(),
				c.lateDiversionThreshold(), c.allowRejection());
	}
}
