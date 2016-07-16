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
import org.matsim.vehicles.Vehicle;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class TtTotalDelay implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler{

	private static final Logger log = Logger
			.getLogger(TtTotalDelay.class);
	
	/** (sub)network where delay should be calculated */
	private Network network;
	
	private Map<Id<Person>, Double> earliestLinkExitTimePerPerson;
	private Map<Id<Vehicle>, Set<Id<Person>>> vehicleIdToPassengerIds;
	private double totalPersonDelay;

	public TtTotalDelay(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.earliestLinkExitTimePerPerson = new HashMap<>();
		this.vehicleIdToPassengerIds = new HashMap<>();
		this.totalPersonDelay = 0.0;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())){
			// for the first link every agent needs one second without delay
			this.earliestLinkExitTimePerPerson.put(event.getPersonId(), event.getTime() + 1);
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
	public void handleEvent(LinkLeaveEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			for (Id<Person> passengerId : vehicleIdToPassengerIds.get(event.getVehicleId())){
				Double earliestLinkExitTime = this.earliestLinkExitTimePerPerson.remove(passengerId);
				if (earliestLinkExitTime != null) {
					// add the number of seconds the agent is later as the earliest link exit time as delay
					this.totalPersonDelay += event.getTime() - earliestLinkExitTime;
				}
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			Link link = this.network.getLinks().get(event.getLinkId());
			double freespeedTT = link.getLength()/link.getFreespeed();
			// this is the earliest time where matsim sets the agent to the next link
			double matsimFreespeedTT = Math.floor(freespeedTT + 1);
			for (Id<Person> passengerId : vehicleIdToPassengerIds.get(event.getVehicleId())){
				this.earliestLinkExitTimePerPerson.put(passengerId, event.getTime() + matsimFreespeedTT);
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.earliestLinkExitTimePerPerson.remove(event.getPersonId());		
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.earliestLinkExitTimePerPerson.remove(event.getPersonId());
		log.warn("Vehicle " + event.getPersonId() + " got stucked at link "
				+ event.getLinkId() + ". Its delay at this link is not considered in the total delay.");
	}

	public double getTotalDelay() {
		return totalPersonDelay;
	}

}
