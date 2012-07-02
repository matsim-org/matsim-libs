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
import java.util.SortedMap;
import java.util.TreeMap;

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
	
	private int noValidPlanScore;
	
	private List <Double> waitingTimes = new ArrayList<Double>();
	private List <Double> waitingTimesMissed = new ArrayList<Double>();
	private List <Double> waitingTimesNotMissed = new ArrayList<Double>();
		
	private int numberOfMissedVehicles;
	private Map <Id, FacilityWaitTimeInfo> id2facilityWaitInfo = new HashMap<Id, FacilityWaitTimeInfo>();
	private SortedMap <Id, RouteInfo> routeId2RouteInfo = new TreeMap<Id, RouteInfo>();
	
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

	protected void setId2facilityWaitInfo(Map <Id, FacilityWaitTimeInfo> facilityId2facilityInfos) {
		this.id2facilityWaitInfo = facilityId2facilityInfos;
	}
	
	protected Map <Id, FacilityWaitTimeInfo> getId2facilityWaitInfo() {
		return id2facilityWaitInfo;
	}
	
	protected double getAvgWaitingTimeAll() {
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
	
	protected double getAvgWaitingTimeNotMissingBus() {
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
	
	protected double getAvgWaitingTimeMissingBus() {
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

	public void setRouteId2RouteInfo(SortedMap <Id, RouteInfo> routeId2RouteInfo) {
		this.routeId2RouteInfo = routeId2RouteInfo;
	}

	public SortedMap<Id, RouteInfo> getRouteId2RouteInfo() {
		return routeId2RouteInfo;
	}
	
	public SortedMap<Id, SortedMap<Id, Integer>> getRouteId2stopId2PersonsEnteringWithin(double start, double end) {
		SortedMap<Id, SortedMap<Id, Integer>> routeId2stopId2PersonEnteringWithin = new TreeMap<Id, SortedMap<Id, Integer>>();
		for (RouteInfo routeInfo : this.routeId2RouteInfo.values()){
			SortedMap<Id, Integer> stopId2PersonEnteringWithin = new TreeMap<Id, Integer>();
			for (FacilityLoadInfo loadInfo : routeInfo.getTransitStopId2FacilityLoadInfo().values()){
				stopId2PersonEnteringWithin.put(loadInfo.getFacilityId(), loadInfo.getPersonsEnteringWithin(start, end));
			}
			routeId2stopId2PersonEnteringWithin.put(routeInfo.getRouteId(), stopId2PersonEnteringWithin);
		}
		return routeId2stopId2PersonEnteringWithin;	
	}

	public SortedMap<Id, SortedMap<Id, Integer>> getRouteId2stopId2PersonsLeavingWithin(double start, double end) {
		SortedMap<Id, SortedMap<Id, Integer>> routeId2stopId2PersonLeavingWithin = new TreeMap<Id, SortedMap<Id, Integer>>();
		for (RouteInfo routeInfo : this.routeId2RouteInfo.values()){
			SortedMap<Id, Integer> stopId2PersonLeavingWithin = new TreeMap<Id, Integer>();
			for (FacilityLoadInfo loadInfo : routeInfo.getTransitStopId2FacilityLoadInfo().values()){
				stopId2PersonLeavingWithin.put(loadInfo.getFacilityId(), loadInfo.getPersonsLeavingWithin(start, end));
			}
			routeId2stopId2PersonLeavingWithin.put(routeInfo.getRouteId(), stopId2PersonLeavingWithin);
		}
		return routeId2stopId2PersonLeavingWithin;	
	}

//	public SortedMap<Id, SortedMap<Id, Integer>> getRouteId2stopId2PassengersWithin(double start, double end) {
//		SortedMap<Id, SortedMap<Id, Integer>> routeId2stopId2PassengersWithin = new TreeMap<Id, SortedMap<Id, Integer>>();
//		for (RouteInfo routeInfo : this.routeId2RouteInfo.values()){
//			SortedMap<Id, Integer> stopId2PassengersWithin = new TreeMap<Id, Integer>();
//			for (FacilityLoadInfo loadInfo : routeInfo.getTransitStopId2FacilityLoadInfo().values()){
//				stopId2PassengersWithin.put(loadInfo.getFacilityId(), loadInfo.getPassengersWithin(start, end));
//			}
//			routeId2stopId2PassengersWithin.put(routeInfo.getRouteId(), stopId2PassengersWithin);
//		}
//		return routeId2stopId2PassengersWithin;
//	}
	
}
