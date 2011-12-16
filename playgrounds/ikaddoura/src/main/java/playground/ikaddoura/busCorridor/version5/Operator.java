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
package playground.ikaddoura.busCorridor.version5;

import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;

/**
 * @author Ihab
 *
 */
public class Operator {
	
	private final static Logger log = Logger.getLogger(Operator.class);
	private final double COSTS_PER_VEH_DAY = 126; // anzupassen in Abh. von der Kapazität!!!
	private final double COSTS_PER_VEH_KM = 0.9;
	private final double COSTS_PER_VEH_HOUR = 33;
	private final double OVERHEAD_PERCENTAGE = 1.21; // on top of direct operating costs

//	private final double COSTS_PER_ROUTE_KM = 1000;

	private int extItNr;
	private int numberOfBuses;
	private double score;
	private double highestScore = 0;
	private int iterationWithHighestScore = 0;

	public Operator(int extItNr, int numberOfBuses) {
		this.extItNr = extItNr;
		this.setNumberOfBuses(numberOfBuses);
	}

	public void calculateScore(String directoryExtIt, int lastInternalIteration, String networkFile) {
		
		Scenario scen = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		Config config = scen.getConfig();
		config.network().setInputFile(networkFile);
		ScenarioUtils.loadScenario(scen);		
		Network network = scen.getNetwork();
		
		String lastEventFile = directoryExtIt+"/internalIterations/ITERS/it."+lastInternalIteration+"/"+lastInternalIteration+".events.xml.gz";
		
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		MoneyEventHandler moneyHandler = new MoneyEventHandler();
		TransitEventHandler transitHandler = new TransitEventHandler();
		LinksEventHandler linksHandler = new LinksEventHandler(network);
		DepartureArrivalEventHandler departureArrivalEventHandler = new DepartureArrivalEventHandler();
		
		events.addHandler(moneyHandler);	
		events.addHandler(transitHandler);
		events.addHandler(linksHandler);
		events.addHandler(departureArrivalEventHandler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(lastEventFile);
		
		double earnings = moneyHandler.getEarnings();
		int numberOfBuses = transitHandler.getVehicleIDs().size(); // Anzahl der Busse aus den Events!		
		double vehicleKm = linksHandler.getVehicleKm(); // vehicle-km aus den Events!
		double vehicleHours = departureArrivalEventHandler.getVehicleHours(); // vehicle-hours aus den Events, nicht aus dem Fahrplan!
		
//		double routeLength = getRouteLength(network);
		
		double operatingCosts = ((numberOfBuses * COSTS_PER_VEH_DAY) + (vehicleKm * COSTS_PER_VEH_KM) + (vehicleHours * COSTS_PER_VEH_HOUR)) * OVERHEAD_PERCENTAGE;
		double operatorScore = earnings - operatingCosts;

		this.setScore(operatorScore);
		log.info("OperatorScore calculated: "+operatorScore);
	}

	public double getRouteLength(Network network) {
		double routeLength = 0;
		for (Link link : network.getLinks().values()){
			routeLength = routeLength + link.getLength()/1000;
		}
		return routeLength;
	}

	public void analyzeScores() {
		if (this.score > this.highestScore){
			this.setHighestScore(this.score);
			this.setIterationWithHighestScore(this.extItNr);
		}
		else {}
		
		log.info("OperatorScore analyzed.");	
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

	public int strategy(Map<Integer, Integer> iteration2numberOfBuses, Map<Integer, Double> iteration2operatorScore) {
		
		int numberOfBuses = iteration2numberOfBuses.get(this.extItNr);
		int newNumberOfBuses = 0;
		
		if (this.extItNr==0){
			newNumberOfBuses = numberOfBuses + 1;  // Start-Strategie
		}
		
		else {
			
			int numberOfBusesBefore = iteration2numberOfBuses.get(this.extItNr-1);
			double score = this.getScore(); // iteration2providerScore.get(this.extItNr);
			double scoreBefore = iteration2operatorScore.get(this.extItNr-1);
			
			if(numberOfBusesBefore < numberOfBuses & scoreBefore < score){
				// mehr Busse, score angestiegen
				newNumberOfBuses = numberOfBuses+1;
			}
			else if(numberOfBusesBefore < numberOfBuses & scoreBefore > score){
				// mehr Busse, score gesunken
				newNumberOfBuses = numberOfBuses-1;
			}
			else if (numberOfBusesBefore > numberOfBuses & scoreBefore < score){
				// weniger Busse, score angestiegen
				newNumberOfBuses = numberOfBuses-1;
			}
			else if (numberOfBusesBefore > numberOfBuses & scoreBefore > score){
				// weniger Busse, score gesunken
				newNumberOfBuses = numberOfBuses+1;
			}
			else {
				log.info("Operator score did not change.");
				newNumberOfBuses = numberOfBuses;
			}
		}
		
		if (newNumberOfBuses == 0){
			log.warn("At least one Bus expected!");
			newNumberOfBuses = 1;
		}
		
		log.info("OperatorStrategy changed numberOfBuses for next external Iteration to "+newNumberOfBuses+".");
		return newNumberOfBuses;
	}
	
	public int increaseNumberOfBuses() {
		int newNumberOfBuses = numberOfBuses + 1;
		log.info("NumberOfBuses increased for next external Iteration to "+newNumberOfBuses+".");
		return newNumberOfBuses;
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
	
	

}
