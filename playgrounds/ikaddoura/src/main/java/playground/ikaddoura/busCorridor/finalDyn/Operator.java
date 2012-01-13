/* *********************************************************************** *
 * project: org.matsim.*
 * Provider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.busCorridor.finalDyn;

import org.apache.log4j.Logger;

/**
 * @author Ihab
 *
 */
public class Operator {
	
	private final static Logger log = Logger.getLogger(Operator.class);
	
	private final double COSTS_PER_VEH_HOUR = 33; // in AUD
	private final double OVERHEAD_PERCENTAGE = 1.21; // on top of direct operating costs

	private int extItNr;
	private int capacity;
	private int numberOfBuses;
	private double profit;
	private double earnings;
	private int numberOfBusesFromEvents;
	private double vehicleKm;
	private double vehicleHours;
	private double costs;
	private double highestScore = 0;
	private int iterationWithHighestScore = 0;
	private double costsPerVehicleDay;
	private double costsPerVehicleKm;

	public Operator(int extItNr, int numberOfBuses, int capacity) {
		this.extItNr = extItNr;
		this.setNumberOfBuses(numberOfBuses);
		this.setCapacity(capacity);
		this.costsPerVehicleDay = getCostsPerVehicleDay();
		this.costsPerVehicleKm = getCostsPerVehicleKm();
	}
	
	private double getCostsPerVehicleKm() {
		double costsPerVehicleKm = 0.006 * this.getCapacity() + 0.513; // siehe lineare Regressionsanalyse in "BusCostsEstimations.xls"
		log.info("CostsPerVehicleKm (AUD): "+costsPerVehicleKm);
		return costsPerVehicleKm;
	}

	private double getCostsPerVehicleDay() {
		double costsPerVehicleDay = 1.6064 * this.getCapacity() + 22.622; // siehe lineare Regressionsanalyse in "BusCostsEstimations.xls"
		log.info("costsPerVehicleDay (AUD): "+costsPerVehicleDay);
		return costsPerVehicleDay;
	}

	public void calculateScore() {
		
		this.costs = (numberOfBusesFromEvents * costsPerVehicleDay) + ((vehicleKm * costsPerVehicleKm) + (vehicleHours * COSTS_PER_VEH_HOUR)) * OVERHEAD_PERCENTAGE;
		this.profit = this.getEarnings() - this.getCosts();

		log.info("OperatorScore calculated: "+this.getProfit());
	}

	/**
	 * @return the earnings
	 */
	public double getEarnings() {
		return earnings;
	}

	/**
	 * @return the costs
	 */
	public double getCosts() {
		return costs;
	}
	
	/**
	 * @return the highestScore
	 */
	public double getHighestScore() {
		return highestScore;
	}

	/**
	 * @param highestScore the highestScore to set
	 */
	public void setHighestScore(double highestScore) {
		this.highestScore = highestScore;
	}

	/**
	 * @return the iterationWithHighestScore
	 */
	public int getIterationWithHighestScore() {
		return iterationWithHighestScore;
	}

	/**
	 * @param iterationWithHighestScore the iterationWithHighestScore to set
	 */
	public void setIterationWithHighestScore(int iterationWithHighestScore) {
		this.iterationWithHighestScore = iterationWithHighestScore;
	}

	/**
	 * @return the score
	 */
	public double getProfit() {
		return profit;
	}
	
	public int increaseCapacity(int increase) {
		int newCapacity = this.capacity + increase;
		log.info("Capacity for next external iteration: "+newCapacity);
		return newCapacity;
	}
	
	public double increaseFare(double fare, double increase) {
		double newFare = fare + increase;
		log.info("Fare for next external iteration: "+newFare);
		return (newFare);
	}

	/**
	 * @param numberOfBuses the numberOfBuses to set
	 */
	public void setNumberOfBuses(int numberOfBuses) {
		this.numberOfBuses = numberOfBuses;
	}

	/**
	 * @return the numberOfBuses
	 */
	public int getNumberOfBuses() {
		return numberOfBuses;
	}

	/**
	 * @return the capacity
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * @param capacity the capacity to set
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	/**
	 * @return the vehicleKm
	 */
	public double getVehicleKm() {
		return vehicleKm;
	}

	/**
	 * @param vehicleKm the vehicleKm to set
	 */
	public void setVehicleKm(double vehicleKm) {
		this.vehicleKm = vehicleKm;
	}

	/**
	 * @return the vehicleHours
	 */
	public double getVehicleHours() {
		return vehicleHours;
	}

	/**
	 * @param vehicleHours the vehicleHours to set
	 */
	public void setVehicleHours(double vehicleHours) {
		this.vehicleHours = vehicleHours;
	}

	/**
	 * @param earnings the earnings to set
	 */
	public void setEarnings(double earnings) {
		this.earnings = earnings;
	}

	/**
	 * @return the numberOfBusesFromEvents
	 */
	public int getNumberOfBusesFromEvents() {
		return numberOfBusesFromEvents;
	}

	/**
	 * @param numberOfBusesFromEvents the numberOfBusesFromEvents to set
	 */
	public void setNumberOfBusesFromEvents(int numberOfBusesFromEvents) {
		this.numberOfBusesFromEvents = numberOfBusesFromEvents;
	}
	
//	public void analyzeScores() {
//		if (this.profit > this.highestScore){
//			this.setHighestScore(this.profit);
//			this.setIterationWithHighestScore(this.extItNr);
//		}
//		else {}
//		
//		log.info("OperatorScore analyzed.");	
//	}
	
//	public int strategy(Map<Integer, Integer> iteration2numberOfBuses, Map<Integer, Double> iteration2operatorScore) {
//		
//		int numberOfBuses = iteration2numberOfBuses.get(this.extItNr);
//		int newNumberOfBuses = 0;
//		
//		if (this.extItNr==0){
//			newNumberOfBuses = numberOfBuses + 1;  // Start-Strategie
//		}
//		
//		else {
//			
//			int numberOfBusesBefore = iteration2numberOfBuses.get(this.extItNr-1);
//			double score = this.getProfit(); // iteration2providerScore.get(this.extItNr);
//			double scoreBefore = iteration2operatorScore.get(this.extItNr-1);
//			
//			if(numberOfBusesBefore < numberOfBuses & scoreBefore < score){
//				// mehr Busse, score angestiegen
//				newNumberOfBuses = numberOfBuses+1;
//			}
//			else if(numberOfBusesBefore < numberOfBuses & scoreBefore > score){
//				// mehr Busse, score gesunken
//				newNumberOfBuses = numberOfBuses-1;
//			}
//			else if (numberOfBusesBefore > numberOfBuses & scoreBefore < score){
//				// weniger Busse, score angestiegen
//				newNumberOfBuses = numberOfBuses-1;
//			}
//			else if (numberOfBusesBefore > numberOfBuses & scoreBefore > score){
//				// weniger Busse, score gesunken
//				newNumberOfBuses = numberOfBuses+1;
//			}
//			else {
//				log.info("Operator score did not change.");
//				newNumberOfBuses = numberOfBuses;
//			}
//		}
//		
//		if (newNumberOfBuses == 0){
//			log.warn("At least one Bus expected!");
//			newNumberOfBuses = 1;
//		}
//		
//		log.info("OperatorStrategy changed numberOfBuses for next external Iteration to "+newNumberOfBuses+".");
//		return newNumberOfBuses;
//	}
	
}
