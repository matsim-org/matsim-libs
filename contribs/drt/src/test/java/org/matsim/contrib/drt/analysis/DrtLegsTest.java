/* *********************************************************************** *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector.EventSequence;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.core.network.NetworkUtils;

/**
 * Tests the rows of drt_legs for requests whose scheduled stops differ from the submitted ones (stop candidates).
 *
 * @author nkuehnel / MOIA
 */
class DrtLegsTest {
	private static final String MODE = "drt";
	private final Network network = NetworkUtils.createNetwork();
	private final Id<Link> routedPickup = link("routedPickup", 0);
	private final Id<Link> otherPickup = link("otherPickup", 300);
	private final Id<Link> routedDropoff = link("routedDropoff", 5000);
	private final Id<Link> otherDropoff = link("otherDropoff", 5300);

	private final Id<Request> requestId = Id.create("r", Request.class);
	private final Id<Person> personId = Id.createPersonId("p");
	private final Id<DvrpVehicle> vehicleId = Id.create("v", DvrpVehicle.class);

	// submitted for the routed stops: passenger expected at the routed pickup stop at 1000
	private final DrtRequestSubmittedEvent submitted = new DrtRequestSubmittedEvent(900, MODE, requestId,
			List.of(personId), routedPickup, routedDropoff, 600, 5000, 1000, 1600, 3000, 3000, null, null);

	@Test
	void legAtTheSubmittedStops() {
		// passenger departs at the routed stop at 1000, picked up at 1100
		var leg = leg(routedPickup, routedDropoff, 1000, 1100);

		assertThat(leg.fromLinkId()).isEqualTo(routedPickup);
		assertThat(leg.toLinkId()).isEqualTo(routedDropoff);
		assertThat(leg.waitTime()).isEqualTo(100);
	}

	@Test
	void legAtOtherStopsThanSubmitted() {
		// the farther pickup stop is reached 180 s later (longer walk), the passenger departs there at 1180
		var leg = leg(otherPickup, otherDropoff, 1180, 1250);

		assertThat(leg.fromLinkId()).isEqualTo(otherPickup);
		assertThat(leg.fromCoord()).isEqualTo(network.getLinks().get(otherPickup).getToNode().getCoord());
		assertThat(leg.toLinkId()).isEqualTo(otherDropoff);
		assertThat(leg.toCoord()).isEqualTo(network.getLinks().get(otherDropoff).getToNode().getCoord());
		// waiting starts with the arrival at the chosen stop; the longer walk is not waiting
		assertThat(leg.waitTime()).isEqualTo(70);
	}

	@Test
	void legWithoutScheduledStopsFallsBackToTheSubmittedOnes() {
		// scheduled event without pickup/dropoff link (e.g. from older event files)
		var scheduled = new PassengerRequestScheduledEvent(950, MODE, requestId, List.of(personId), vehicleId, 1100,
				2500);
		var leg = legs(scheduled, 1000, 1100).getFirst();

		assertThat(leg.fromLinkId()).isEqualTo(routedPickup);
		assertThat(leg.toLinkId()).isEqualTo(routedDropoff);
		assertThat(leg.waitTime()).isEqualTo(100);
	}

	private DrtAnalysisControlerListener.DrtLeg leg(Id<Link> pickupLink, Id<Link> dropoffLink, double departureTime,
			double pickupTime) {
		var scheduled = new PassengerRequestScheduledEvent(950, MODE, requestId, List.of(personId), vehicleId,
				pickupTime, 2500, pickupLink, dropoffLink);
		var legs = legs(scheduled, departureTime, pickupTime);
		assertThat(legs).hasSize(1);
		return legs.getFirst();
	}

	private List<DrtAnalysisControlerListener.DrtLeg> legs(PassengerRequestScheduledEvent scheduled,
			double departureTime, double pickupTime) {
		var departure = new PersonDepartureEvent(departureTime, personId, scheduled.getPickupLinkId()
				.orElse(routedPickup), MODE, MODE);
		var pickedUp = new PassengerPickedUpEvent(pickupTime, MODE, requestId, personId, vehicleId);
		var droppedOff = new PassengerDroppedOffEvent(2500, MODE, requestId, personId, vehicleId);
		var sequence = new EventSequence(personId, departure, submitted, scheduled, pickedUp, droppedOff, List.of());
		return DrtAnalysisControlerListener.newDrtLegs(sequence, network.getLinks()::get);
	}

	private Id<Link> link(String id, double x) {
		var from = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_from"), new Coord(x, 0));
		var to = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_to"), new Coord(x, 100));
		return NetworkUtils.createAndAddLink(network, Id.createLinkId(id), from, to, 100, 10, 1000, 1).getId();
	}
}
