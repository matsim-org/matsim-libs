/* *********************************************************************** *
 * project: org.matsim.*
 * CountPossibleSharedRides.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.possiblesharedrides;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEvent;
import org.matsim.core.api.experimental.events.PersonEvent;

/**
 * Computes and outputs statistics about number of possible shared rides.
 * @author thibautd
 */
public class CountPossibleSharedRides {

	private final double eventInitialSearchWindow = 10*60d;
	private final double eventSeachWindowIncr = 5*60d;

	private final EventsTopology arrivalsTopology;
	private final EventsTopology departuresTopology;
	private final EventsTopology enterLinksTopology;
	private final EventsTopology leaveLinksTopology;
	private final Map<Id, Plan> plans = new HashMap<Id, Plan>();

	private final List<TripData> results = new ArrayList<TripData>();

	/*
	 * =========================================================================
	 * constructors
	 * =========================================================================
	 */
	public CountPossibleSharedRides(
			final Network network,
			final EventsAccumulator events,
			final Population population,
			final double acceptableDistance,
			final double timeWindowRadius) {
		TopologyFactory factory = new TopologyFactory(
				network,
				acceptableDistance,
				timeWindowRadius);
		this.arrivalsTopology = factory.createEventTopology(events.getArrivalEvents());
		this.departuresTopology = factory.createEventTopology(events.getDepartureEvents());
		this.enterLinksTopology = factory.createEventTopology(events.getEnterLinkEvents());
		this.leaveLinksTopology = factory.createEventTopology(events.getLeaveLinkEvents());

		for (Person pers : population.getPersons().values()) {
			this.plans.put(pers.getId(), pers.getSelectedPlan());
		}
	}

	/*
	 * =========================================================================
	 * Analysis functions
	 * =========================================================================
	 */
	/**
	 * Extracts trip data and accumulates them internaly.
	 * To run before any result getting method.
	 */
	public void run() {
		for (Map.Entry<Id, Plan> plan : this.plans.entrySet()) {
			for (PlanElement pe : plan.getValue().getPlanElements()) {
				if (pe instanceof Leg) {
					this.results.add(getTripData((Leg) pe, plan.getKey()));
				}
			}
		}
	}

	/*
	 * =========================================================================
	 * formated results accessors
	 * =========================================================================
	 */

	/*
	 * =========================================================================
	 * various helper methods
	 * =========================================================================
	 */
	private TripData getTripData(final Leg leg, final Id personId) {
		Route route = leg.getRoute();
		double departureTime = leg.getDepartureTime();
		double arrivalTime = departureTime + leg.getTravelTime();
		//TODO: get distance by a non-deprecated way
		double distance = route.getDistance();
		int numberOfJoinableTrips = 0;
		Id departureId = route.getStartLinkId();
		Id arrivalId = route.getEndLinkId();

		// correct leg info based on events
		LinkEvent departure = getDepartureEvent(personId, departureTime, departureId);
		LinkEvent arrival = getArrivalEvent(personId, arrivalTime, arrivalId);

		numberOfJoinableTrips = countPossibleSharedRides(departure, arrival);

		return new TripData(departure.getTime(), distance, numberOfJoinableTrips);
	}

	private LinkEvent getDepartureEvent(
			final Id person,
			final double expectedTime,
			final Id link) {
		return this.getEvent(person, expectedTime, link, this.departuresTopology);
	}

	private LinkEvent getArrivalEvent(
			final Id person,
			final double expectedTime,
			final Id link) {
		return this.getEvent(person, expectedTime, link, this.arrivalsTopology);
	}

	private LinkEvent getEvent(
			final Id person,
			final double expectedTime,
			final Id link,
			final EventsTopology searchEvents) {
		double timeWindow = this.eventInitialSearchWindow;
		List<LinkEvent> currentEventList;

		// increment the tw size until an event corresponding to the agent is
		// found.
		while (true) {
			currentEventList = searchEvents.getEventsInTimeWindow(
					link,
					expectedTime,
					timeWindow);
			for (LinkEvent currentEvent : currentEventList) {
				if (currentEvent.getPersonId().equals(person)) {
					return currentEvent;
				}
			}
			timeWindow +=  this.eventSeachWindowIncr;
		}
	}

	private int countPossibleSharedRides(
			final LinkEvent departure,
			final LinkEvent arrival) {
		List<LinkEvent> departureNeighbors =
			this.leaveLinksTopology.getNeighbors(departure);
		List<LinkEvent> arrivalNeighbors =
			this.enterLinksTopology.getNeighbors(arrival);
		int count = 0;
		
		// count the number of persons having a "leaveLinkEvent" near the
		// departure and a "enterLinkEvent" near the arrival: they are considered
		// as potential shared ride drivers.
		for (PersonEvent event : departureNeighbors) {
			if ( passesHere(event.getPersonId(), arrivalNeighbors) ) {
				count++;
			}
		}

		return count;
	}

	/**
	 * @return true if there is an event in the list corresponding to the person.
	 */
	private boolean passesHere(
			final Id person,
			final List<? extends PersonEvent> events) {
		for (PersonEvent event : events) {
			if (event.getPersonId().equals(person)) {
				//remove event from list?
				return true;
			}
		}

		return false;
	}

	/*
	 * =========================================================================
	 * helper class
	 * =========================================================================
	 */
	/**
	 * stocks all relevant information related to a trip
	 */
	private class TripData {
		public final double timeOfDay;
		public final double distance;
		public final int numberOfJoinableTrips;

		public TripData(
				final double tod,
				final double dist,
				final int n) {
			this.timeOfDay = tod;
			this.distance = dist;
			this.numberOfJoinableTrips = n;
		}
	}
}

