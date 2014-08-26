/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.P2.scoring.deprecated;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Network;

import playground.andreas.P2.helper.PConfigGroup;

/**
 * Scores paratransit vehicles
 * 
 * @author aneumann
 *
 */
@Deprecated
public class ScorePlansHandler implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler{
	
	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(ScorePlansHandler.class);
	
	private final String pIdentifier;
	private double earningsPerBoardingPassenger;
	private final double earningsPerMeterAndPassenger;
	private final double expensesPerMeter;
	private final double costPerVehicleAndDay;
	
	private Network net;
	
	TreeMap<Id, Id> driverId2VehIdMap = new TreeMap<Id, Id>();
	TreeMap<Id, ScoreContainer> vehicleId2ScoreMap = new TreeMap<Id, ScoreContainer>();


	public ScorePlansHandler(PConfigGroup pConfig){
		this.pIdentifier = pConfig.getPIdentifier();
		this.earningsPerBoardingPassenger = pConfig.getEarningsPerBoardingPassenger();
		this.earningsPerMeterAndPassenger = pConfig.getEarningsPerKilometerAndPassenger() / 1000.0;
		this.expensesPerMeter = pConfig.getCostPerKilometer() / 1000.0;
		this.costPerVehicleAndDay = pConfig.getCostPerVehicleAndDay();
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
			this.vehicleId2ScoreMap.put(event.getVehicleId(), new ScoreContainer(event.getVehicleId(), this.earningsPerBoardingPassenger, this.earningsPerMeterAndPassenger, this.expensesPerMeter, this.costPerVehicleAndDay));
		}		
	}

	@Override
	public void reset(int iteration) {
		this.driverId2VehIdMap = new TreeMap<Id, Id>();
		this.vehicleId2ScoreMap = new TreeMap<Id, ScoreContainer>();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				this.vehicleId2ScoreMap.get(event.getVehicleId()).addPassenger();
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				this.vehicleId2ScoreMap.get(event.getVehicleId()).removePassenger();
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(this.driverId2VehIdMap.get(event.getPersonId()) != null){
			this.vehicleId2ScoreMap.get(this.driverId2VehIdMap.get(event.getPersonId())).handleLinkTravelled(this.net.getLinks().get(event.getLinkId()));
		}
	}
}