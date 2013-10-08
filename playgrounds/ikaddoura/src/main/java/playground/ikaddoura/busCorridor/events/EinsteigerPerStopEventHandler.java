/* *********************************************************************** *
 * project: org.matsim.*
 * BusCorridorEventHandler.java
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
package playground.ikaddoura.busCorridor.events;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;

/**
 * @author Ihab
 *
 */

public class EinsteigerPerStopEventHandler implements PersonEntersVehicleEventHandler, VehicleArrivesAtFacilityEventHandler {
	
	Map <Id, Id> vehicleId2stopId = new HashMap<Id, Id>();
	Map <Id, Integer> stopId2einsteiger = new HashMap<Id, Integer>();

	Scenario scenario;
	
	public EinsteigerPerStopEventHandler() {
	}
	
	public void reset(int iteration) {
		// wird am Ende jeder Iteration ausgeführt (zum resetten von Variablen...)
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (event.getPersonId().toString().contains("bus")){
			
		}
		else {
//			System.out.println("Person "+event.getPersonId()+" enters Vehicle "+event.getVehicleId()+" at Facility "+vehicleId2stopId.get(event.getVehicleId())+".");
			Id vehicleId = event.getVehicleId();
			Id stopId = vehicleId2stopId.get(vehicleId);
			Integer einsteigerBisher = 0;
			if (stopId2einsteiger.containsKey(stopId)){
				einsteigerBisher = stopId2einsteiger.get(stopId);
			}
			else {}
			Integer einsteigerNew = einsteigerBisher + 1;
			stopId2einsteiger.put(stopId, einsteigerNew);
	//		System.out.println(Time.writeTime(event.getTime(), Time.TIMEFORMAT_HHMMSS) +" Uhr: Link "+event.getLinkId()+ " wurde von "+event.getPersonId()+" verlassen.");
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
//		System.out.println("Vehicle "+event.getVehicleId()+" arrives at "+event.getFacilityId()+".");

		vehicleId2stopId.put(event.getVehicleId(), event.getFacilityId());
	}

	/**
	 * @return the stopId2einsteiger
	 */
	public Map<Id, Integer> getStopId2einsteiger() {
		return stopId2einsteiger;
	}
	
	
}
