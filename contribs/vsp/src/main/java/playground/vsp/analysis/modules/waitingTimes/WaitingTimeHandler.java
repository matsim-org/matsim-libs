/* *********************************************************************** *
 * project: org.matsim.*
 * InVehWaitHandler.java
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

/**
 * 
 */
package playground.vsp.analysis.modules.waitingTimes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;

import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;

/**
 * 
 * @author ikaddoura
 *
 */
public class WaitingTimeHandler implements PersonEntersVehicleEventHandler, PersonDepartureEventHandler, VehicleArrivesAtFacilityEventHandler, TransitDriverStartsEventHandler {
	private PtDriverIdAnalyzer ptDriverIdAna;
	private List<Id> vehicleIDs = new ArrayList<Id>();

	private List <Double> waitingTimes = new ArrayList<Double>();
	private Map <Id, List<Double>> personId2waitingTimes = new HashMap<Id, List<Double>>();
	private Map <Id, List<Double>> facilityId2waitingTimes = new HashMap<Id, List<Double>>();
	
	private Map <Id, Double> personId2PersonEntersVehicleTime = new HashMap<Id, Double>();
	private Map <Id, Double> personId2AgentDepartureTime = new HashMap<Id, Double>();
	private Map <Id, Id> busId2currentFacilityId = new HashMap<Id, Id>();
	
	public WaitingTimeHandler(PtDriverIdAnalyzer ptDriverIdAna) {
		this.ptDriverIdAna = ptDriverIdAna;
	}

	@Override
	public void reset(int iteration) {
		this.vehicleIDs.clear();
		this.waitingTimes.clear();
		this.personId2waitingTimes.clear();
		this.facilityId2waitingTimes.clear();
		this.personId2PersonEntersVehicleTime.clear();
		this.personId2AgentDepartureTime.clear();
		this.busId2currentFacilityId.clear();
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id personId = event.getPersonId();
		Id vehId = event.getVehicleId();
		if (this.ptDriverIdAna.isPtDriver(personId)){
		 // pt driver
	
		} else {
			
			if (vehicleIDs.contains(vehId)){
				
				// person is entering a public vehicle
				personId2PersonEntersVehicleTime.put(personId, event.getTime());
				
				double waitingTime;
				if (personId2AgentDepartureTime.get(personId) == null){
					throw new RuntimeException("Person " + personId + " is entering vehicle " + vehId + " without having departed from an activity. Aborting...");
				} else {
					waitingTime =  event.getTime() - personId2AgentDepartureTime.get(personId);
				}
				
				waitingTimes.add(waitingTime);
				
				// save waitingTimes per person
				if (this.personId2waitingTimes.get(personId) == null){
					List<Double> waitingTimes = new ArrayList<Double>();
					waitingTimes.add(waitingTime);
					this.personId2waitingTimes.put(personId, waitingTimes);
				} else {
					List<Double> waitingTimes = this.personId2waitingTimes.get(personId);
					waitingTimes.add(waitingTime);
					this.personId2waitingTimes.put(personId, waitingTimes);
				}
				
				// save waitingTime per stop
				Id currentFacilityId = this.busId2currentFacilityId.get(vehId);

				if (this.facilityId2waitingTimes.get(currentFacilityId) == null){
					List<Double> waitingTimes = new ArrayList<Double>();
					waitingTimes.add(waitingTime);
					this.facilityId2waitingTimes.put(currentFacilityId, waitingTimes);
				} else {
					List<Double> waitingTimes = this.facilityId2waitingTimes.get(currentFacilityId);
					waitingTimes.add(waitingTime);
					this.facilityId2waitingTimes.put(currentFacilityId, waitingTimes);
				}	
				
			} else {
				// person is not entering a public vehicle
			}
		} 
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id personId = event.getPersonId();
		if (this.ptDriverIdAna.isPtDriver(personId)){
			// pt driver
		} else {
			if (event.getLegMode().toString().equals(TransportMode.pt)){
				personId2AgentDepartureTime.put(personId, event.getTime());
			} else {
				// not a pt Leg
			}
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id vehId = event.getVehicleId();
		Id facilityId = event.getFacilityId();
		this.busId2currentFacilityId.put(vehId, facilityId);
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		Id vehicleId = event.getVehicleId();
		if (this.vehicleIDs.contains(vehicleId)){
			// vehicleID bereits in Liste
		}
		else{
			this.vehicleIDs.add(vehicleId);
		}
	}

	public List <Double> getWaitingTimes() {
		return waitingTimes;
	}
	
	public Map <Id, List<Double>> getPersonId2waitingTimes() {
		return personId2waitingTimes;
	}
	
	public Map <Id, List<Double>> getFacilityId2waitingTimes() {
		return facilityId2waitingTimes;
	}
	
}
