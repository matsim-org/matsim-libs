/* *********************************************************************** *
 * project: org.matsim.*
 * ExtendedLocationChoiceProbabilityCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.locationchoice;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.collections.Tuple;

/*
 * Calculates the probabilities dynamically, depending on the travel times
 * in the scenario and the departure time. 
 */
public class ExtendedLocationChoiceProbabilityCreator {

	private static final Logger log = Logger.getLogger(ExtendedLocationChoiceProbabilityCreator.class);
	
	private Scenario scenario;
	private PersonalizableTravelTime travelTime;
	private CalculateDestinationChoice calculateDestinationChoice;
		
	public static void main(String[] args) {
//		Map<Integer, Double> probabs = new ExtendedLocationChoiceProbabilityCreator(new ScenarioImpl()).getFromZoneProbabilities(1525, 0.0);
//		
//		double sum = 0.0;
//		for (double value : probabs.values()) {
//			sum = sum + value;
//		}
//		
//		log.info("Sum Probabilities = " + sum);
	}
	
	public ExtendedLocationChoiceProbabilityCreator(Scenario scenario, PersonalizableTravelTime travelTime) {		
		this.scenario = scenario;
		this.travelTime = travelTime;
		
		log.info("Creating constant probabilities...");
		calculateConstantProbabilities();
		log.info("done.");
	}
	
	private void calculateConstantProbabilities() {
		calculateDestinationChoice = new CalculateDestinationChoice(scenario);
		calculateDestinationChoice.calculateConstantFactors();
	}
	
	public void calculateDynamicProbabilities() {
		calculateDestinationChoice.calculateDynamicFactors(travelTime);
	}
	
	public void calculateTotalProbabilities() {
		calculateDestinationChoice.calculateTotalFactors();
	}
	
	/**
	 * @return Tuple<toZone TAZ, Probability of that Zone>
	 */
	public Tuple<int[], double[]> getFromZoneProbabilities(int type, int fromZoneTAZ, double depatureTime) {
		return calculateDestinationChoice.getFromZoneProbabilities(type, fromZoneTAZ, depatureTime);
	}
}
