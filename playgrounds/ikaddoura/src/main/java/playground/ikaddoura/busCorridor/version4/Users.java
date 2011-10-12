/* *********************************************************************** *
 * project: org.matsim.*
 * Users.java
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author Ihab
 *
 */
public class Users {
	
	private final static Logger log = Logger.getLogger(Users.class);

	private double avgExecScore;
	private int numberOfPtLegs;
	private int numberOfCarLegs;
	private int numberOfWalkLegs;
	
	public void analyzeScores(String directoryExtIt, String networkFile) {
		
		List<Double> scores = new ArrayList<Double>();
		double scoreSum = 0.0;
		
		String outputPlanFile = directoryExtIt+"/internalIterations/output_plans.xml.gz";		
		
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(outputPlanFile);
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();

		for(Person person : population.getPersons().values()){
			double score = person.getSelectedPlan().getScore();
			scores.add(score);
		}
		
		for (Double score : scores){
			scoreSum = scoreSum+score;
		}
		
		this.setAvgExecScore(scoreSum/scores.size());
		
		log.info("Users Scores analyzed.");
	}
	 
	public void setAvgExecScore(double avgExecScore) {
		this.avgExecScore = avgExecScore;
	}

	public double getAvgExecScore() {
		return avgExecScore;
	}

	public int getNumberOfPtLegs() {
		return numberOfPtLegs;
	}

	public int getNumberOfCarLegs() {
		return numberOfCarLegs;
	}
	
	public int getNumberOfWalkLegs() {
		return numberOfWalkLegs;
	}

	public void analyzeLegModes(String directoryExtIt, int lastInternalIteration) {
		String lastEventFile = directoryExtIt+"/internalIterations/ITERS/it."+lastInternalIteration+"/"+lastInternalIteration+".events.xml.gz";
		
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		DepartureEventHandler departureHandler = new DepartureEventHandler();
		
		events.addHandler(departureHandler);	
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(lastEventFile);
		
		this.numberOfPtLegs = departureHandler.getNumberOfPtLegs();
		this.numberOfCarLegs = departureHandler.getNumberOfCarLegs();
		this.numberOfWalkLegs = departureHandler.getNumberOfWalkLegs();
		
		log.info("Leg Modes analyzed.");

	}
}
