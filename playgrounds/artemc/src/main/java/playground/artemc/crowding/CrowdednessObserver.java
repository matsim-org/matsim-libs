/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.artemc.crowding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.artemc.crowding.VehicleStateAdministrator;
import playground.artemc.crowding.events.PersonCrowdednessEvent;
import playground.artemc.crowding.rules.SeatAssignmentRule;
/**
 * The crowdedness observed used PersonEntersVehicleEvents and
 * PersonLeveasVehiclEvents to keep track of the crowdedness of PT-Vehicles. It
 * also tracks how long passengers are staying within a vehicle while it is
 * moving. As soon as passengers enter or leave the vehicle, all passengers that
 * were in the vehicle will be notified that they have been in a crowded vehicle
 * for a certain amount of time. This is done by generating
 * PersonCrowdednessEvents.
 * 
 * TODO: Probably it is not a problem in most cases, but the driver takes one
 * seat.
 * 
 * @author nagel
 * @author pbouman
 * 
 * A map ("vehicleStates") has been added to collect the data of the seat administration
 * (travel times and occupancy data).
 * 
 * boardingOrAlightingAtFacility is used to include the dwell time in PersonCrowdednessEvent 
 * 
 * nextAgentAlighting is used for the Second-Pricing function. If not activate, only the first agent
 * alighting at a facility has to pay a fare 
 * 
 * @author grerat
 * 
 */
public class CrowdednessObserver implements
VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler,
PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private EventsManager ev;
	private Scenario sc;
	private Vehicles vehs;
	private SeatAssignmentRule rule;

	private Set<Id> ptVehicles;

	private HashMap<Vehicle, VehicleStateAdministrator> vehicleStates;

	private Set<Vehicle> pendingObservation;
	private Set<Id> nextAgentAlighting;
	private Map<Vehicle,Double> lastDeparture;
	private Map<Vehicle,Double> lastArrival;
	private Map<Vehicle, Boolean> boardingOrAlightingAtFacility;
	

	public CrowdednessObserver( Scenario sc, EventsManager ev , SeatAssignmentRule rule) {
		this.sc = sc ;
		this.ev = ev ;
		this.vehs = ((ScenarioImpl)this.sc).getVehicles() ;
		this.rule = rule;
		this.vehicleStates = new LinkedHashMap<Vehicle,VehicleStateAdministrator>();
		this.pendingObservation = new HashSet<Vehicle>();
		this.nextAgentAlighting = new HashSet<Id>();
		this.ptVehicles = new HashSet<Id>();
		this.lastDeparture = new HashMap<Vehicle,Double>();
		this.lastArrival = new HashMap<Vehicle, Double>();
		this.boardingOrAlightingAtFacility = new HashMap<Vehicle, Boolean>();
		collectPtVehicles();
	}

	private void collectPtVehicles()
	{
		ptVehicles.clear();
		for (TransitLine tsl : sc.getTransitSchedule().getTransitLines().values())
		{
			for (TransitRoute tr : tsl.getRoutes().values())
			{
				for (Departure dep : tr.getDepartures().values())
				{
					ptVehicles.add(dep.getVehicleId());
				}
			}
		}
	}

	private Vehicle getVehicle(Id vehicleId)
	{
		return this.vehs.getVehicles().get(vehicleId);
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event)
	{

		if (!ptVehicles.contains(event.getVehicleId()))
		{
			return;
		}

		Vehicle v = getVehicle(event.getVehicleId());
	
		double time = event.getTime();
		Id facility = event.getFacilityId();

		VehicleStateAdministrator vehicle = vehicleStates.get(v);
		vehicle.addBusFacilityDeparture(facility, time);
		
		if(facility.toString().equals("1")){
			vehicleStates.get(v).setRouteDepartureTime(time);
		}
		
		// TRICKY! Here, there is no personId available. FacilityId is written in the event, 
		// but there is no consequence on the scoring functions, because the personId is not called
		// (grerat)
		if(boardingOrAlightingAtFacility.get(v).equals(true)){
			pushEvents(event.getFacilityId(), event.getVehicleId(), event.getTime(), false);	
		}
		
		if (v != null)
		{
			// If there is already a pending observation and we observe
			// another departure, no one has left or entered the vehicle 
			// in the meantime. This implies that no events were generated.
			// We thus do not need to update the lastDeparture.
			if (!pendingObservation.contains(v))
			{
				lastDeparture.put(v, event.getTime());
				pendingObservation.add(v);
			}
		}
	}
	
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {

		if (!ptVehicles.contains(event.getVehicleId())) {
			return;
		}

		Vehicle v = getVehicle(event.getVehicleId());
		double time = event.getTime();
		Id facility = event.getFacilityId();

		VehicleStateAdministrator vehicle = vehicleStates.get(v);
		vehicle.addBusFacilityArrival(facility, time);
	}

	private void pushEvents(Id personId, Id vehicleId, double time, boolean leavingVehicle)
	{
		Vehicle v = this.vehs.getVehicles().get(vehicleId);
	
		if (v != null)
		{
			
			// Check whether there was a departure in the past. If so
			if (pendingObservation.contains(v))
			{

				// Get the administration

				VehicleStateAdministrator vehicleAdmin = vehicleStates.get(v);
				
				// Get the last facility saved (the facility where the agent is)
				Set<Entry<Id, BusFacilityInteractionEvent>> mapValues = vehicleAdmin.getFacilityStates().entrySet();
				    int maplength = mapValues.size();
				    Entry<Id, BusFacilityInteractionEvent>[] test = new Entry[maplength];
				    mapValues.toArray(test);
				    Id stationId = test[maplength-1].getKey();
				    	    
				double seatCrwd = vehicleAdmin.getSeatCrowdedness();
				double standCrwd = vehicleAdmin.getStandingCrowdedness();
				double totalCrwd = vehicleAdmin.getTotalCrowdedness();
				double loadFactor = vehicleAdmin.getLoadFactor();
				int sitters = vehicleAdmin.getSittingSet().size();
				int standees = vehicleAdmin.getStandingSet().size();
				
				// Get the time difference between now and the last departure
				// we have registered.
				double travelTime = time - lastDeparture.get(v);
				double dwellTime = 0;
				

				// Now if any persons are in the vehicle
				if (vehicleAdmin != null && !boardingOrAlightingAtFacility.get(v).equals(true))
				{
					for (Id person : vehicleAdmin.getSittingSet())
					{	
						// Send the people who are sitting an Event that tells them they have been sitting.
						// If an agent is leaving, send this agent an event that tell him he is leaving.
						if(person.equals(personId) && leavingVehicle==true){
							ev.processEvent(new PersonCrowdednessEvent(time, person, vehicleId, stationId, travelTime, dwellTime, true, true, sitters, standees, seatCrwd, standCrwd, totalCrwd, loadFactor) );
						}
						else {
							ev.processEvent(new PersonCrowdednessEvent(time, person, vehicleId, stationId, travelTime, dwellTime, true, false, sitters, standees, seatCrwd, standCrwd, totalCrwd, loadFactor) );
						}
					}
					for (Id person : vehicleAdmin.getStandingSet())
					{
						//Send the people who are standing an Event that tells them they have been standing.
						if(person.equals(personId) && leavingVehicle==true){
						ev.processEvent(new PersonCrowdednessEvent(time, person, vehicleId, stationId, travelTime, dwellTime, false, true, sitters, standees, seatCrwd, standCrwd, totalCrwd, loadFactor) );				
						}
						else {
							ev.processEvent(new PersonCrowdednessEvent(time, person, vehicleId, stationId, travelTime, dwellTime, false, false, sitters, standees, seatCrwd, standCrwd, totalCrwd, loadFactor) );				
						}
						}
				}

				pendingObservation.remove(v);
			}

			// Send an event that give the vehicle state during dwell time
			if(boardingOrAlightingAtFacility.get(v).equals(true)) {

				// Get the administration

				VehicleStateAdministrator vehicleAdmin = vehicleStates.get(v);

				// Get the last facility saved (the facility where the agent is)
				Set<Entry<Id, BusFacilityInteractionEvent>> mapValues = vehicleAdmin.getFacilityStates().entrySet();
				int maplength = mapValues.size();
				Entry<Id, BusFacilityInteractionEvent>[] test = new Entry[maplength];
				mapValues.toArray(test);
				Id stationId = test[maplength-1].getKey();

				double seatCrwd = vehicleAdmin.getSeatCrowdedness();
				double standCrwd = vehicleAdmin.getStandingCrowdedness();
				double totalCrwd = vehicleAdmin.getTotalCrowdedness();
				double loadFactor = vehicleAdmin.getLoadFactor();
				int sitters = vehicleAdmin.getSittingSet().size();
				int standees = vehicleAdmin.getStandingSet().size();

				// Get the time difference between now and the last departure we have registered.
				double travelTime = 0;
				double dwellTime = time - lastArrival.get(v);


				// Now if any persons are in the vehicle
				if (vehicleAdmin != null)
				{
					for (Id person : vehicleAdmin.getSittingSet())
					{
						// Send the people who are sitting an Event that tells them they have been sitting.
						ev.processEvent(new PersonCrowdednessEvent(time, person, vehicleId, stationId, travelTime, dwellTime, true, false, sitters, standees, seatCrwd, standCrwd, totalCrwd, loadFactor) );
					}
					for (Id person : vehicleAdmin.getStandingSet())
					{
						// Send the people who are standing an Event that tells them they have been standing.
						ev.processEvent(new PersonCrowdednessEvent(time, person, vehicleId, stationId, travelTime, dwellTime, false, false, sitters, standees, seatCrwd, standCrwd, totalCrwd, loadFactor) );				
					}
				}

				boardingOrAlightingAtFacility.put(v, false);		
			}
			
			// To allow scoring of the following agents alighting (only useful for SecondBestPricing)
			// MUST BE HIDDEN FOR OTHERS SIMULATIONS
//			if(nextAgentAlighting.contains(personId)){
//				VehicleStateAdministrator vehicleAdmin = vehicleStates.get(v);
//
//				// Get the last facility saved (the facility where the agent is)
//				Set<Entry<Id, BusFacilityInteractionEvent>> mapValues = vehicleAdmin.getFacilityStates().entrySet();
//				int maplength = mapValues.size();
//				Entry<Id, BusFacilityInteractionEvent>[] test = new Entry[maplength];
//				mapValues.toArray(test);
//				Id stationId = test[maplength-1].getKey();
//				
//				// These calculation don't take in account the agents already alighted. But value not use for SecondBestPricing
//				double seatCrwd = vehicleAdmin.getSeatCrowdedness();
//				double standCrwd = vehicleAdmin.getStandingCrowdedness();
//				double totalCrwd = vehicleAdmin.getTotalCrowdedness();
//				double loadFactor = vehicleAdmin.getLoadFactor();
//				int sitters = vehicleAdmin.getSittingSet().size();
//				int standees = vehicleAdmin.getStandingSet().size();
//
//				// Already added when the first agent has alighted
//				double travelTime = 0;
//				double dwellTime = 0;
//
//				ev.processEvent(new PersonCrowdednessEvent(time, personId, vehicleId, stationId, travelTime, dwellTime, true, true, sitters, standees, seatCrwd, standCrwd, totalCrwd, loadFactor) );
//				nextAgentAlighting.remove(personId);
//			}
		}
	}

	@Override
	public void reset(int iteration) {
		vehicleStates.clear();
		pendingObservation.clear();
		lastDeparture.clear();
		lastArrival.clear();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {

		if (!ptVehicles.contains(event.getVehicleId()))
		{
			return;
		}


		Id vehId = event.getVehicleId() ;
		Vehicle v = this.vehs.getVehicles().get(vehId) ;

		// Somehow, cars return null, but it would be better
		// to distinguish PT-vehicles and cars in a more clever way.
		if (v != null)
		{
			Id person = event.getPersonId();

			//if(!boardingOrAlightingAtFacility.containsKey(v)){
				boardingOrAlightingAtFacility.put(v, false);
			//}
				
			// If any observations are pending, push PersonCrowdednessEvents before updating the seat administration.
			pushEvents(person, vehId, event.getTime(), false);
			
			//if(!boardingOrAlightingAtFacility.containsKey(v)){
			boardingOrAlightingAtFacility.put(v, true);
			lastArrival.put(v, event.getTime());
			//}

			VehicleStateAdministrator vehicleAdmin = vehicleStates.get(v);
			if (vehicleAdmin == null)
			{
				vehicleAdmin = new VehicleStateAdministrator(v,rule);
				vehicleStates.put(v, vehicleAdmin);
			}

			vehicleAdmin.addPerson(person);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event)
	{

		if (!ptVehicles.contains(event.getVehicleId()))
		{
			return;
		}

		Id vehId = event.getVehicleId() ;
		Vehicle v = this.vehs.getVehicles().get(vehId);
		
		if (v != null)
		{
			Id person = event.getPersonId();
			boardingOrAlightingAtFacility.put(v, false);
			
			// Used for the implementation of the Second-Best pricing strategy. 
			// No effect on the other simulations 
			if (!pendingObservation.contains(v))
			{
				nextAgentAlighting.add(person);
			}
			
			// If any observations are pending, push PersonCrowdednessEvents before updating the seat administration.
			pushEvents(person, vehId, event.getTime(), true);

			boardingOrAlightingAtFacility.put(v, true);
			lastArrival.put(v, event.getTime());
			
			vehicleStates.get(v).remove(person);
		
		}
	}
		
	public HashMap<Vehicle, VehicleStateAdministrator> getVehicleStates() {
		return vehicleStates;
	}

}

