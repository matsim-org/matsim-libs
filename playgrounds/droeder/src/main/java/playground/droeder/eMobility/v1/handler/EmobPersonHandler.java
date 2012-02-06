/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.eMobility.v1.handler;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.GenericEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.v1.subjects.EmobilityPerson;
import playground.droeder.eMobility.v2.population.EmobPerson;

/**
 * @author droeder
 *
 */
public class EmobPersonHandler implements AgentArrivalEventHandler, AgentDepartureEventHandler{
	
	private Map<Id, EmobilityPerson> persons;
	private EmobVehicleDrivingHandler vehicleHandler;
	private ChargingProfiles cp;
	private Map<Id, Double> vehId2startCharging;
	private Map<Id, Double> vehId2finishCharging;
	private Map<Id, Id> veh2chargetype;
	private EventsManager events;

	public EmobPersonHandler(Map<Id, EmobilityPerson> persons, EmobVehicleDrivingHandler vehicleHandler, ChargingProfiles chargingProfiles, EventsManager events){
		this.persons = persons;
		this.vehicleHandler = vehicleHandler;
		this.cp = chargingProfiles;
		this.vehId2startCharging = new HashMap<Id, Double>();
		this.vehId2finishCharging = new HashMap<Id, Double>();
		this.veh2chargetype = new HashMap<Id, Id>();
		this.events = events ;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if(this.persons.containsKey(event.getPersonId())){
			EmobilityPerson p  = this.persons.get(event.getPersonId());
			//handle the vehicle
			if(event.getLegMode().equals(TransportMode.car)){
				if(! (p.getCurrentAppointment().getDisChargingType() == null)){
					this.vehicleHandler.startRide(p.getVehicle(), p.getCurrentAppointment().getDisChargingType());
				}
			}
			// increase activityCnt after every activity
			this.persons.get(event.getPersonId()).getCurrentAppointment().increase();
			if(!this.persons.get(event.getPersonId()).getCurrentAppointment().isCharging()){
				if(this.vehId2startCharging.containsKey(p.getId())){
					double start = this.vehId2startCharging.remove(p.getId());
					double end = this.vehId2finishCharging.remove(p.getId());
					if(end > event.getTime()){
						end = event.getTime();
					}
					double newCharge = this.cp.getNewState(this.veh2chargetype.get(p.getId()), end/start, this.persons.get(p.getId()).getVehicle().getChargeState());
					p.getVehicle().setNewCharge(newCharge, event.getTime());
					GenericEvent chargingStateEvent = events.getFactory().createGenericEvent("chargingState", event.getTime() ) ;
					chargingStateEvent.getAttributes().put("chargingLevel", Double.toString(newCharge) ) ;
					events.processEvent(chargingStateEvent) ;
				}
			}
		}
		
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if(this.persons.containsKey(event.getPersonId())){
			EmobilityPerson p  = this.persons.get(event.getPersonId());
			// handle the vehicle
			if(event.getLegMode().equals(TransportMode.car)){
				this.vehicleHandler.finishRide(p.getVehicle().getId(), event.getTime());
			}
			
			if(p.getCurrentAppointment().isCharging()){
				if(event.getTime() > p.getCurrentAppointment().getChargingStart()){
					this.vehId2startCharging.put(p.getId(), event.getTime());
				}else{
					this.vehId2startCharging.put(p.getId(), p.getCurrentAppointment().getChargingStart());
				}
				this.vehId2finishCharging.put(p.getId(), p.getCurrentAppointment().getChargingEnd());
				this.veh2chargetype.put(p.getId(), p.getCurrentAppointment().getChargingType());
			}
		}
		
	}
	
	
	
	

}
