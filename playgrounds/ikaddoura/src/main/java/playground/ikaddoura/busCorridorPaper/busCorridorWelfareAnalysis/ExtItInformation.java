/* *********************************************************************** *
 * project: org.matsim.*
 * ExtItInformation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author benjamin
 *
 */
public class ExtItInformation {
	
	private double fare;
	private double capacity;
	private double numberOfBuses;
	private double headway;
	
	private double operatorCosts;
	private double operatorRevenue;
	
	private double usersLogSum;
	private double numberOfCarLegs;
	private double numberOfPtLegs;
	private double numberOfWalkLegs;
	private double sumOfWaitingTimes;
	private int numberOfWaitingTimesMoreThanHeadway;
	private int numberOfMissedVehicles;
	private Map <Id, FacilityInfo> facilityId2facilityInfos = new HashMap<Id, FacilityInfo>();
	
	protected double getFare() {
		return fare;
	}

	protected void setFare(double fare) {
		this.fare = fare;
	}

	protected double getCapacity() {
		return capacity;
	}

	protected void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	protected double getNumberOfBuses() {
		return numberOfBuses;
	}

	protected void setNumberOfBuses(double numberOfBuses) {
		this.numberOfBuses = numberOfBuses;
	}

	protected double getOperatorProfit() {
		return this.operatorRevenue - this.operatorCosts;
	}

	protected double getOperatorCosts() {
		return operatorCosts;
	}

	protected void setOperatorCosts(double operatorCosts) {
		this.operatorCosts = operatorCosts;
	}

	protected double getOperatorRevenue() {
		return operatorRevenue;
	}

	protected void setOperatorRevenue(double operatorRevenue) {
		this.operatorRevenue = operatorRevenue;
	}

	protected double getUsersLogSum() {
		return usersLogSum;
	}

	protected void setUsersLogSum(double usersLogSum) {
		this.usersLogSum = usersLogSum;
	}

	protected double getNumberOfCarLegs() {
		return numberOfCarLegs;
	}

	protected void setNumberOfCarLegs(double numberOfCarLegs) {
		this.numberOfCarLegs = numberOfCarLegs;
	}

	protected double getNumberOfPtLegs() {
		return numberOfPtLegs;
	}

	protected void setNumberOfPtLegs(double numberOfPtLegs) {
		this.numberOfPtLegs = numberOfPtLegs;
	}

	protected double getNumberOfWalkLegs() {
		return numberOfWalkLegs;
	}

	protected void setNumberOfWalkLegs(double numberOfWalkLegs) {
		this.numberOfWalkLegs = numberOfWalkLegs;
	}

	protected double getSumOfWaitingTimes() {
		return sumOfWaitingTimes;
	}

	protected void setSumOfWaitingTimes(double sumOfWaitingTimes) {
		this.sumOfWaitingTimes = sumOfWaitingTimes;
	}

	protected double getWelfare() {
		return this.getOperatorProfit() + this.usersLogSum;
	}

	public void setHeadway(double headway) {
		this.headway = headway;
	}

	public double getHeadway() {
		return headway;
	}

	public void setNumberOfWaitingTimesMoreThanHeadway(
			int numberOfWaitingTimesMoreThanHeadway) {
		this.numberOfWaitingTimesMoreThanHeadway = numberOfWaitingTimesMoreThanHeadway;
	}

	public int getNumberOfWaitingTimesMoreThanHeadway() {
		return numberOfWaitingTimesMoreThanHeadway;
	}

	public void setNumberOfMissedVehicles(int numberOfMissedVehicles) {
		this.numberOfMissedVehicles = numberOfMissedVehicles;
	}

	public int getNumberOfMissedVehicles() {
		return numberOfMissedVehicles;
	}

	public void setFacilityId2facilityInfos(Map <Id, FacilityInfo> facilityId2facilityInfos) {
		this.facilityId2facilityInfos = facilityId2facilityInfos;
	}
	
	public Map <Id, FacilityInfo> getFacilityId2facilityInfos() {
		return facilityId2facilityInfos;
	}
	
}
