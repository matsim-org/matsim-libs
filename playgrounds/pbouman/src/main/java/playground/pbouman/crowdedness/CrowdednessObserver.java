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
package playground.pbouman.crowdedness;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.pbouman.crowdedness.events.PersonCrowdednessEvent;
import playground.pbouman.crowdedness.rules.SeatAssignmentRule;

/**
 * The crowdedness observed used PersonEntersVehicleEvents and PersonLeveasVehiclEvents
 * to keep track of the crowdedness of PT-Vehicles. It also tracks how long passengers
 * are staying within a vehicle while it is moving. As soon as passengers enter or leave
 * the vehicle, all passengers that were in the vehicle will be notified that they have
 * been in a crowded vehicle for a certain amount of time. This is done by generating
 * PersonCrowdednessEvents.
 * 
 * TODO: Probably it is not a problem in most cases, but the driver takes one seat.
 * 
 * @author nagel
 * @author pbouman
 */
public class CrowdednessObserver implements
		VehicleDepartsAtFacilityEventHandler, 
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	
	private EventsManager ev;
	private Scenario sc;
	private Vehicles vehs;
	private SeatAssignmentRule rule;
	
	private Set<Id> ptVehicles;
	
	private Map<Vehicle,SeatAdministration> personsInVehicles;

	private Set<Vehicle> pendingObservation;
	private Map<Vehicle,Double> lastDeparture;
	
	public CrowdednessObserver( Scenario sc, EventsManager ev , SeatAssignmentRule rule) {
		this.sc = sc ;
		this.ev = ev ;
		this.vehs = ((MutableScenario)this.sc).getTransitVehicles() ;
		this.rule = rule;
		this.personsInVehicles = new HashMap<Vehicle,SeatAdministration>();
		this.pendingObservation = new HashSet<Vehicle>();
		this.ptVehicles = new HashSet<Id>();
		this.lastDeparture = new HashMap<Vehicle,Double>();
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
	
	private void pushEvents(Id vehicleId, double time)
	{
		Vehicle v = this.vehs.getVehicles().get(vehicleId);
	
		if (v != null)
		{
		
			// Check whether there was a departure in the past. If so
			if (pendingObservation.contains(v))
			{
				
				// Get the administration
				
				SeatAdministration personsInVehicle = personsInVehicles.get(v);
				
				double seatCrwd = personsInVehicle.getSeatCrowdedness();
				double standCrwd = personsInVehicle.getStandingCrowdedness();
				double totalCrwd = personsInVehicle.getTotalCrowdedness();
				
				// Get the time difference between now and the last departure
				// we have registered.
				double duration = time - lastDeparture.get(v);
				
				// Now if any persons are in the vehicle
				if (personsInVehicle != null)
				{
					for (Id person : personsInVehicle.getSittingSet())
					{
						// Send the people who are sitting an Event that tells them they have been sitting.
						ev.processEvent(new PersonCrowdednessEvent(time, person, vehicleId, duration, true, seatCrwd, standCrwd, totalCrwd) );
					}
					for (Id person : personsInVehicle.getStandingSet())
					{
						// Send the people who are standing an Event that tells them they have been standing.
						ev.processEvent(new PersonCrowdednessEvent(time, person, vehicleId, duration, false, seatCrwd, standCrwd, totalCrwd) );				
					}
				}
				
				pendingObservation.remove(v);
			}
			
		}
	}

	@Override
	public void reset(int iteration) {
		personsInVehicles.clear();
		pendingObservation.clear();
		lastDeparture.clear();
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
			
			// If any observations are pending, push PersonCrowdednessEvents before updating the seat administration.
			pushEvents(vehId, event.getTime());
			
			SeatAdministration personsInVehicle = personsInVehicles.get(v);
			if (personsInVehicle == null)
			{
				personsInVehicle = new SeatAdministration(v,rule);
				personsInVehicles.put(v, personsInVehicle);
			}
			
			personsInVehicle.addPerson(person);
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
			
			// If any observations are pending, push PersonCrowdednessEvents before updating the seat administration.
			pushEvents(vehId, event.getTime());
			
			personsInVehicles.get(v).remove(person);
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}


}
