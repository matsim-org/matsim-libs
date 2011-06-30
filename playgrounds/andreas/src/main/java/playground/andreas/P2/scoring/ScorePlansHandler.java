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
package playground.andreas.P2.scoring;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;

/**
 * Scores paratransit vehicles
 * 
 * @author aneumann
 *
 */
public class ScorePlansHandler implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler{
	
	private final static Logger log = Logger.getLogger(ScorePlansHandler.class);
	private double earningsPerMeterAndPassenger = 0.05;
	private double expensesPerMeter = 0.03;
	
	private Network net;
	
	TreeMap<Id, Id> driverId2VehIdMap = new TreeMap<Id, Id>();
	TreeMap<Id, ScoreContainer> vehicleId2ScoreMap = new TreeMap<Id, ScoreContainer>();

	public ScorePlansHandler(double earningsPerMeterAndPassenger, double expensesPerMeter){
		this.earningsPerMeterAndPassenger = earningsPerMeterAndPassenger;
		this.expensesPerMeter = expensesPerMeter;
	}

	public void init(Network net) {
		this.net = net;
	}

	public TreeMap<Id, ScoreContainer> getDriverId2ScoreMap() {
		return this.vehicleId2ScoreMap;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(this.driverId2VehIdMap.get(event.getDriverId()) == null){
			this.driverId2VehIdMap.put(event.getDriverId(), event.getVehicleId());
		}
		if(this.vehicleId2ScoreMap.get(event.getVehicleId()) == null){
			this.vehicleId2ScoreMap.put(event.getVehicleId(), new ScoreContainer(event.getVehicleId(), this.earningsPerMeterAndPassenger, this.expensesPerMeter));
		}		
	}

	@Override
	public void reset(int iteration) {
		this.driverId2VehIdMap = new TreeMap<Id, Id>();
		this.vehicleId2ScoreMap = new TreeMap<Id, ScoreContainer>();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.vehicleId2ScoreMap.get(event.getVehicleId()).addPassenger();		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		this.vehicleId2ScoreMap.get(event.getVehicleId()).removePassenger();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(this.driverId2VehIdMap.get(event.getPersonId()) != null){
			this.vehicleId2ScoreMap.get(this.driverId2VehIdMap.get(event.getPersonId())).handleLinkTravelled(this.net.getLinks().get(event.getLinkId()));
		}
	}
}