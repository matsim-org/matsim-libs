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


package playground.pbouman.crowdedness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import playground.pbouman.crowdedness.rules.SeatAssignmentRule;

/**
 * The SeatAdministration class is responsible to keep track of who
 * are sitting and who are standing in a certain vehicle.
 * 
 * In principle, this process can be extremely complicated, as it
 * may depend on the layout of seats within the vehicle, the number
 * of doors, etc. We allow some flexibility by letting the user
 * specify a SeatAssingmentRule, that decides when someone will
 * get a seat upon entering the vehicle and whether someone who
 * is standing will sit down once someone who was sitting is
 * leaving the vehicle.
 * 
 * @author pbouman
 *
 */

public class SeatAdministration
{
	private static final Logger log = Logger.getLogger(SeatAdministration.class);
	
	private Vehicle vehicle;
	
	private List<Id> sittingPersons;
	private List<Id> standingPersons;

	private SeatAssignmentRule rule;
	
	/**
	 * Creates a SeatAdministration for a given vehicle using a certain
	 * decision rule to assign passengers to seats.
	 * @param vehicle
	 * @param rule
	 */
	
	public SeatAdministration(Vehicle vehicle, SeatAssignmentRule rule)
	{
		this.vehicle = vehicle; 
		this.rule = rule;
		this.sittingPersons = new ArrayList<Id>();
		this.standingPersons = new ArrayList<Id>();
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
			log.error("Agent '"+person+"' is registered as sitting in the SeatAdministration for Vehicle '"+vehicle.getId()+"', while there is no room left.");
		}
		sittingPersons.add(person);
	}
	
	private void addStanding(Id person)
	{
		if (getRemainingTotalCapacity() < 1)
		{
			log.error("Agent '"+person+"' is registered as standing in the SeatAdministration for Vehicle '"+vehicle.getId()+"', while there is no room left.");
			
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
	
}
