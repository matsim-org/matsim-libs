/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.P.replan;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;

public class ScorePlansHandler implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler{
	
	private final static Logger log = Logger.getLogger(ScorePlansHandler.class);
	private static final double earningsPerMeter = 0.05;
	private static final double costsPerMeter = 0.03;
	
	private Network net;
	TreeMap<Id, Id> vehId2PersonIdMap = new TreeMap<Id, Id>();
	TreeMap<Id, ScoreContainer> personId2ScoreMap = new TreeMap<Id, ScoreContainer>();
	
	
	public ScorePlansHandler(Network net){
		this.net = net;
	}

	public TreeMap<Id, ScoreContainer> getPersonId2ScoreMap() {
		return this.personId2ScoreMap;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(this.vehId2PersonIdMap.get(event.getVehicleId()) == null){
			this.vehId2PersonIdMap.put(event.getVehicleId(), event.getDriverId());
		}
		if(this.personId2ScoreMap.get(event.getDriverId()) == null){
			this.personId2ScoreMap.put(event.getDriverId(), new ScoreContainer(event.getDriverId()));
		}		
	}

	@Override
	public void reset(int iteration) {
		log.info("Nothing to do in iteration " + iteration);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.personId2ScoreMap.get(this.vehId2PersonIdMap.get(event.getVehicleId())).addPassenger();		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		this.personId2ScoreMap.get(this.vehId2PersonIdMap.get(event.getVehicleId())).removePassenger();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(this.personId2ScoreMap.get(event.getPersonId()) != null){
			this.personId2ScoreMap.get(event.getPersonId()).handleLinkTravelled(this.net.getLinks().get(event.getLinkId()));
		}
	}

	class ScoreContainer {
		Id driverId;
		double costs = 0;
		double servedTrips = 0;
		double earnings = 0;
		
		int passengersCurrentlyInVeh = 0;
		
		public ScoreContainer(Id driverId){
			this.driverId = driverId;
		}
		
		public void addPassenger(){
			this.passengersCurrentlyInVeh++;
		}
		
		public void removePassenger(){
			this.passengersCurrentlyInVeh--;
			this.servedTrips++;
		}
		
		public void handleLinkTravelled(Link link){
			this.costs += link.getLength() * ScorePlansHandler.costsPerMeter;
			this.earnings += link.getLength() * ScorePlansHandler.earningsPerMeter * this.passengersCurrentlyInVeh;
		}
		
		@Override
		public String toString() {
			return "Driver " + this.driverId.toString() + " served " + this.servedTrips + " trips spending a total of " + this.costs + " vs. " + this.earnings + " earnings";
		}
	}
	

}
