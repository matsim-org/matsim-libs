/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.contrib.signals.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * Calculate total delay and delay per link. 
 * Delay of stucked agents can be considered (call considerDelayOfStuckedAgents()).
 * Delay occurring between PersonDeparture and VehicleEntersTraffic event is included 
 * (that is why it is not enough to consider only vehicle events).
 * Delay of all passengers (not only the driver) is considered.
 * 
 * @author tthunig
 */
public class DelayAnalysisTool implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonStuckEventHandler {

	private static final Logger LOG = Logger.getLogger(DelayAnalysisTool.class);
	
	private final Network network;
	private boolean considerStuckedAgents = false;
	
	private double totalDelay = 0.0;
	private Map<Id<Link>, Double> totalDelayPerLink = new HashMap<>();
	private Map<Id<Link>, Integer> numberOfAgentsPerLink = new HashMap<>();
	
	private Map<Id<Person>, Double> earliestLinkExitTimePerAgent = new HashMap<>();
	private Map<Id<Vehicle>, Set<Id<Person>>> vehicleIdToPassengerIds = new HashMap<>();
	
	public DelayAnalysisTool(Network network) {
		this.network = network;
	}
	
	@Inject
	public DelayAnalysisTool(Network network, EventsManager em) {
		this(network);
		em.addHandler(this);
	}
	
	@Override
	public void reset(int iteration) {
		this.totalDelay = 0.0;
		this.earliestLinkExitTimePerAgent.clear();
		this.vehicleIdToPassengerIds.clear();
		this.totalDelayPerLink.clear();
		this.numberOfAgentsPerLink.clear();
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// for the first link every vehicle needs one second without delay
		earliestLinkExitTimePerAgent.put(event.getPersonId(), event.getTime() + 1);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!vehicleIdToPassengerIds.containsKey(event.getVehicleId())){
			// register empty vehicle
			vehicleIdToPassengerIds.put(event.getVehicleId(), new HashSet<Id<Person>>());
		}
		// add passenger
		vehicleIdToPassengerIds.get(event.getVehicleId()).add(event.getPersonId());
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		// calculate earliest link exit time
		Link currentLink = network.getLinks().get(event.getLinkId());
		double freespeedTt = currentLink.getLength() / currentLink.getFreespeed();
		// this is the earliest time where matsim sets the agent to the next link
		double matsimFreespeedTT = Math.floor(freespeedTt + 1);	
		for (Id<Person> passengerId : vehicleIdToPassengerIds.get(event.getVehicleId())){
			this.earliestLinkExitTimePerAgent.put(passengerId, event.getTime() + matsimFreespeedTT);
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// initialize link based analysis data structure
		if (!totalDelayPerLink.containsKey(event.getLinkId())) {
			totalDelayPerLink.put(event.getLinkId(), 0.0);
			numberOfAgentsPerLink.put(event.getLinkId(), 0);
		}
		
		for (Id<Person> passengerId : vehicleIdToPassengerIds.get(event.getVehicleId())) {
			// calculate delay for every passenger
			double currentDelay = event.getTime() - this.earliestLinkExitTimePerAgent.remove(passengerId);
			totalDelayPerLink.put(event.getLinkId(), totalDelayPerLink.get(event.getLinkId()) + currentDelay);
			totalDelay += currentDelay;
			// increase agent counter
			numberOfAgentsPerLink.put(event.getLinkId(), numberOfAgentsPerLink.get(event.getLinkId()) +1 );
		}
	}
	
	@Override
	public void handleEvent(PersonStuckEvent event) {
		if (this.considerStuckedAgents) {
			if (!totalDelayPerLink.containsKey(event.getLinkId())) {
				// initialize link based analysis data structure
				totalDelayPerLink.put(event.getLinkId(), 0.0);
				numberOfAgentsPerLink.put(event.getLinkId(), 0);
			}
			
			double stuckDelay = event.getTime() - this.earliestLinkExitTimePerAgent.remove(event.getPersonId());
			LOG.warn("Add delay " + stuckDelay + " of agent " + event.getPersonId() + " that stucked on link "
					+ event.getLinkId());
			totalDelayPerLink.put(event.getLinkId(), totalDelayPerLink.get(event.getLinkId()) + stuckDelay);
			this.totalDelay += stuckDelay;
			// increase agent counter
			numberOfAgentsPerLink.put(event.getLinkId(), numberOfAgentsPerLink.get(event.getLinkId()) + 1);
		}
	}

	public double getTotalDelay(){
		return totalDelay;
	}

	public Map<Id<Link>, Double> getTotalDelayPerLink(){
		return totalDelayPerLink;
	}

	public Map<Id<Link>, Double> getAvgDelayPerLink(){
		Map<Id<Link>, Double> avgDelayMap = new HashMap<>();
		for (Id<Link> linkId : totalDelayPerLink.keySet()){
			avgDelayMap.put(linkId, totalDelayPerLink.get(linkId) / numberOfAgentsPerLink.get(linkId));
		}
		return avgDelayMap;
	}

	public void considerDelayOfStuckedAgents() {
		this.considerStuckedAgents = true;
	}

	
}
