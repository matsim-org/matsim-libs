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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;


/**
 * @author dgrether
 *
 */
public class DgAverageTravelTimeSpeed implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler{

	private Network network;
	private Map<Id, Double> networkEnterTimeByPersonId;
	private Set<Id> seenPersonIds;
	private double sumTravelTime;
	private double sumDistance;
	private double numberOfTrips;

	public DgAverageTravelTimeSpeed(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.networkEnterTimeByPersonId = new HashMap<Id, Double>();
		this.seenPersonIds = new HashSet<Id>();
		this.sumTravelTime = 0.0;
		this.sumDistance = 0.0;
		this.numberOfTrips = 0.0;
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
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (network.getLinks().containsKey(event.getLinkId())) {
			this.networkEnterTimeByPersonId.put(event.getPersonId(), event.getTime());
			this.seenPersonIds.add(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		this.networkEnterTimeByPersonId.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (this.seenPersonIds.contains(event.getPersonId())){
			this.numberOfTrips++;
		}
		this.networkEnterTimeByPersonId.remove(event.getPersonId());		
	}

	
	public double getTravelTime() {
		return sumTravelTime;
	}
	
	public double getNumberOfPersons(){
		return this.seenPersonIds.size();
	}
	
	public double getDistanceMeter(){
		return this.sumDistance;
	}
	
	public double getNumberOfTrips(){
		return this.numberOfTrips;
	}


}
