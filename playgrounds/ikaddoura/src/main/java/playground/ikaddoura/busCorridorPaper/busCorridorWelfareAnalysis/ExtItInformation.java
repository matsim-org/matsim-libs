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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	
	private List <Double> waitingTimes = new ArrayList<Double>();
	private List <Double> waitingTimesMissed = new ArrayList<Double>();
	private List <Double> waitingTimesNotMissed = new ArrayList<Double>();
		
	private int numberOfMissedVehicles;
	private Map <Id, FacilityInfo> facilityId2facilityInfos = new HashMap<Id, FacilityInfo>();
	
	public List <Double> getWaitingTimes() {
		return waitingTimes;
	}

	public void setWaitingTimes(List <Double> waitingTimes) {
		this.waitingTimes = waitingTimes;
	}

	public List <Double> getWaitingTimesMissed() {
		return waitingTimesMissed;
	}

	public void setWaitingTimesMissed(
			List <Double> waitingTimesMissed) {
		this.waitingTimesMissed = waitingTimesMissed;
	}

	public List <Double> getWaitingTimesNotMissed() {
		return waitingTimesNotMissed;
	}

	public void setWaitingTimesNotMissed(
			List <Double> waitingTimesNotMissed) {
		this.waitingTimesNotMissed = waitingTimesNotMissed;
	}

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

	protected double getWelfare() {
		return this.getOperatorProfit() + this.usersLogSum;
	}

	protected void setHeadway(double headway) {
		this.headway = headway;
	}

	protected double getHeadway() {
		return headway;
	}

	protected int getMissedBusTrips() {
		return this.waitingTimesMissed.size();
	}
	
	protected int getNotMissedBusTrips() {
		return this.waitingTimesNotMissed.size();
	}

	protected void setNumberOfMissedVehicles(int numberOfMissedVehicles) {
		this.numberOfMissedVehicles = numberOfMissedVehicles;
	}

	protected int getNumberOfMissedVehicles() {
		return numberOfMissedVehicles;
	}

	protected void setFacilityId2facilityInfos(Map <Id, FacilityInfo> facilityId2facilityInfos) {
		this.facilityId2facilityInfos = facilityId2facilityInfos;
	}
	
	protected Map <Id, FacilityInfo> getFacilityId2facilityInfos() {
		return facilityId2facilityInfos;
	}
	
	protected double getAvgWaitingTimeAll() {
		int counter = 0;
		double sumOfWaitingTimes = 0.0;
		for(Double waitingTime : this.waitingTimes){
			sumOfWaitingTimes = sumOfWaitingTimes + waitingTime;
			counter++;
		}
		return sumOfWaitingTimes / counter;
	}
	
	protected double getAvgWaitingTimeNotMissingBus() {
		int counter = 0;
		double sumOfWaitingTimesNotMissingBus = 0.0;
		for(Double waitingTime : this.waitingTimesNotMissed){
			sumOfWaitingTimesNotMissingBus = sumOfWaitingTimesNotMissingBus + waitingTime;
			counter++;
		}
		return sumOfWaitingTimesNotMissingBus / counter;
	}
	
	protected double getAvgWaitingTimeMissingBus() {
		int counter = 0;
		double sumOfWaitingTimesMissingBus = 0.0;
		for(Double waitingTime : this.waitingTimesMissed){
			sumOfWaitingTimesMissingBus = sumOfWaitingTimesMissingBus + waitingTime;
			counter++;
		}
		return sumOfWaitingTimesMissingBus / counter;
	}
	
}
