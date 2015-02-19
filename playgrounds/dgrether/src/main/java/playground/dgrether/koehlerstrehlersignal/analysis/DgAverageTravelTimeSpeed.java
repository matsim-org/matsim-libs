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
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class DgAverageTravelTimeSpeed implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler, PersonDepartureEventHandler{

	private static final Logger log = Logger.getLogger(DgAverageTravelTimeSpeed.class);
	
	private Network network;
	private Map<Id<Person>, Double> networkEnterTimeByPersonId;
	private Set<Id<Person>> seenPersonIds;
	private double sumTravelTime;
	private double sumDistance;
	private double numberOfTrips;

	public DgAverageTravelTimeSpeed(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.networkEnterTimeByPersonId = new HashMap<>();
		this.seenPersonIds = new HashSet<>();
		this.sumTravelTime = 0.0;
		this.sumDistance = 0.0;
		this.numberOfTrips = 0.0;
	}

	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (network.getLinks().containsKey(event.getLinkId())) {
			this.networkEnterTimeByPersonId.put(event.getPersonId(), event.getTime());
			this.seenPersonIds.add(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (network.getLinks().containsKey(event.getLinkId())) {
			this.networkEnterTimeByPersonId.put(event.getPersonId(), event.getTime());
			this.seenPersonIds.add(event.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (network.getLinks().containsKey(event.getLinkId())) {
			Double linkEnterEvent = this.networkEnterTimeByPersonId.remove(event.getPersonId());
			if (linkEnterEvent != null) {
				this.sumTravelTime += event.getTime() - linkEnterEvent;
				this.sumDistance += network.getLinks().get(event.getLinkId()).getLength();
			}
		}
		else{
			log.error("Link wasn't found.");
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (this.seenPersonIds.contains(event.getPersonId())){
			this.numberOfTrips++;
		}
		if (network.getLinks().containsKey(event.getLinkId())) {
			Double personDepartureEvent = this.networkEnterTimeByPersonId.remove(event.getPersonId());
			if (personDepartureEvent != null) {
				this.sumTravelTime += event.getTime() - personDepartureEvent;
				this.sumDistance += network.getLinks().get(event.getLinkId()).getLength();
			}
		}
		else{
			log.error("Link wasn't found.");
		}		
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.networkEnterTimeByPersonId.remove(event.getPersonId());
	}

	
	public double getTravelTime() {
		return sumTravelTime;
	}
	
	public double getNumberOfPersons(){
		return this.seenPersonIds.size();
	}
	
	public Set<Id<Person>> getSeenPersonIds(){
		return this.seenPersonIds;
	}
	
	public double getDistanceMeter(){
		return this.sumDistance;
	}
	
	public double getNumberOfTrips(){
		return this.numberOfTrips;
	}


}
