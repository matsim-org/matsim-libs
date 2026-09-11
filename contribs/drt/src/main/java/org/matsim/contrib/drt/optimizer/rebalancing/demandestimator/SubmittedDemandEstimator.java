/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */
package org.matsim.contrib.drt.optimizer.rebalancing.demandestimator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/**
 * Aggregates {@link DrtRequestSubmittedEvent}s per iteration for the given mode and returns, as expected demand for the
 * current iteration, a forward-looking rolling window over the previous iteration's submissions.
 * <p>
 * Two deliberate differences from {@link PreviousIterationDrtDemandEstimator}:
 * <ol>
 * <li><b>Submission-based signal, not departure-event-based.</b> This estimator counts {@link DrtRequestSubmittedEvent},
 * which is emitted for <em>every</em> request, including those that are subsequently rejected. The previous-iteration
 * estimator counts {@code PersonDepartureEvent}, which never fires for a rejected (prebooked) request — the agent is
 * aborted on its origin activity and never departs. That makes rejected demand invisible to the rebalancing feedback
 * loop: a zone/time that got rejected contributes zero expected demand next iteration, so no vehicles are sent there,
 * so it is rejected again — a self-reinforcing dead zone. Counting submissions keeps rejected-but-known demand in the
 * signal, which matters most in scenarios dominated by prebooked requests.</li>
 * <li><b>Rolling horizon, binned by intended departure time.</b> Each submission is stored with its
 * {@code earliestDepartureTime} (when the passenger actually wants to travel, not when the booking was placed).
 * {@code getExpectedDemand(fromTime, estimationPeriod)} returns the demand whose departure falls in the exact window
 * {@code [fromTime, fromTime + estimationPeriod)}. As {@code fromTime} advances with the rebalancing clock, the window
 * rolls with it — the rebalancer at time {@code t} pre-positions for the demand expected over the next
 * {@code estimationPeriod} seconds, with no fixed-bin quantization or boundary artifacts.</li>
 * </ol>
 * Implementation: the previous iteration's submissions are held in a single list sorted by departure time, so each
 * window lookup is a binary search to the window start plus a linear walk over only the entries inside the window —
 * cheap even for hundreds of rebalancing calls over tens of thousands of submissions.
 *
 * @author nkuehnel / MOIA
 */
public final class SubmittedDemandEstimator
		implements ZonalDemandEstimator, DrtRequestSubmittedEventHandler, IterationEndsListener {
	private static final Logger logger = LogManager.getLogger(SubmittedDemandEstimator.class);

	private record Submission(double departureTime, Zone zone) {
	}

	private final ZoneSystem zonalSystem;
	private final String mode;

	private List<Submission> currentIterationSubmissions = new ArrayList<>();
	// previous iteration's submissions, sorted ascending by departureTime (see notifyIterationEnds)
	private List<Submission> previousIterationSubmissions = new ArrayList<>();

	public SubmittedDemandEstimator(ZoneSystem zonalSystem, DrtConfigGroup drtCfg) {
		this.zonalSystem = zonalSystem;
		this.mode = drtCfg.getMode();
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		if (event.getMode().equals(mode)) {
			zonalSystem.getZoneForLinkId(event.getFromLinkId()).ifPresentOrElse(
					zone -> currentIterationSubmissions.add(new Submission(event.getEarliestDepartureTime(), zone)),
					//might be that somebody walks into the service area or that service area is larger/different than DrtZonalSystem...
					() -> logger.warn("No zone found for linkId " + event.getFromLinkId().toString()));
		}
	}

	@Override
	public ToDoubleFunction<Zone> getExpectedDemand(double fromTime, double estimationPeriod) {
		double toTime = fromTime + estimationPeriod;
		Map<Zone, Double> aggregated = new HashMap<>();
		// binary search for the first submission with departureTime >= fromTime, then walk until departureTime >= toTime
		int start = lowerBound(previousIterationSubmissions, fromTime);
		for (int i = start; i < previousIterationSubmissions.size(); i++) {
			Submission submission = previousIterationSubmissions.get(i);
			if (submission.departureTime() >= toTime) {
				break;
			}
			aggregated.merge(submission.zone(), 1.0, Double::sum);
		}
		return zone -> aggregated.getOrDefault(zone, 0.0);
	}

	/**
	 * Index of the first element whose departureTime is >= key (or list size if none), on a list sorted ascending by
	 * departureTime.
	 */
	private static int lowerBound(List<Submission> sorted, double key) {
		int lo = 0;
		int hi = sorted.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (sorted.get(mid).departureTime() < key) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		return lo;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		currentIterationSubmissions.sort((a, b) -> Double.compare(a.departureTime(), b.departureTime()));
		previousIterationSubmissions = currentIterationSubmissions;
		currentIterationSubmissions = new ArrayList<>();
	}
}
