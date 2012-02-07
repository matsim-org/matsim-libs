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
package playground.droeder.eMobility.v2.handler;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.v2.energy.EmobCharging;
import playground.droeder.eMobility.v2.fleet.EmobFleet;
import playground.droeder.eMobility.v2.fleet.EmobVehicle;
import playground.droeder.eMobility.v2.population.EmobPopulation;

/**
 * @author droeder
 *
 */
public class EmobPersonHandler implements AgentArrivalEventHandler, AgentDepartureEventHandler, 
										PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	
	private EmobVehicleDrivingHandler vehHandler;
	private EmobPopulation population;
	private EmobFleet fleet;
	private Map<Id, EmobCharging> chargingActs;
	private Map<Id, Id> person2veh;
	private ChargingProfiles chargingProfiles;

	public EmobPersonHandler(EmobVehicleDrivingHandler vehHandler, EmobPopulation population, EmobFleet fleet, ChargingProfiles chargingProfiles){
		this.vehHandler = vehHandler;
		this.population = population;
		this.fleet = fleet;
		this.chargingActs = new HashMap<Id, EmobCharging>();
		this.person2veh = new HashMap<Id, Id>();
		this.chargingProfiles = chargingProfiles;
	}
		
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("deprecation")
	@Override
	public void handleEvent(AgentDepartureEvent e) {
		if(this.population.containsPerson(e.getPersonId())){
			this.population.processEvent(e);
			
			/*
			 * do this only if the person has parked a car
			 * 
			 * probably this should be changed later, for example if a you try to simulate a carsharing-service or sth like that
			 */
			if(this.person2veh.containsKey(e.getPersonId())){
				EmobVehicle veh = this.fleet.getVehicle(this.person2veh.get(e.getPersonId()));
				/*
				 * if there is sth to charge and the person/car (at the moment it is the same) already left the chargingStation -> charge
				 */
				if(chargingActs.containsKey(veh.getId()) &&  (!this.population.getPerson(e.getPersonId()).atChargingStation())){
					EmobCharging c = chargingActs.remove(veh.getId());
					c.checkRealEnd(e.getTime());
					Double newSOC = this.chargingProfiles.getNewState(c.getProfile(), c.getDuration(), veh.getCurrentSoC());
					veh.setSoC(newSOC);
				}
				
			}
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent e) {
		if(this.population.containsPerson(e.getPersonId())){
			this.population.processEvent(e);

			//check if the person parked a vehicle
			if(this.person2veh.containsKey(e.getPersonId())){
				Id veh =this.person2veh.get(e.getPersonId());
				/*
				 *  if no other chargingActivity is stored and the person arrived a chargingStation,
				 *  store the current chargingAct and calculate realStartTime
				 */
				if((!chargingActs.containsKey(veh)) &&  this.population.getPerson(e.getPersonId()).atChargingStation()){
					EmobCharging c = this.population.getPerson(e.getPersonId()).getCurrentChargingParameters();
					c.setRealStart(e.getTime());
				}
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent e) {
		if(this.population.containsPerson(e.getPersonId())){
			// the person starts driving, so register the vehicle
			this.vehHandler.registerVeh(e.getVehicleId(), this.fleet.getVehicle(e.getVehicleId()), 
									this.population.getPerson(e.getPersonId()).getCurrentDischargingProfile());
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent e) {
		if(this.population.containsPerson(e.getPersonId())){
			//the person finished its ride, so deregister...
			this.vehHandler.removeVeh(e.getVehicleId(), e.getTime());
			// probably not necessary at this moment, but in future maybe the veh- and personId are not the same
			this.person2veh.put(e.getPersonId(), e.getVehicleId());
		}
	}





	
}
