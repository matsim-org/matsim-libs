/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyThrowEventHandler.java
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
package playground.ikaddoura.internalizationPt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.vehicles.Vehicle;



/**
 * Throws InVehicleDelayEvents to indicate that an agent entering or leaving a public vehicle delayed passengers being in that public vehicle.
 * 
 * External effects to be considered in future:
 * TODO: Capacity constraints.
 * 
 * IMPORTANT Assumptions:
 * 1) The scheduled dwell time at transit stops is 0sec. TODO: Get dwell time from schedule and account for dwell times >0sec.
 * 2) The door operation mode of public vehicles is serial. TODO: Adjust for parallel door operation mode.
 * 3) Public vehicles start with no delay. The slack time at the end of a transit route has to be larger than the max. delay of public vehicles.
 * 4) Agents board the first arriving public vehicle. The vehicle capacity has to be larger than the max. number of passengers.
 * 
 * Note: Whenever a pt vehicle stops at a transit stop due to at least one agent boarding or alighting,
 * the pt vehicle will be delayed by additional 2 sec, e.g. 1 sec before and 1 sec after agents are entering and leaving.
 * These seconds can be interpret as "doors opening" and "doors closing" time.
 * It is assumed that these extra delay are caused by ALL agents entering and leaving the vehicle at the current transit stop.
 * The delay per person is calculated by dividing the sum of "door opening" and "door closing" time by the total number of transfering agents.
 * Another possibility (NOT implemented): Let the first transfering agent pay for these extra delay. But this may cause weird competition
 * not to be the first boarding or alighting agent... 
 * 
 * @author Ihab
 *
 */
public class TransferDelayInVehicleHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {
//	private final static Logger log = Logger.getLogger(TransferDelayInVehicleHandler.class);

	// extra delay for a bus before and after agents are entering or leaving a public vehicle
	private final double doorOpeningTime = 1.0;
	private final double doorClosingTime = 1.0;
	
	private final MutableScenario scenario;
	private final EventsManager events;
	private final List<Id<Person>> ptDriverIDs = new ArrayList<>();
	private final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<>();
	private final Map<Id<Vehicle>, Integer> vehId2passengers = new HashMap<>();
	
	private final Map<Id<Vehicle>, Integer> vehId2agentsInBusBeforeStop = new HashMap<>();
	private final Map<Id<Vehicle>, List<Id<Person>>> vehId2agentsBoardingAtThisStop = new HashMap<>();
	private final Map<Id<Vehicle>, List<Id<Person>>> vehId2agentsAlightingAtThisStop = new HashMap<>();
	
	public TransferDelayInVehicleHandler(EventsManager events, MutableScenario scenario) {
		this.events = events;
		this.scenario = scenario;
	}
	
	@Override
	public void reset(int iteration) {
		this.vehId2passengers.clear();
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
		this.vehId2agentsInBusBeforeStop.clear();
		this.vehId2agentsBoardingAtThisStop.clear();
		this.vehId2agentsAlightingAtThisStop.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
				
		if (!this.ptDriverIDs.contains(event.getDriverId())){
			this.ptDriverIDs.add(event.getDriverId());
		}
		
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
				
		if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){
			
			// remember who was boarding and alighting at this stop.
			List<Id<Person>> agentsTransferingAtThisStop;
			if (this.vehId2agentsBoardingAtThisStop.get(event.getVehicleId()) == null){
				agentsTransferingAtThisStop = new ArrayList<>();
			} else {
				// TODO: addAll
				agentsTransferingAtThisStop = this.vehId2agentsBoardingAtThisStop.get(event.getVehicleId());
			}
			agentsTransferingAtThisStop.add(event.getPersonId());
			this.vehId2agentsBoardingAtThisStop.put(event.getVehicleId(), agentsTransferingAtThisStop);
													
			double delay = this.scenario.getTransitVehicles().getVehicles().get(event.getVehicleId()).getType().getAccessTime();
			int delayedPassengers_inVeh = calcDelayedPassengersInVeh(event.getVehicleId());
			TransferDelayInVehicleEvent delayInVehicleEvent = new TransferDelayInVehicleEvent(event.getPersonId(), event.getVehicleId(), event.getTime(), delayedPassengers_inVeh, delay);
			this.events.processEvent(delayInVehicleEvent);
			
			// update number of passengers in vehicle after calculating the external effect
			if (this.vehId2passengers.containsKey(event.getVehicleId())){
				int passengersInVeh = this.vehId2passengers.get(event.getVehicleId());
				this.vehId2passengers.put(event.getVehicleId(), passengersInVeh + 1);
			} else {
				this.vehId2passengers.put(event.getVehicleId(), 1);
			}
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		
		if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){		
			
			// remember who was boarding and alighting at this stop.
			List<Id<Person>> agentsTransferingAtThisStop;
			if (this.vehId2agentsAlightingAtThisStop.get(event.getVehicleId()) == null){
				agentsTransferingAtThisStop = new ArrayList<>();
			} else {
				// TODO: addAll
				agentsTransferingAtThisStop = this.vehId2agentsAlightingAtThisStop.get(event.getVehicleId());
			}
			agentsTransferingAtThisStop.add(event.getPersonId());
			this.vehId2agentsAlightingAtThisStop.put(event.getVehicleId(), agentsTransferingAtThisStop);
			
			// update number of passengers in vehicle before throwing delay event
			int passengersInVeh = this.vehId2passengers.get(event.getVehicleId());
			this.vehId2passengers.put(event.getVehicleId(), passengersInVeh - 1);
			
			double delay = this.scenario.getTransitVehicles().getVehicles().get(event.getVehicleId()).getType().getEgressTime();
			int delayedPassengers_inVeh = calcDelayedPassengersInVeh(event.getVehicleId());
			
			// throw delay event
			TransferDelayInVehicleEvent delayInVehicleEvent = new TransferDelayInVehicleEvent(event.getPersonId(), event.getVehicleId(), event.getTime(), delayedPassengers_inVeh, delay);
			this.events.processEvent(delayInVehicleEvent);
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		// Vehicle has arrived at transit stop. Reset all informations for that vehicle.
		this.vehId2agentsInBusBeforeStop.remove(event.getVehicleId());

		this.vehId2agentsBoardingAtThisStop.remove(event.getVehicleId());
		this.vehId2agentsAlightingAtThisStop.remove(event.getVehicleId());
		
		this.vehId2agentsInBusBeforeStop.put(event.getVehicleId(), this.vehId2passengers.get(event.getVehicleId()));
	}
	
	private int calcDelayedPassengersInVeh(Id<Vehicle> vehId) {
		int delayedPassengersInVeh = 0;
		if (this.vehId2passengers.containsKey(vehId)) {
			delayedPassengersInVeh = this.vehId2passengers.get(vehId);
		}
		return delayedPassengersInVeh;
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		// vehicle departs at facility. Throw extra delay events for the doors opening and closing time
		int numberOfAgentsBoarding = 0;
		int numberOfAgentsAlighting = 0;
		
		if (!(this.vehId2agentsBoardingAtThisStop.get(event.getVehicleId()) == null)){
			List<Id<Person>> agentsBoardingAtThisStop = this.vehId2agentsBoardingAtThisStop.get(event.getVehicleId());
			numberOfAgentsBoarding = agentsBoardingAtThisStop.size();	
		}
		
		if (!(this.vehId2agentsAlightingAtThisStop.get(event.getVehicleId()) == null)){
			List<Id<Person>> agentsAlightingAtThisStop = this.vehId2agentsAlightingAtThisStop.get(event.getVehicleId());
			numberOfAgentsAlighting = agentsAlightingAtThisStop.size();	
		}
		
		int numberOfAgentsTransfering = numberOfAgentsBoarding + numberOfAgentsAlighting;
		double delayPerPerson = (this.doorClosingTime + this.doorOpeningTime) / numberOfAgentsTransfering;

		if (!(this.vehId2agentsBoardingAtThisStop.get(event.getVehicleId()) == null)){
			List<Id<Person>> agentsBoardingAtThisStop = this.vehId2agentsBoardingAtThisStop.get(event.getVehicleId());
			
			if (!(agentsBoardingAtThisStop.size() == 0)) {
				
				int affectedAgents = 0;
				if (!(this.vehId2agentsInBusBeforeStop.get(event.getVehicleId()) == null)){
					affectedAgents = this.vehId2agentsInBusBeforeStop.get(event.getVehicleId()) - numberOfAgentsAlighting;
				}
				
				for (Id<Person> personId : agentsBoardingAtThisStop){
					TransferDelayInVehicleEvent delayInVehicleEvent = new TransferDelayInVehicleEvent(personId, event.getVehicleId(), event.getTime(), affectedAgents, delayPerPerson);
					this.events.processEvent(delayInVehicleEvent);
				}
			}
		}
		
		if (!(this.vehId2agentsAlightingAtThisStop.get(event.getVehicleId()) == null)){
			List<Id<Person>> agentsAlightingAtThisStop = this.vehId2agentsAlightingAtThisStop.get(event.getVehicleId());
			
			if (!(agentsAlightingAtThisStop.size() == 0)) {
				
				int affectedAgents = 0;
				if (!(this.vehId2passengers.get(event.getVehicleId()) == null)){
					affectedAgents = this.vehId2passengers.get(event.getVehicleId()) - numberOfAgentsBoarding;
				}
				
				for (Id<Person> personId : agentsAlightingAtThisStop){
					TransferDelayInVehicleEvent delayInVehicleEvent = new TransferDelayInVehicleEvent(personId, event.getVehicleId(), event.getTime(), affectedAgents, delayPerPerson);
					this.events.processEvent(delayInVehicleEvent);
				}
			}
		}
	}
	
}