/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import java.util.function.Function;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector.EventSequence;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;

import com.google.common.base.Preconditions;

final class DrtLeg {
	final Id<Request> request;
	final double departureTime;
	final Id<Person> person;
	final Id<DvrpVehicle> vehicle;
	final Id<Link> fromLinkId;
	final Coord fromCoord;
	final Id<Link> toLinkId;
	final Coord toCoord;
	final double waitTime;
	final double unsharedDistanceEstimate_m;
	final double unsharedTimeEstimate_m;
	final double arrivalTime;
	final double fare;
	final double latestDepartureTime;
	final double latestArrivalTime;

	DrtLeg(EventSequence sequence, Function<Id<Link>, ? extends Link> linkProvider) {
		Preconditions.checkArgument(sequence.isCompleted());
		DrtRequestSubmittedEvent submittedEvent = sequence.getSubmitted();
		PersonDepartureEvent departureEvent = sequence.getDeparture().get();
		PassengerPickedUpEvent pickedUpEvent = sequence.getPickedUp().get();
		this.request = submittedEvent.getRequestId();
		this.departureTime = departureEvent.getTime();
		this.person = submittedEvent.getPersonId();
		this.vehicle = pickedUpEvent.getVehicleId();
		this.fromLinkId = submittedEvent.getFromLinkId();
		this.fromCoord = linkProvider.apply(fromLinkId).getToNode().getCoord();
		this.toLinkId = submittedEvent.getToLinkId();
		this.toCoord = linkProvider.apply(toLinkId).getToNode().getCoord();
		this.waitTime = pickedUpEvent.getTime() - departureEvent.getTime();
		this.unsharedDistanceEstimate_m = submittedEvent.getUnsharedRideDistance();
		this.unsharedTimeEstimate_m = submittedEvent.getUnsharedRideTime();
		this.arrivalTime = sequence.getDroppedOff().get().getTime();
		// PersonMoneyEvent has negative amount because the agent's money is reduced -> for the operator that is a positive amount
		this.fare = sequence.getDrtFares().stream().mapToDouble(PersonMoneyEvent::getAmount).sum();
		this.latestDepartureTime = sequence.getSubmitted().getLatestPickupTime();
		this.latestArrivalTime = sequence.getSubmitted().getLatestDropoffTime();
	}
}
