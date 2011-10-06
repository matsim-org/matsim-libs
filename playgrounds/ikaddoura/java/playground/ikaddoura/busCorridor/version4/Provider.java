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
package playground.ikaddoura.busCorridor.version4;

import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author Ihab
 *
 */
public class Provider {
	
	private final static Logger log = Logger.getLogger(MyLegScoringFunction.class);

	private int extItNr;
	private double score;
	private double highestScore = 0;
	private int iterationWithHighestScore = 0;

	/**
	 * @param extItNr
	 */
	public Provider(int extItNr) {
	this.extItNr = extItNr;
	}

	public void calculateScore(String directoryExtIt, int lastInternalIteration) {
		
		String lastEventFile = directoryExtIt+"/internalIterations/ITERS/it."+lastInternalIteration+"/"+lastInternalIteration+".events.xml.gz";

//		Config config = ConfigUtils.loadConfig(configFile);
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		EventsManager events = new BusCorridorEventsManagerImpl();
//		
//		BusCorridorLinkLeaveEventHandler handler1 = new BusCorridorLinkLeaveEventHandler(scenario);
//		BusCorridorActivityEndEventHandler handler2 = new BusCorridorActivityEndEventHandler(scenario);
//		BusCorridorPersonEntersVehicleEventHandler handler3 = new BusCorridorPersonEntersVehicleEventHandler(scenario);
//		BusCorridorPersonLeavesVehicleEventHandler handler4 = new BusCorridorPersonLeavesVehicleEventHandler(scenario);
//
//		events.addHandler(handler1);	
//		events.addHandler(handler2);
//		events.addHandler(handler3);
//		events.addHandler(handler4);
//		
//
//		MatsimEventsReader reader = new MatsimEventsReader(events);
//		reader.readFile(eventFile);
		
		// berechne aus der EventsFile einen Provider-Score
//		double busCostsPerDay = 500;
//		double busCostsPerKm = 1;
//		double fixCosts = this.getNumberOfBuses() * busCostsPerDay;
//		double varCosts = busKm * busCostsPerKm;
//		double providerScore = - ( fixCosts + varCosts ) + earnings
		
		this.setScore(333);
		log.info("ProviderScore calculated.");
	}

	public void analyzeScores() {
		if (this.score > this.highestScore){
			this.setHighestScore(this.score);
			this.setIterationWithHighestScore(this.extItNr);
		}
		else {}
		
		log.info("ProviderScore analyzed.");	
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
	 * @param score the score to set
	 */
	public void setScore(double score) {
		this.score = score;
	}

	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	public int strategy(Map<Integer, Integer> iteration2numberOfBuses, Map<Integer, Double> iteration2providerScore) {
		
		int numberOfBuses = iteration2numberOfBuses.get(this.extItNr);
		int newNumberOfBuses = 0;
		
		if (this.extItNr==0){
			newNumberOfBuses = numberOfBuses + 1;  // Start-Strategie
		}
		
		else {
			
			int numberOfBusesBefore = iteration2numberOfBuses.get(this.extItNr-1);
			double score = this.getScore(); // iteration2providerScore.get(this.extItNr);
			double scoreBefore = iteration2providerScore.get(this.extItNr-1);
			
			if(numberOfBusesBefore < numberOfBuses & scoreBefore < score){
				// mehr Busse, score angestiegen
				newNumberOfBuses = numberOfBuses+1;
			}
			if(numberOfBusesBefore < numberOfBuses & scoreBefore > score){
				// mehr Busse, score gesunken
				newNumberOfBuses = numberOfBuses-1;
			}
			if (numberOfBusesBefore > numberOfBuses & scoreBefore < score){
				// weniger Busse, score angestiegen
				newNumberOfBuses = numberOfBuses-1;
			}
			if (numberOfBusesBefore > numberOfBuses & scoreBefore > score){
				// weniger Busse, score gesunken
				newNumberOfBuses = numberOfBuses+1;
			}
			else {
				System.out.println("******************** Score unverändert ******************");
				newNumberOfBuses = numberOfBuses;
			}
		}
		
		log.info("ProviderStrategy --> newNumberOfBuses");
		return newNumberOfBuses;
	}
	
	

}
