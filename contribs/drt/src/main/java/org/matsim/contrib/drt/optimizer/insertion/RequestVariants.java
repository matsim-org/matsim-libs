package org.matsim.contrib.drt.optimizer.insertion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
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
	 * then one variant per further combination of pickup and dropoff stop, skipping combinations on the same link and
	 * combinations whose latest arrival is not after their earliest start (no travel budget between the two stops) and
	 * pickup stops the passenger cannot reach before the latest pickup of the request.
	 * Candidates on a link that already occurs (the request's own link or an earlier candidate) are ignored, so each
	 * pair of links is evaluated once; the first candidate per link wins.
	 * Without stop candidates, a single variant wrapping the request.
	 * <p>
	 * Time windows of a variant with pickup stop a and dropoff stop e (walk = walking duration):
	 * <ul>
	 * <li>departure at the origin = earliest start of the request - walk(own pickup stop)</li>
	 * <li>latest arrival at the destination = latest arrival of the request + walk(own dropoff stop)</li>
	 * <li>earliest start = departure at the origin + walk(a) (arrival at the stop)</li>
	 * <li>latest start = latest start of the request, i.e. the same for all variants: walking farther does not allow a
	 * later pickup, the longer walk is taken from the waiting budget (max wait = latest start - earliest start)</li>
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

		List<DrtStopCandidate> access = firstPerLink(
				new DrtStopCandidate(request.getFromLink(), c.accessWalkDuration()), c.alternativeAccess());
		List<DrtStopCandidate> egress = firstPerLink(
				new DrtStopCandidate(request.getToLink(), c.egressWalkDuration()), c.alternativeEgress());

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
				if (latestArrival <= earliestStart) {
					continue; // no travel budget between the two stops: never feasible, skip the path calculation
				}
				double maxWait = request.getLatestStartTime() - earliestStart;
				if (maxWait < 0) {
					continue; // the passenger cannot reach the pickup stop before the latest pickup
				}
				DrtRequest variantRequest = DrtRequest.newBuilder(request)
						.stopCandidates(null)
						.fromLink(pickup.link())
						.toLink(dropoff.link())
						.earliestDepartureTime(earliestStart)
						.constraints(withMaxDurations(request.getConstraints(), latestArrival - earliestStart, maxWait))
						.build();
				variants.add(new RequestVariant(variantRequest, pickup.walkDuration(), dropoff.walkDuration(),
						variants.size()));
			}
		}
		return List.copyOf(variants);
	}

	private static List<DrtStopCandidate> firstPerLink(DrtStopCandidate own, List<DrtStopCandidate> alternatives) {
		List<DrtStopCandidate> candidates = new ArrayList<>(alternatives.size() + 1);
		Set<Id<Link>> links = new HashSet<>();
		candidates.add(own);
		links.add(own.link().getId());
		for (DrtStopCandidate candidate : alternatives) {
			if (links.add(candidate.link().getId())) {
				candidates.add(candidate);
			}
		}
		return candidates;
	}

	private static DrtRouteConstraints withMaxDurations(DrtRouteConstraints c, double maxTravelDuration,
			double maxWaitDuration) {
		return new DrtRouteConstraints(maxTravelDuration, c.maxRideDuration(), maxWaitDuration, c.maxPickupDelay(),
				c.lateDiversionThreshold(), c.allowRejection());
	}
}
