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

package org.matsim.contrib.drt.passenger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * @author nkuehnel / MOIA
 */
@SuppressWarnings("deprecation")
public class PromisedPickupTimeWindowDrtOfferAcceptorTest {

	private final DrtRequest request = DrtRequest.newBuilder()
			.id(Id.create("r", Request.class))
			.submissionTime(0)
			.earliestDepartureTime(100)
			.constraints(new DrtRouteConstraints(900, 600, 300, Double.POSITIVE_INFINITY, 0, false))
			.build();

	private final AcceptedDrtRequest accepted = new PromisedPickupTimeWindowDrtOfferAcceptor(120)
			.acceptDrtOffer(request, 200, 500, 60, 30)
			.orElseThrow();

	@Test
	void keepsMaxRideDuration() {
		// a missing value would default to 0 and leave existing requests without ride-time slack
		assertThat(accepted.getMaxRideDuration()).isEqualTo(600);
	}

	@Test
	void setsPlannedPickupAndDropoffTime() {
		assertThat(accepted.getRequestTiming().getPlannedPickupTime().seconds()).isEqualTo(200);
		assertThat(accepted.getRequestTiming().getPlannedDropoffTime().seconds()).isEqualTo(500);
	}

	@Test
	void narrowsPickupWindowAndKeepsArrivalWindow() {
		assertThat(accepted.getLatestStartTime()).isEqualTo(320);
		assertThat(accepted.getLatestArrivalTime()).isEqualTo(1000);
		assertThat(accepted.getPickupDuration()).isEqualTo(60);
		assertThat(accepted.getDropoffDuration()).isEqualTo(30);
	}
}
