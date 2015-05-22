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
package playground.ikaddoura.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.ikaddoura.optimization.users.FareData;

/**
 * @author ihab and benjamin
 *
 */
public class IterationInfo {
	
	private double fare;
	private double capacity;
	private double numberOfBuses;
	private double headway;
	private double totalDemand;
	
	private double operatorCosts;
	private double operatorRevenue;
	
	private List<FareData> fareDataList = new ArrayList<FareData>();
	
	private double usersLogSum;
	private double numberOfCarLegs;
	private double numberOfPtLegs;
	private double numberOfWalkLegs;
	
	private int noValidPlanScore;
	
	private double avgT0MinusTActPerPerson;
	private double t0MinusTActSum;
	private double avgT0MinusTActDivT0PerTrip;
	
	private List <Double> waitingTimes = new ArrayList<Double>();
	private List <Double> waitingTimesMissed = new ArrayList<Double>();
	private List <Double> waitingTimesNotMissed = new ArrayList<Double>();
			
	private int numberOfMissedVehicles;
	private int numberOfBoardingDeniedEvents;
	
	private double maxArrivalDelay;
	private double maxDepartureDelay;
	
	private double averageFarePerAgent;
	
	private Map <Id<Person>, List<Double>> personId2waitingTimes = new HashMap<Id<Person>, List<Double>>();
	
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

	public double getFare() {
		return fare;
	}

	public void setFare(double fare) {
		this.fare = fare;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	public double getNumberOfBuses() {
		return numberOfBuses;
	}

	public void setNumberOfBuses(double numberOfBuses) {
		this.numberOfBuses = numberOfBuses;
	}

	public double getOperatorProfit() {
		return this.operatorRevenue - this.operatorCosts;
	}

	public double getOperatorCosts() {
		return operatorCosts;
	}

	public void setOperatorCosts(double operatorCosts) {
		this.operatorCosts = operatorCosts;
	}

	public double getOperatorRevenue() {
		return operatorRevenue;
	}

	public void setOperatorRevenue(double operatorRevenue) {
		this.operatorRevenue = operatorRevenue;
	}

	public double getUsersLogSum() {
		return usersLogSum;
	}

	public void setUsersLogSum(double usersLogSum) {
		this.usersLogSum = usersLogSum;
	}

	public double getNumberOfCarLegs() {
		return numberOfCarLegs;
	}

	public void setNumberOfCarLegs(double numberOfCarLegs) {
		this.numberOfCarLegs = numberOfCarLegs;
	}

	public double getNumberOfPtLegs() {
		return numberOfPtLegs;
	}

	public void setNumberOfPtLegs(double numberOfPtLegs) {
		this.numberOfPtLegs = numberOfPtLegs;
	}

	public double getNumberOfWalkLegs() {
		return numberOfWalkLegs;
	}

	public void setNumberOfWalkLegs(double numberOfWalkLegs) {
		this.numberOfWalkLegs = numberOfWalkLegs;
	}

	public double getWelfare() {
		return this.getOperatorProfit() + this.usersLogSum;
	}

	public void setHeadway(double headway) {
		this.headway = headway;
	}

	public double getHeadway() {
		return headway;
	}

	public int getMissedBusTrips() {
		return this.waitingTimesMissed.size();
	}
	
	public int getNotMissedBusTrips() {
		return this.waitingTimesNotMissed.size();
	}

	public void setNumberOfMissedVehicles(int numberOfMissedVehicles) {
		this.numberOfMissedVehicles = numberOfMissedVehicles;
	}

	public int getNumberOfMissedVehicles() {
		return numberOfMissedVehicles;
	}
	
	public double getAvgWaitingTimeAll() {
		int counter = 0;
		double sumOfWaitingTimes = 0.0;
		for(Double waitingTime : this.waitingTimes){
			sumOfWaitingTimes = sumOfWaitingTimes + waitingTime;
			counter++;
		}
		if (sumOfWaitingTimes == 0.0){
			return 0.0;
		} else {
			return sumOfWaitingTimes / counter;
		}
	}
	
	public double getAvgWaitingTimeNotMissingBus() {
		int counter = 0;
		double sumOfWaitingTimesNotMissingBus = 0.0;
		for(Double waitingTime : this.waitingTimesNotMissed){
			sumOfWaitingTimesNotMissingBus = sumOfWaitingTimesNotMissingBus + waitingTime;
			counter++;
		}
		if (sumOfWaitingTimesNotMissingBus == 0.0){
			return 0.0;
		} else {
			return sumOfWaitingTimesNotMissingBus / counter;
		}
	}
	
	public double getAvgWaitingTimeMissingBus() {
		int counter = 0;
		double sumOfWaitingTimesMissingBus = 0.0;
		for(Double waitingTime : this.waitingTimesMissed){
			sumOfWaitingTimesMissingBus = sumOfWaitingTimesMissingBus + waitingTime;
			counter++;
		}
		if (sumOfWaitingTimesMissingBus == 0.0){
			return 0.0;
		} else {
			return sumOfWaitingTimesMissingBus / counter;
		}
	}

	public void setNoValidPlanScore(int noValidPlanScore) {
		this.noValidPlanScore = noValidPlanScore;
	}

	public int getNoValidPlanScore() {
		return noValidPlanScore;
	}

	public void setPersonId2waitingTimes(Map <Id<Person>, List<Double>> personId2waitingTimes) {
		this.personId2waitingTimes = personId2waitingTimes;
	}

	public Map <Id<Person>, List<Double>> getPersonId2waitingTimes() {
		return personId2waitingTimes;
	}

	public void setAvgT0MinusTActPerPerson(double avgT0MinusTAct) {
		this.avgT0MinusTActPerPerson = avgT0MinusTAct;
	}

	public double getAvgT0MinusTActPerPerson() {
		if (Math.abs(this.avgT0MinusTActPerPerson) < 0.0001){
			this.avgT0MinusTActPerPerson = 0.;
		}
		return avgT0MinusTActPerPerson;
	}

	public void setT0MinusTActSum(double t0MinusTActSum) {
		this.t0MinusTActSum = t0MinusTActSum;
	}

	public double getT0MinusTActSum() {
		return t0MinusTActSum;
	}

	public double getAvgT0MinusTActDivT0PerTrip() {
		if (Math.abs(this.avgT0MinusTActDivT0PerTrip) < 0.0001){
			this.avgT0MinusTActDivT0PerTrip = 0.;
		}
		return avgT0MinusTActDivT0PerTrip;
	}

	public void setAvgT0MinusTActDivT0PerTrip(double avgT0MinusTActDivT0PerTrip) {
		this.avgT0MinusTActDivT0PerTrip = avgT0MinusTActDivT0PerTrip;
	}

	public void setTotalDemand(double demand) {
		this.totalDemand = demand;
	}

	public double getTotalDemand() {
		return totalDemand;
	}

	public List<FareData> getFareDataList() {
		return fareDataList;
	}

	public void setFareDataList(List<FareData> fareDataList) {
		this.fareDataList = fareDataList;
	}

	public double getAverageFarePerAgent() {
		return averageFarePerAgent;
	}

	public void setAverageFarePerAgent(double averageFarePerAgent) {
		this.averageFarePerAgent = averageFarePerAgent;
	}

	public double getMaxArrivalDelay() {
		return maxArrivalDelay;
	}

	public void setMaxArrivalDelay(double maxArrivalDelay) {
		this.maxArrivalDelay = maxArrivalDelay;
	}

	public double getMaxDepartureDelay() {
		return maxDepartureDelay;
	}

	public void setMaxDepartureDelay(double maxDepartureDelay) {
		this.maxDepartureDelay = maxDepartureDelay;
	}

	public int getNumberOfBoardingDeniedEvents() {
		return numberOfBoardingDeniedEvents;
	}

	public void setNumberOfBoardingDeniedEvents(int numberOfBoardingDeniedEvents) {
		this.numberOfBoardingDeniedEvents = numberOfBoardingDeniedEvents;
	}
	
}
