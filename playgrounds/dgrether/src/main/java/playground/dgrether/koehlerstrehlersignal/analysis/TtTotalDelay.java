/* *********************************************************************** *
 * project: org.matsim.*
 * DgAverageTravelTimeSpeed
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
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
 * Determines delay of all agents inside a given subnetwork.
 * Delay occurring between PersonDeparture and VehicleEntersTraffic event is included (that is also why it is not enough to consider only vehicle events).
 * 
 * @author dgrether
 * @author tthunig
 *
 */
public class TtTotalDelay implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler, PersonStuckEventHandler{

	private static final Logger LOG = Logger.getLogger(TtTotalDelay.class);
	
	/** (sub)network where delay should be calculated */
	private Network network;
	private boolean considerStuckAbortDelay = false;
	
	private Map<Id<Person>, Double> earliestLinkExitTimePerAgent;
	private Map<Id<Vehicle>, Set<Id<Person>>> vehicleIdToPassengerIds;
	private double agentsTotalDelay;

	@Inject
	public TtTotalDelay(Network network, EventsManager events) {
		this.network = network;
		this.reset(0);
		events.addHandler(this);
	}

	@Override
	public void reset(int iteration) {
		this.earliestLinkExitTimePerAgent = new HashMap<>();
		this.vehicleIdToPassengerIds = new HashMap<>();
		this.agentsTotalDelay = 0.0;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())){
			// for the first link every agent needs one second without delay
			this.earliestLinkExitTimePerAgent.put(event.getPersonId(), event.getTime() + 1);
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!vehicleIdToPassengerIds.containsKey(event.getVehicleId())){
			vehicleIdToPassengerIds.put(event.getVehicleId(), new HashSet<Id<Person>>());
		}
		vehicleIdToPassengerIds.get(event.getVehicleId()).add(event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			Link link = this.network.getLinks().get(event.getLinkId());
			double freespeedTT = link.getLength()/link.getFreespeed();
			// this is the earliest time where matsim sets the agent to the next link
			double matsimFreespeedTT = Math.floor(freespeedTT + 1);
			for (Id<Person> passengerId : vehicleIdToPassengerIds.get(event.getVehicleId())){
				this.earliestLinkExitTimePerAgent.put(passengerId, event.getTime() + matsimFreespeedTT);
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			for (Id<Person> passengerId : vehicleIdToPassengerIds.get(event.getVehicleId())) {
				Double earliestLinkExitTime = this.earliestLinkExitTimePerAgent.remove(passengerId);
				if (earliestLinkExitTime != null) {
					// add the number of seconds the agent is later as the earliest link exit time as delay
					this.agentsTotalDelay += event.getTime() - earliestLinkExitTime;
				}
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// no delay occurs on the arrival link
		this.earliestLinkExitTimePerAgent.remove(event.getPersonId());		
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		Double earliestLinkExitTime = this.earliestLinkExitTimePerAgent.remove(event.getPersonId());
		if (this.considerStuckAbortDelay){
			if (this.network.getLinks().containsKey(event.getLinkId())) {
				if (earliestLinkExitTime != null) {
					// add the number of seconds the agent is later as the earliest link exit time as delay
					double stuckAbortDelay = event.getTime() - earliestLinkExitTime;
					this.agentsTotalDelay += stuckAbortDelay;
					LOG.warn("Add delay " + stuckAbortDelay + " of agent " + event.getPersonId() + " that stucked on link " + event.getLinkId());
				}
			}
		}
	}

	public double getTotalDelay() {
		return agentsTotalDelay;
	}
	
	public void considerDelayOfStuckedOrAbortedVehicles(){
		this.considerStuckAbortDelay = true;
	}

}
