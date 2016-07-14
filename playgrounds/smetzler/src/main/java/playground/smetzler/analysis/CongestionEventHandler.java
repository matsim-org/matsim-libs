/* *********************************************************************** *
 * project: org.matsim.*
 * LinksEventHandler.java
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

/**
 * 
 */
package playground.smetzler.analysis;


import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

/**
 * @author Ihab, Simon
 *
 */
public class CongestionEventHandler implements  LinkLeaveEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private final Network network;
	private double pott_sec = 0;
		
	Map<Id<Person>, Double> personId2personEnterTime = new HashMap<Id<Person>, Double>();
	Map<Id<Person>, Boolean> personId2justDeparted = new HashMap<Id<Person>, Boolean>();
	
	Vehicle2DriverEventHandler vehicle2driver = new Vehicle2DriverEventHandler();

	//Map<Id, Double> onLinkTime = new HashMap<Id, Double>();
	
	public CongestionEventHandler(Network network) {
		this.network = network;
	}


	@Override
	public void reset(int iteration) {
		
	}

	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
				
		// person verlÃ¤sst link
		// suche fÃ¼r diese Person aus der Map heraus: linkEnterTime
		// berechne wie lange die Person gebraucht hat

		// berechne die free travel time
		// berechne die differenz zur free travel time
		// wirf diesen wert in den topf
		
//		if (this.personId2justDeparted.get(event.getDriverId())){
		if (this.personId2justDeparted.get(vehicle2driver.getDriverOfVehicle(event.getVehicleId()))) {
			// ignore this guy
		
		} else {
			
			Link link = this.network.getLinks().get(event.getLinkId());	
			double freeSpeedLinkduration = link.getLength() / link.getFreespeed();
//			double travelTime = event.getTime() - this.personId2personEnterTime.get(event.getDriverId());
			double travelTime = event.getTime() - this.personId2personEnterTime.get(
					vehicle2driver.getDriverOfVehicle(event.getVehicleId()));
			double diff = travelTime - freeSpeedLinkduration;
			
			this.pott_sec = pott_sec + diff;
		}
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
	
//		this.personId2justDeparted.put(event.getDriverId(), false);
//		this.personId2personEnterTime.put(event.getDriverId(), event.getTime());
		this.personId2justDeparted.put(vehicle2driver.getDriverOfVehicle(event.getVehicleId()), false);
		this.personId2personEnterTime.put(vehicle2driver.getDriverOfVehicle(event.getVehicleId()), event.getTime());
	}
	

	public void printResults() {
		System.out.println("gesamter Zeitverlust durch Stau: " + pott_sec / 3600 + "h");
	}


	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.personId2justDeparted.put(event.getPersonId(), true);
	}


	public double getPott_h() {
		return pott_sec / 3600.;
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicle2driver.handleEvent(event);
	}


	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		vehicle2driver.handleEvent(event);
	}

}
