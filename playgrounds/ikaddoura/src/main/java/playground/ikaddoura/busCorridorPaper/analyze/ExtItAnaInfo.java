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
package playground.ikaddoura.busCorridorPaper.analyze;


/**
 * @author ikaddoura
 *
 */
public class ExtItAnaInfo {
	
	private int iteration;
	
	private int numberOfBuses;
	private double fare;
	private double capacity;
	private double operatorProfit;
	private double usersLogSum;
	private double welfare;
	
	private double numberOfCarLegs;
	private double numberOfPtLegs;
	private double numberOfWalkLegs;
	
	private double noValidPlanScore;
	private double avgT0MinusTActPerPerson;
	private double t0MinusTActSum;
	
	private double avgWaitingTimeAll;
	private double avgWaitingTimeNotMissing;
	private double avgWaitingTimeMissing;
	private double numberOfMissedBusTrips;
	private double numberOfNotMissedBusTrips;
	
	public int getIteration() {
		return iteration;
	}
	public void setIteration(int iteration) {
		this.iteration = iteration;
	}
	public int getNumberOfBuses() {
		return numberOfBuses;
	}
	public void setNumberOfBuses(int numberOfBuses) {
		this.numberOfBuses = numberOfBuses;
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
	public double getOperatorProfit() {
		return operatorProfit;
	}
	public void setOperatorProfit(double operatorProfit) {
		this.operatorProfit = operatorProfit;
	}
	public double getUsersLogSum() {
		return usersLogSum;
	}
	public void setUsersLogSum(double usersLogSum) {
		this.usersLogSum = usersLogSum;
	}
	public double getWelfare() {
		return welfare;
	}
	public void setWelfare(double welfare) {
		this.welfare = welfare;
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
	public double getNoValidPlanScore() {
		return noValidPlanScore;
	}
	public void setNoValidPlanScore(double noValidPlanScore) {
		this.noValidPlanScore = noValidPlanScore;
	}
	public double getAvgT0MinusTActPerPerson() {
		return avgT0MinusTActPerPerson;
	}
	public void setAvgT0MinusTActPerPerson(double avgT0MinusTActPerPerson) {
		this.avgT0MinusTActPerPerson = avgT0MinusTActPerPerson;
	}
	public double getT0MinusTActSum() {
		return t0MinusTActSum;
	}
	public void setT0MinusTActSum(double t0MinusTActSum) {
		this.t0MinusTActSum = t0MinusTActSum;
	}
	public double getAvgWaitingTimeAll() {
		return avgWaitingTimeAll;
	}
	public void setAvgWaitingTimeAll(double avgWaitingTimeAll) {
		this.avgWaitingTimeAll = avgWaitingTimeAll;
	}
	public double getAvgWaitingTimeNotMissing() {
		return avgWaitingTimeNotMissing;
	}
	public void setAvgWaitingTimeNotMissing(double avgWaitingTimeNotMissing) {
		this.avgWaitingTimeNotMissing = avgWaitingTimeNotMissing;
	}
	public double getAvgWaitingTimeMissing() {
		return avgWaitingTimeMissing;
	}
	public void setAvgWaitingTimeMissing(double avgWaitingTimeMissing) {
		this.avgWaitingTimeMissing = avgWaitingTimeMissing;
	}
	public double getNumberOfMissedBusTrips() {
		return numberOfMissedBusTrips;
	}
	public void setNumberOfMissedBusTrips(double numberOfMissedBusTrips) {
		this.numberOfMissedBusTrips = numberOfMissedBusTrips;
	}
	public double getNumberOfNotMissedBusTrips() {
		return numberOfNotMissedBusTrips;
	}
	public void setNumberOfNotMissedBusTrips(double numberOfNotMissedBusTrips) {
		this.numberOfNotMissedBusTrips = numberOfNotMissedBusTrips;
	}
	
}
