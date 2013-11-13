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
package playground.wrashid.parkingSearch.withinDay_v_STRC.scoring;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class ParkingActivityAttributes {
	public double getToActWalkDuration() {
		return toActWalkDuration;
	}
	public void setToActWalkDuration(double toActWalkDuration) {
		this.toActWalkDuration = toActWalkDuration;
	}
	public double getToParkWalkDuration() {
		return toParkWalkDuration;
	}
	public void setToParkWalkDuration(double toParkWalkDuration) {
		this.toParkWalkDuration = toParkWalkDuration;
	}
	public double getParkingSearchDuration() {
		return parkingSearchDuration;
	}
	public void setParkingSearchDuration(double parkingSearchDuration) {
		this.parkingSearchDuration = parkingSearchDuration;
	}
	public void setPersonId(Id personId) {
		this.personId = personId;
	}
	public void setFacilityId(Id facilityId) {
		this.facilityId = facilityId;
	}
	public void setParkingArrivalTime(double parkingArrivalTime) {
		this.parkingArrivalTime = parkingArrivalTime;
	}
	public void setParkingDuration(double parkingDuration) {
		this.parkingDuration = parkingDuration;
	}
	public void setActivityDuration(double activityDuration) {
		this.activityDuration = activityDuration;
	}

	private double toActWalkDuration;
	private double toParkWalkDuration;
	public Id getPersonId() {
		return personId;
	}
	public Id getFacilityId() {
		return facilityId;
	}
	public double getParkingArrivalTime() {
		return parkingArrivalTime;
	}
	public double getParkingDuration() {
		return parkingDuration;
	}
	public double getActivityDuration() {
		return activityDuration;
	}
	
	public double getParkingSearchDurationInSeconds() {
		return parkingSearchDuration;
	}
	public ParkingActivityAttributes(Id personId, Id facilityId, double parkingArrivalTime, double parkingDuration,
			double activityDuration, double parkingSearchDuration, double toActWalkDuration, double toParkWalkDuration) {
		super();
		this.personId = personId;
		this.facilityId = facilityId;
		this.parkingArrivalTime = parkingArrivalTime;
		this.parkingDuration = parkingDuration;
		this.activityDuration = activityDuration;
		this.parkingSearchDuration = parkingSearchDuration;
		this.toActWalkDuration = toActWalkDuration;
		this.toParkWalkDuration = toParkWalkDuration;
	}
	
	public ParkingActivityAttributes(Id personId){
		this.personId = personId;
	}
	
	public double getToActWalkDurationInSeconds() {
		return toActWalkDuration;
	}
	
	public double getToParkWalkDurationInSeconds() {
		return toParkWalkDuration;
	}
	
	public double getTotalWalkDurationInSeconds(){
		return toActWalkDuration + toParkWalkDuration;
	}
	
	public double getParkingCost() {
		return parkingCost;
	}
	public void setParkingCost(double parkingCost) {
		this.parkingCost = parkingCost;
	}

	private Id personId;
	private Id facilityId;
	private double parkingArrivalTime;
	private double parkingDuration;
	private double activityDuration;
	private double parkingSearchDuration;
	private double parkingCost;
	public Coord destinationCoord;
	private double walkDistance;
	public double getWalkDistance() {
		return walkDistance;
	}
	public void setWalkDistance(double walkDistance) {
		this.walkDistance = walkDistance;
	}
}

