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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemImpl;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * Unit tests for {@link SubmittedDemandEstimator}, locking down the three properties that distinguish it from
 * {@link PreviousIterationDrtDemandEstimator}: it is fed by submissions (so rejected demand stays visible), it bins by
 * intended departure time (not submission time), and it returns an exact forward-rolling window rather than fixed bins.
 *
 * @author nkuehnel / MOIA
 */
public class SubmittedDemandEstimatorTest {

	private static final int HORIZON = 1800;

	private final Network network = PreviousIterationDrtDemandEstimatorTest.createNetwork();

	private final Link link1 = network.getLinks().get(Id.createLinkId("link_1"));
	private final Link link2 = network.getLinks().get(Id.createLinkId("link_2"));

	private final Zone zone1 = ZoneImpl.createDummyZone(Id.create("zone_1", Zone.class), new Coord());
	private final Zone zone2 = ZoneImpl.createDummyZone(Id.create("zone_2", Zone.class), new Coord());
	private final ZoneSystem zonalSystem = new ZoneSystemImpl(List.of(zone1, zone2), coord -> {
		if (coord == link1.getToNode().getCoord()) {
			return Optional.of(zone1);
		} else if (coord == link2.getToNode().getCoord()) {
			return Optional.of(zone2);
		} else {
			throw new RuntimeException();
		}
	}, network);

	private int nextRequestId = 0;

	@Test
	void noSubmissions() {
		SubmittedDemandEstimator estimator = createEstimator();
		estimator.notifyIterationEnds(null);

		assertDemand(estimator, 0, zone1, 0);
		assertDemand(estimator, 2000, zone1, 0);
		assertDemand(estimator, 0, zone2, 0);
	}

	@Test
	void submissionsAreCountedByDepartureZoneAndTime() {
		SubmittedDemandEstimator estimator = createEstimator();

		// three requests departing zone1 and one departing zone2, all within [0, 1800)
		estimator.handleEvent(submission(100, link1, 100, TransportMode.drt));
		estimator.handleEvent(submission(150, link1, 200, TransportMode.drt));
		estimator.handleEvent(submission(150, link1, 1500, TransportMode.drt));
		estimator.handleEvent(submission(150, link2, 500, TransportMode.drt));
		estimator.notifyIterationEnds(null);

		assertDemand(estimator, 0, zone1, 3);
		assertDemand(estimator, 0, zone2, 1);
	}

	@Test
	void binnedByDepartureTimeNotSubmissionTime() {
		SubmittedDemandEstimator estimator = createEstimator();

		// booked (submission time) at t=100, but the passenger intends to depart much later at t=5000
		estimator.handleEvent(submission(100, link1, 5000, TransportMode.drt));
		estimator.notifyIterationEnds(null);

		// the window anchored at the submission time sees nothing...
		assertDemand(estimator, 0, zone1, 0);
		// ...but the window covering the intended departure time does
		assertDemand(estimator, 5000, zone1, 1);
	}

	@Test
	void rollingWindowIsExactAndForwardLooking() {
		SubmittedDemandEstimator estimator = createEstimator();

		estimator.handleEvent(submission(0, link1, 1000, TransportMode.drt));
		estimator.handleEvent(submission(0, link1, 2000, TransportMode.drt));
		estimator.handleEvent(submission(0, link1, 2500, TransportMode.drt));
		estimator.notifyIterationEnds(null);

		// window [500, 2300) covers departures at 1000 and 2000, but not 2500 (unlike fixed 1800-bins, which at
		// fromTime=500 would return bin [0,1800) = only the 1000 departure)
		assertDemand(estimator, 500, zone1, 2);
		// window [0, 1800) covers only the 1000 departure
		assertDemand(estimator, 0, zone1, 1);
		// window boundaries are half-open [from, from+H): a departure exactly at from+H is excluded, exactly at from is included
		assertDemand(estimator, 1000, zone1, 3); // [1000, 2800) -> 1000 (inclusive lower bound), 2000, 2500
		assertDemand(estimator, 2500, zone1, 1); // [2500, 4300) -> only 2500 (inclusive lower bound)
		assertDemand(estimator, 2501, zone1, 0); // [2501, 4301) -> none
	}

	@Test
	void rejectedRequestsStayVisible() {
		SubmittedDemandEstimator estimator = createEstimator();

		// the whole point: a request that will be rejected still emits a submission event, so it is counted. There is
		// no rejection handling in this estimator by design -- the survivorship bias of departure-event-based estimators
		// is exactly what this class fixes. We simply submit and never depart; demand is still seen next iteration.
		estimator.handleEvent(submission(100, link1, 300, TransportMode.drt));
		estimator.notifyIterationEnds(null);

		assertDemand(estimator, 0, zone1, 1);
	}

	@Test
	void nonDrtSubmissionsAreIgnored() {
		SubmittedDemandEstimator estimator = createEstimator();

		estimator.handleEvent(submission(100, link1, 100, "mode X"));
		estimator.notifyIterationEnds(null);

		assertDemand(estimator, 0, zone1, 0);
	}

	@Test
	void currentSubmissionsAreCopiedToPreviousAfterReset() {
		SubmittedDemandEstimator estimator = createEstimator();

		estimator.handleEvent(submission(100, link1, 100, TransportMode.drt));
		estimator.handleEvent(submission(100, link2, 200, TransportMode.drt));

		// before the reset, the previous-iteration map is still empty
		assertDemand(estimator, 0, zone1, 0);
		assertDemand(estimator, 0, zone2, 0);

		estimator.notifyIterationEnds(null);

		assertDemand(estimator, 0, zone1, 1);
		assertDemand(estimator, 0, zone2, 1);
	}

	private SubmittedDemandEstimator createEstimator() {
		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		return new SubmittedDemandEstimator(zonalSystem, drtConfigGroup);
	}

	private DrtRequestSubmittedEvent submission(double submissionTime, Link fromLink, double earliestDepartureTime,
			String mode) {
		return new DrtRequestSubmittedEvent(submissionTime, mode,
				Id.create("req_" + nextRequestId++, Request.class), List.of(), fromLink.getId(), link2.getId(), 0.0, 0.0,
				earliestDepartureTime, earliestDepartureTime + 600, earliestDepartureTime + 1200, 1200, null, "1");
	}

	private void assertDemand(SubmittedDemandEstimator estimator, double fromTime, Zone zone, double expectedDemand) {
		assertThat(estimator.getExpectedDemand(fromTime, HORIZON).applyAsDouble(zone)).isEqualTo(expectedDemand);
	}
}
