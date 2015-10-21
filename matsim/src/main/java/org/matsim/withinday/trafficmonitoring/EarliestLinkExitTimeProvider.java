/* *********************************************************************** *
 * project: org.matsim.*
 * MobsimDataProvider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmonitoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

/**
 * Returns the time when an agent could leave a link if he can travel
 * at free speed. After this time, the agent cannot stop on its current link
 * anymore which influences its possible replanning operations.
 *
 * @author cdobler
 */
public class EarliestLinkExitTimeProvider implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler,
		PersonDepartureEventHandler, PersonStuckEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private static final Logger log = Logger.getLogger(EarliestLinkExitTimeProvider.class);

	/*
	 * We have to create an internal TransportModeProvider and delegate the events to it.
	 * Otherwise, race conditions could occur since an it could not be guaranteed that an
	 * external TransportModeProvider has processed all relevant events when this class
	 * handles an event.
	 */
	private final TransportModeProvider transportModeProvider;

	private final Scenario scenario;
	private final Map<String, TravelTime> multiModalTravelTimes;
	private final TravelTime freeSpeedTravelTime;

	private final Map<Id<Person>, Double> earliestLinkExitTimes = new ConcurrentHashMap<>();
	private final Map<Double, Set<Id<Person>>> earliestLinkExitTimesPerTimeStep = new ConcurrentHashMap<>();

	private Map<Id<Vehicle>, Set<Id<Person>>> personsInsideVehicle = new HashMap<>();
	
	public EarliestLinkExitTimeProvider(Scenario scenario) {
		this(scenario, null);
		log.info("Note: no map containing TravelTime objects for all simulated modes is given. Therefore use free speed " +
				"car travel time as minimal link travel time for all modes.");
	}

	public EarliestLinkExitTimeProvider(Scenario scenario, Map<String, TravelTime> multiModalTravelTimes) {
		this.scenario = scenario;
		this.multiModalTravelTimes = multiModalTravelTimes;
		this.transportModeProvider = new TransportModeProvider();
		this.freeSpeedTravelTime = new FreeSpeedTravelTime();
	}

	public TransportModeProvider getTransportModeProvider() {
		return this.transportModeProvider;
	}

	public double getEarliestLinkExitTime(Id<Person> agentId) {
		Double earliestExitTime = this.earliestLinkExitTimes.get(agentId);
		if (earliestExitTime == null) return Time.UNDEFINED_TIME;
		else return earliestExitTime;
	}

	public Map<Id<Person>, Double> getEarliestLinkExitTimes() {
		return Collections.unmodifiableMap(this.earliestLinkExitTimes);
	}

	public Set<Id<Person>> getEarliestLinkExitTimesPerTimeStep(double time) {
		Set<Id<Person>> set = this.earliestLinkExitTimesPerTimeStep.get(time);
		if (set != null) return Collections.unmodifiableSet(set);
		else return null;
	}

	public Map<Double, Set<Id<Person>>> getEarliestLinkExitTimesPerTimeStep() {
		return Collections.unmodifiableMap(this.earliestLinkExitTimesPerTimeStep);
	}

	@Override
	public void reset(int iteration) {
		this.transportModeProvider.reset(iteration);

		this.earliestLinkExitTimes.clear();
		this.earliestLinkExitTimesPerTimeStep.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.transportModeProvider.handleEvent(event);
		this.removeEarliestLinkExitTimesAtTime(event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// handle event for all persons inside the vehicle
		for (Id<Person> personInsideVehicle : personsInsideVehicle.get(event.getVehicleId())) {
			String transportMode = this.transportModeProvider.getTransportMode(personInsideVehicle);
			double now = event.getTime();
			Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
			Person person = this.scenario.getPopulation().getPersons().get(personInsideVehicle);
			double earliestExitTime = Time.UNDEFINED_TIME;
			if (this.multiModalTravelTimes != null) {
				if (transportMode == null) {
					throw new RuntimeException("Agent " + personInsideVehicle.toString() + " is currently not performing a leg. Aborting!");
				} else {
					TravelTime travelTime = this.multiModalTravelTimes.get(transportMode);
					if (travelTime == null) {
						throw new RuntimeException("No TravelTime object was found for mode " + transportMode + ". Aborting!");
					}

					earliestExitTime = Math.floor(now + travelTime.getLinkTravelTime(link, now, person, null));
				}
			} else {
				earliestExitTime = Math.floor(now + this.freeSpeedTravelTime.getLinkTravelTime(link, now, person, null));
			}
			this.handleAddEarliestLinkExitTime(personInsideVehicle, earliestExitTime);
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// handle event for all persons inside the vehicle
		for (Id<Person> personInsideVehicle : personsInsideVehicle.get(event.getVehicleId())) {
			this.removeEarliestLinkExitTimesAtTime(personInsideVehicle);
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.transportModeProvider.handleEvent(event);
		this.handleAddEarliestLinkExitTime(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.transportModeProvider.handleEvent(event);
		this.removeEarliestLinkExitTimesAtTime(event.getPersonId());
	}

	private void handleAddEarliestLinkExitTime(Id<Person> agentId, double earliestExitTime) {

		this.earliestLinkExitTimes.put(agentId, earliestExitTime);

		Set<Id<Person>> earliestLinkExitTimesAtTime = this.earliestLinkExitTimesPerTimeStep.get(earliestExitTime);
		if (earliestLinkExitTimesAtTime == null) {
			earliestLinkExitTimesAtTime = new HashSet<>();
			this.earliestLinkExitTimesPerTimeStep.put(earliestExitTime, earliestLinkExitTimesAtTime);
		}
		earliestLinkExitTimesAtTime.add(agentId);
	}

	private void removeEarliestLinkExitTimesAtTime(Id<Person> agentId) {

		Double earliestExitTime = this.earliestLinkExitTimes.remove(agentId);

		if (earliestExitTime != null) {
			Set<Id<Person>> earliestLinkExitTimesAtTime = this.earliestLinkExitTimesPerTimeStep.get(earliestExitTime);
			if (earliestLinkExitTimesAtTime != null) {
				earliestLinkExitTimesAtTime.remove(agentId);
				if (earliestLinkExitTimesAtTime.isEmpty()) {
					this.earliestLinkExitTimesPerTimeStep.remove(earliestExitTime);
				}
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!personsInsideVehicle.containsKey(event.getVehicleId())){
			personsInsideVehicle.put(event.getVehicleId(), new HashSet<Id<Person>>());
		}
		personsInsideVehicle.get(event.getVehicleId()).add(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		personsInsideVehicle.get(event.getVehicleId()).remove(event.getPersonId());
	}
}