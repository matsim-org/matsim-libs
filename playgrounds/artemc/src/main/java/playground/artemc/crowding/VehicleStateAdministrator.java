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

package playground.artemc.crowding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import playground.artemc.crowding.rules.SeatAssignmentRule;

/**
 * The VehicleStateAdministrator class is responsible to keep track of who
 * are sitting and who are standing in a certain vehicle.
 * Furthermore, it keeps the map of all interaction events between the vehicle and the facility/stop/station. 
 * 
 * In principle, this process can be extremely complicated, as it
 * may depend on the layout of seats within the vehicle, the number
 * of doors, etc. We allow some flexibility by letting the user
 * specify a SeatAssingmentRule, that decides when someone will
 * get a seat upon entering the vehicle and whether someone who
 * is standing will sit down once someone who was sitting is
 * leaving the vehicle.
 * 
 * @author pbouman, achakirov, guillaumer
**/

public class VehicleStateAdministrator
{
	
	private Vehicle vehicle;
	
	private List<Id> sittingPersons;
	private List<Id> standingPersons;
	private List<Id> boardingAtFacility;
	private List<Id> alightingAtFacility;
	
	private double routeDepartureTime;
	
	private LinkedHashMap<Double, LinkedHashMap<Id,BusFacilityInteractionEvent>> facilityStatesForRouteStartTimes;
	private LinkedHashMap<Id,BusFacilityInteractionEvent> facilityStates;

	public LinkedHashMap<Id, BusFacilityInteractionEvent> getFacilityStates() {
		return facilityStates;
	}

	private SeatAssignmentRule rule;
	
	/**
	 * Creates a SeatAdministration for a given vehicle using a certain
	 * decision rule to assign passengers to seats.
	 * @param vehicle
	 * @param rule
	 */
	
	public VehicleStateAdministrator(Vehicle vehicle, SeatAssignmentRule rule)
	{
		this.vehicle = vehicle; 
		this.rule = rule;
		this.sittingPersons = new ArrayList<Id>();
		this.standingPersons = new ArrayList<Id>();
		this.boardingAtFacility = new ArrayList<Id>();
		this.alightingAtFacility = new ArrayList<Id>();
		this.facilityStates = new LinkedHashMap<Id,BusFacilityInteractionEvent>();
		this.facilityStatesForRouteStartTimes = new LinkedHashMap<Double, LinkedHashMap<Id,BusFacilityInteractionEvent>>();
	}
	
	
	public Integer getRemainingSeatCapacity()
	{
		return vehicle.getType().getCapacity().getSeats() - sittingPersons.size();
	}
	
	public Integer getRemainingStandingCapacity()
	{
		return vehicle.getType().getCapacity().getStandingRoom() - standingPersons.size();
	}
	
	
	public Integer getRemainingTotalCapacity()
	{
		return getRemainingSeatCapacity() + getRemainingStandingCapacity();
	}

	/**
	 * 
	 * @return A list of people who are currently standing.
	 */
	public List<Id> getStandingSet()
	{
		return Collections.unmodifiableList(standingPersons);
	}
	
	/**
	 *
	 * @return A list of people who currently sitting
	 */
	public List<Id> getSittingSet()
	{
		return Collections.unmodifiableList(sittingPersons);
	}
	
	/**
	 * This method removes a person from the vehicle.
	 * If the person is currently sitting and there are people standing,
	 * this may trigger the event that someone who was standing will sit.
	 * (Using the SeatAssignmentRule)
	 * @param person the person who is leaving
	 */
	public void remove(Id person)
	{	
		alightingAtFacility.add(person);
		
		if (sittingPersons.remove(person))	
		{	
			if (standingPersons.size() > 0)
			{
				Id sitter = rule.giveSeatOnLeave(person, vehicle, sittingPersons.size(), Collections.unmodifiableList(standingPersons));
				if (sitter != null)
				{
					standingPersons.remove(sitter);
					sittingPersons.add(sitter);
				}
			}
		}
		else
		{
			standingPersons.remove(person);		
		}
	}
	
	private void addSitting(Id person)
	{
		if (getRemainingTotalCapacity() < 1)
		{
			//log.error("Agent '"+person+"' is registered as sitting in the SeatAdministration for Vehicle '"+vehicle.getId()+"', while there is no room left.");
		}
		sittingPersons.add(person);
	}
	
	private void addStanding(Id person)
	{
		if (getRemainingTotalCapacity() < 1)
		{
			//log.error("Agent '"+person+"' is registered as standing in the SeatAdministration for Vehicle '"+vehicle.getId()+"', while there is no room left.");
		}
		standingPersons.add(person);
	}

	
	/**
	 * This adds a person to the vehicle. The SeatAssignmentRule is used to determine
	 * whether he will get a seat or not.
	 * @param person
	 */
	public void addPerson(Id person)
	{	
		boardingAtFacility.add(person);		
		if (rule.getsSeatOnEnter(person, vehicle, sittingPersons.size(), standingPersons.size()))
		{
			addSitting(person);
		}
		else
		{
			addStanding(person);
		}
	}

	/**
	 * @return the number of people who are sitting divided by the number of seats
	 */
	public double getSeatCrowdedness() {
		if (vehicle.getType().getCapacity().getSeats() < 1)
		{
			return 0;
		}
		return ((double)sittingPersons.size()) / vehicle.getType().getCapacity().getSeats();
	}
	
	/**
	 * 
	 * @return the number of people who are standing divided by the room to stand.
	 */
	public double getStandingCrowdedness() {
		if (vehicle.getType().getCapacity().getStandingRoom() < 1)
		{
			return 0;
		}
		return ((double)standingPersons.size()) / vehicle.getType().getCapacity().getStandingRoom();
	}
	
	/**
	 * 
	 * @return the number of people in the vehicle divided by the total vehicle capacity.
	 */
	public double getTotalCrowdedness() {
		int pasgs = standingPersons.size() + sittingPersons.size();
		int cap = vehicle.getType().getCapacity().getSeats() + vehicle.getType().getCapacity().getStandingRoom();
		
		if (cap < 1)
		{
			return 0;
		}
		
		return (double) pasgs / (double) cap;
	}
	
	/**
	 * 
	 * @return the load factor of the bus (number of people divided by the number of seats).
	 */
		public double getLoadFactor() {
			int users = standingPersons.size() + sittingPersons.size();
			int capa = vehicle.getType().getCapacity().getSeats();
			
			if (capa < 1)
			{
				return 0;
			}
			return (double) users / (double) capa;
	}
		
		public double getRouteDepartureTime() {
			return routeDepartureTime;
		}


		public void setRouteDepartureTime(double routeDepartureTime) {
			this.routeDepartureTime = routeDepartureTime;
		}
		
	// Add the time, the occupancy, the load factor and the amount of people on-board when a vehicle arrive at facility
	public void addBusFacilityArrival(Id station, double time){
		facilityStates.put(station, new BusFacilityInteractionEvent(station, vehicle.getId()));
		facilityStates.get(station).setBusArrivalTime(time);
		facilityStates.get(station).setBusArrivalSitters(sittingPersons.size());
		facilityStates.get(station).setBusArrivalStandees(standingPersons.size());
		facilityStates.get(station).setTotalArrivalCrowdedness(getTotalCrowdedness());	
		facilityStates.get(station).setArrivalLoadFactor(getLoadFactor());
	}
	
	// Add the time, the occupancy, the load factor and the amount of people on-board when a vehicle depart from facility
	public void addBusFacilityDeparture(Id station, double time){
		facilityStates.get(station).setBusDepartureTime(time);
		facilityStates.get(station).setBusDepartureSitters(sittingPersons.size());
		facilityStates.get(station).setBusDepartureStandees(standingPersons.size());
		facilityStates.get(station).setTotalDepartureCrowdedness(getTotalCrowdedness());	
		facilityStates.get(station).setDepartureLoadFactor(getLoadFactor());				
		
	// Add the amount of people boarding/alighting at facility
		facilityStates.get(station).setPersonsBoarding(boardingAtFacility.size());
		facilityStates.get(station).setPersonsAlighting(alightingAtFacility.size());
		boardingAtFacility.clear();
		alightingAtFacility.clear();

	}
	
	public LinkedHashMap<Double, LinkedHashMap<Id, BusFacilityInteractionEvent>> getFacilityStatesForRouteStartTimes() {
		return facilityStatesForRouteStartTimes;
	}
	
	public void setFacilityStates(LinkedHashMap<Id, BusFacilityInteractionEvent> facilityStates) {
		this.facilityStates = facilityStates;
	}

}
