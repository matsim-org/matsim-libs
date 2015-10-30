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

package playground.artemc.crowding.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.internal.HasPersonId;

/**
 * The PersonCrowdednessEvent is generated if a person has made a journey
 * using a vehicle and people are either entering or leaving the vehicle.
 * The Event contains information on the crowdedness, the duration of the
 * journey with this crowdedness and whether this person has been sitting
 * or not.
 * @author pcbouman
 *
 * In addition to that, the event contain the ID of the agents and the 
 * number of sitters and sitters 
 * 
 * @author grerat
 */
public class PersonCrowdednessEvent extends Event implements HasPersonId {

	private Id personId;
	private Id vehicleId;
	private Id facilityId;
	private boolean isSitting;
	private boolean isLeaving;
	private int sitters;
	private int standees;
	private double seatCrowdedness;
	private double standCrowdedness;
	private double totalCrowdedness;
	private double loadFactor;
	private double travelTime;
	private double dwellTime;
	
	public PersonCrowdednessEvent(double time, Id person, Id vehicle, Id station, double tripDuration, double dwellTime, boolean isSitting, boolean isLeaving, int sitters, int standees, double seatCrwd, double standCrwd, double totalCrwd, double loadFac) {
		super(time);
		this.personId = person;
		this.vehicleId = vehicle;
		this.facilityId = station;
		this.isSitting = isSitting;
		this.isLeaving = isLeaving;
		this.sitters = sitters;
		this.standees = standees;
		this.seatCrowdedness = seatCrwd;
		this.standCrowdedness = standCrwd;
		this.totalCrowdedness = totalCrwd;
		this.loadFactor = loadFac;
		this.travelTime = tripDuration;
		this.dwellTime = dwellTime;
	}


	/**
	 * Indicates whether the person is sitting
	 * @return the isSitting
	 */
	public boolean isSitting() {
		return isSitting;
	}
	
	/**
	 * Indicates whether the person is Leaving (for SecondBestPricingFunction)
	 * @return the isLeaving
	 */
	public boolean isLeaving() {
		return isLeaving;
	}
	
	/**
	 * return the number of sitters
	 * @return the isSitting
	 */
	public int getSitters() {
		return sitters;
	}

	/**
	 * return the number of standees
	 * @return the isSitting
	 */
	public int getStandees() {
		return standees;
	}

	
	/**
	 * Returns the fraction of people sitting divided by the number of seats.
	 * Is thus between 0 (no one sitting) and 1 (all seats taken)
	 * In case the vehicle has no seats, the value returned is 0,
	 * as the case where no one is sitting cannot be considered "crowded".
	 * @return the seatCrowdedness
	 */
	public double getSeatCrowdedness() {
		return seatCrowdedness;
	}

	/**
	 * Return the fraction of people standing divided by the amount of room to stand.
	 * Is thus a value between 0 (no one standing) and 1 (all room for standing is taken).
	 * In case there is no room to stand, the returned value is 0,
	 * as the case where no one is standing cannot be considered "crowded".
	 * @return the standCrowdedness
	 */
	public double getStandCrowdedness() {
		return standCrowdedness;
	}

	/**
	 * Return the fraction of all people in the vehicle divided by all capacity in the vehicle.
	 * Is thus a value between 0 (vehicle is empty) and 1 (vehicle is full).
	 * In case the vehicle has no capacity, 0 is returned since a vehicle with no one
	 * in it cannot be considered a crowded vehicle.
	 * @return the totalCrowdedness
	 */
	public double getTotalCrowdedness() {
		return totalCrowdedness;
	}
	
	//NEW
	/**
	 * Return the fraction of all people in the vehicle divided by the number of seats of the vehicle.
	 * Is thus a value between 0 and more than 1.
	 * In case the vehicle has no capacity, 0 is returned since a vehicle with no one
	 * in it cannot be considered a crowded vehicle.
	 * @return the loadFactor
	 */
	public double getloadFactor() {
		return loadFactor;
	}
	
	/**
	 * Returns the duration for which a person has been in the vehicle during a trip with the observed
	 * crowdedness
	 * @return the duration of the trip
	 */
	public double getTravelTime() {
		return travelTime;
	}
	
	/**
	 * Returns the duration for which a person has been in the vehicle at facility with the observed
	 * crowdedness
	 * @return the dwell time
	 */
	public double getDwellTime() {
		return dwellTime;
	}

	@Override
	public String getEventType()	{
		return "PersonCrowdednessEvent";
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put("person", this.personId.toString());
		attributes.put("vehicle", this.vehicleId.toString());
		attributes. put("station", this.facilityId.toString());
		attributes.put("traveltime", Double.toString(this.travelTime));
		attributes.put("dwelltime", Double.toString(this.dwellTime));
		attributes.put("sitters", Integer.toString(this.sitters));
		attributes.put("standees", Integer.toString(this.standees));
		attributes.put("sitting", Boolean.toString(this.isSitting));
		attributes.put("leaving", Boolean.toString(this.isLeaving));
		attributes.put("seatcrowdedness", Double.toString(this.seatCrowdedness));
		attributes.put("standcrowdedness", Double.toString(this.standCrowdedness));
		attributes.put("totalcrowdedness", Double.toString(this.totalCrowdedness));
		attributes.put("loadfactor", Double.toString(this.loadFactor));
		return attributes;
	}

	@Override
	public Id getPersonId()	{
		return personId;
	}
	
	public Id getVehicleId() {
		return vehicleId;
	}
	
	public Id getFacilityId() {
		return facilityId;
	}
	
}
