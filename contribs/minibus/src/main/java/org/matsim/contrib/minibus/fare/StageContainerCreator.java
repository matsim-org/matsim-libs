/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.fare;

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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * Collects all information needed to calculate create a {@link StageContainer}
 * and pushes them to all registered {@link StageContainerHandler}.
 * 
 * @author aneumann
 *
 */
public final class StageContainerCreator implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, AfterMobsimListener{
	
	private final static Logger log = Logger.getLogger(StageContainerCreator.class);
	
	private Network network;
	private final String pIdentifier;
	
	private final List<StageContainerHandler> stageContainerHandlerList = new LinkedList<>();
	private HashMap<Id<Vehicle>, TransitDriverStartsEvent> vehId2TransitDriverStartsE = new HashMap<>();
	private HashMap<Id<Vehicle>, VehicleArrivesAtFacilityEvent> vehId2VehArrivesAtFacilityE = new HashMap<>();
	private HashMap<Id<Vehicle>, LinkedList<StageContainer>> vehId2StageContainerListMap = new HashMap<>();
	private HashMap<Id<Person>, StageContainer> personId2StageContainer = new HashMap<>();

	public StageContainerCreator(String pIdentifier){
		this.pIdentifier = pIdentifier;
		log.info("enabled");
	}
	
	public void init(Network network){
		this.network = network;
	}
	
	public void addStageContainerHandler(StageContainerHandler stageContainerHandler){
		this.stageContainerHandlerList.add(stageContainerHandler);
	}
	
	@Override
	public void reset(int iteration) {
		this.vehId2TransitDriverStartsE = new HashMap<>();
		this.vehId2VehArrivesAtFacilityE = new HashMap<>();
		this.vehId2StageContainerListMap = new HashMap<>();
		this.personId2StageContainer = new HashMap<>();
		
		for (StageContainerHandler stageContainerHandler : this.stageContainerHandlerList) {
			stageContainerHandler.reset();
		}
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehId2TransitDriverStartsE.put(event.getVehicleId(), event);
		if (this.vehId2StageContainerListMap.get(event.getVehicleId()) == null) {
			this.vehId2StageContainerListMap.put(event.getVehicleId(), new LinkedList<StageContainer>());
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehId2VehArrivesAtFacilityE.put(event.getVehicleId(), event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.vehId2TransitDriverStartsE.containsKey(event.getVehicleId())) {
			// It's a transit driver
			double linkLength = this.network.getLinks().get(event.getLinkId()).getLength();
			for (StageContainer stageContainer : this.vehId2StageContainerListMap.get(event.getVehicleId())) {
				stageContainer.addDistanceTravelled(linkLength);
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			// it's a paratransit vehicle
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				// it's not the driver
				StageContainer stageContainer = new StageContainer();
				stageContainer.handlePersonEnters(event, this.vehId2VehArrivesAtFacilityE.get(event.getVehicleId()), this.vehId2TransitDriverStartsE.get(event.getVehicleId()));
				this.vehId2StageContainerListMap.get(event.getVehicleId()).add(stageContainer);
				this.personId2StageContainer.put(event.getPersonId(), stageContainer);
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			// it's a paratransit vehicle
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				// it's not the driver
				StageContainer stageContainer = this.personId2StageContainer.remove(event.getPersonId());
				this.vehId2StageContainerListMap.get(event.getVehicleId()).remove(stageContainer);
				stageContainer.handlePersonLeaves(event, this.vehId2VehArrivesAtFacilityE.get(event.getVehicleId()));
				
				// call all StageContainerHandler
				for (StageContainerHandler stageContainerHandler : this.stageContainerHandlerList) {
					stageContainerHandler.handleFareContainer(stageContainer);
				}
				
				// Note the stageContainer is dropped at this point.
			}
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// ok, mobsim is done - finish incomplete entries
		if (this.personId2StageContainer.size() > 0) {
			log.warn("There are " + this.personId2StageContainer.size() + " passengers with incomplete trips. Cannot finish them. Will not forward those entries");
		}
	}
}
