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

package playground.andreas.P2.scoring.fare;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;

/**
 * 
 * Collects all information needed to calculate the fare.
 * 
 * @author aneumann
 *
 */
public class FareCollectorHandler implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, AfterMobsimListener{
	
	private final static Logger log = Logger.getLogger(FareCollectorHandler.class);
	
	private Network network;
	private String pIdentifier;
	private double earningsPerBoardingPassenger;
	private double earningsPerMeterAndPassenger;
	
	private List<FareContainerHandler> fareContainerHandlerList = new LinkedList<FareContainerHandler>();
	private HashMap<Id, TransitDriverStartsEvent> vehId2TransitDriverStartsE = new HashMap<Id, TransitDriverStartsEvent>();
	private HashMap<Id, VehicleArrivesAtFacilityEvent> vehId2VehArrivesAtFacilityE = new HashMap<Id, VehicleArrivesAtFacilityEvent>();
	private HashMap<Id, LinkedList<FareContainer>> vehId2FareContainerList = new HashMap<Id, LinkedList<FareContainer>>();
	private HashMap<Id, FareContainer> personId2FareContainer = new HashMap<Id, FareContainer>();

	public FareCollectorHandler(String pIdentifier, double earningsPerBoardingPassenger, double earningsPerMeterAndPassenger){
		this.pIdentifier = pIdentifier;
		this.earningsPerBoardingPassenger = earningsPerBoardingPassenger;
		this.earningsPerMeterAndPassenger = earningsPerMeterAndPassenger;
		log.info("enabled");
	}
	
	public void init(Network network){
		this.network = network;
	}
	
	public void addFareContainerHandler(FareContainerHandler fareContainerHandler){
		this.fareContainerHandlerList.add(fareContainerHandler);
	}
	
	@Override
	public void reset(int iteration) {
		this.vehId2TransitDriverStartsE = new HashMap<Id, TransitDriverStartsEvent>();
		this.vehId2VehArrivesAtFacilityE = new HashMap<Id, VehicleArrivesAtFacilityEvent>();
		this.vehId2FareContainerList = new HashMap<Id, LinkedList<FareContainer>>();
		this.personId2FareContainer = new HashMap<Id, FareContainer>();
		
		for (FareContainerHandler fareContainerHandler : this.fareContainerHandlerList) {
			fareContainerHandler.reset(iteration);
		}
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehId2TransitDriverStartsE.put(event.getVehicleId(), event);
		if (this.vehId2FareContainerList.get(event.getVehicleId()) == null) {
			this.vehId2FareContainerList.put(event.getVehicleId(), new LinkedList<FareContainer>());
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
			for (FareContainer fareContainer : this.vehId2FareContainerList.get(event.getVehicleId())) {
				fareContainer.addDistanceTravelled(linkLength);
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			// it's a paratransit vehicle
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				// it's not the driver
				FareContainer fareContainer = new FareContainer(this.earningsPerBoardingPassenger, this.earningsPerMeterAndPassenger);
				fareContainer.handlePersonEnters(event, this.vehId2VehArrivesAtFacilityE.get(event.getVehicleId()), this.vehId2TransitDriverStartsE.get(event.getVehicleId()));
				this.vehId2FareContainerList.get(event.getVehicleId()).add(fareContainer);
				this.personId2FareContainer.put(event.getPersonId(), fareContainer);
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			// it's a paratransit vehicle
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				// it's not the driver
				FareContainer fareContainer = this.personId2FareContainer.remove(event.getPersonId());
				this.vehId2FareContainerList.get(event.getVehicleId()).remove(fareContainer);
				fareContainer.handlePersonLeaves(event, this.vehId2VehArrivesAtFacilityE.get(event.getVehicleId()));
				
				// call all FareContainerHandler
				for (FareContainerHandler fareContainerHandler : this.fareContainerHandlerList) {
					fareContainerHandler.handleFareContainer(fareContainer);
				}
				
				// Note the fareContainer is dropped at this point.
			}
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// ok, mobsim is done - finish incomplete entries
		if (this.personId2FareContainer.size() > 0) {
			log.warn("There are " + this.personId2FareContainer.size() + " passengers with incomplete trips. Cannot finish them. Will not forward those entries");
		}
	}
}
