/* *********************************************************************** *
 * project: org.matsim.*
 * LinkEnteredProvider.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.vehicles.Vehicle;

/**
 * Returns all agents that have entered a new link in the last time step.
 * Agents who just ended an activity are NOT included, since they...
 * <ul>
 * 	<li>do not produce a link entered event.</li>
 * 	<li>are limited in their possible replanning operations.</li>
 * </ul>
 * 
 * @author cdobler
 */
public class LinkEnteredProvider implements LinkEnterEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler,
		MobsimAfterSimStepListener, PersonEntersVehicleEventHandler {

	private Map<Id<Person>, Id<Link>> linkEnteredAgents = new ConcurrentHashMap<>();	// <agentId, linkId>
	private Map<Id<Person>, Id<Link>> lastTimeStepLinkEnteredAgents = new ConcurrentHashMap<>();	// <agentId, linkId>
	private Map<Id<Vehicle>, List<Id<Person>>> personsInsideVehicle = new HashMap<>(); // <vehicleId, List<personIds inside vehicle>>
	
	public Map<Id<Person>, Id<Link>> getLinkEnteredAgentsInLastTimeStep() {
		return Collections.unmodifiableMap(this.lastTimeStepLinkEnteredAgents);
	}
	
	@Override
	public void reset(int iteration) {
		this.linkEnteredAgents.clear();
		this.lastTimeStepLinkEnteredAgents.clear();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!personsInsideVehicle.containsKey(event.getVehicleId())){
			personsInsideVehicle.put(event.getVehicleId(), new ArrayList<Id<Person>>());
		}
		personsInsideVehicle.get(event.getVehicleId()).add(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.linkEnteredAgents.remove(event.getPersonId());
	}

	/*
	 * Not sure whether the QSim allows an agent to enter a link and start an activity 
	 * in the same time step. If not, this could be removed. 
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.linkEnteredAgents.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		for (Id<Person> personInsideVehicle : personsInsideVehicle.get(event.getVehicleId())){
			this.linkEnteredAgents.put(personInsideVehicle, event.getLinkId());
		}
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		this.lastTimeStepLinkEnteredAgents = linkEnteredAgents;
		this.linkEnteredAgents = new ConcurrentHashMap<>();
	}

}