/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice;

import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;
import herbie.running.controler.listeners.ScoreElements;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.anhorni.surprice.scoring.SurpriceScoringFunctionFactory;

public class DayControler extends Controler {
	
	private AgentMemories memories = new AgentMemories();
	private String day;	
	private ObjectAttributes preferences;
	
	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
	
	public DayControler(final Config config, AgentMemories memories, String day, ObjectAttributes preferences) {
		super(config);	
		super.setOverwriteFiles(true);
		this.memories = memories;	
		this.day = day;
		this.preferences = preferences;
	} 
		
	protected void setUp() {
	    super.setUp();	
	    	    
	  	SurpriceScoringFunctionFactory scoringFunctionFactory = new SurpriceScoringFunctionFactory(
	  			this, this.config.planCalcScore(), this.network, this.memories, this.day, this.preferences);	  		
	  	this.setScoringFunctionFactory(scoringFunctionFactory);
	  	
	  	//this.addControlerListener(new ScoringFunctionResetter()); TODO: check if really not necessary anymore!
	  	this.addControlerListener(new Memorizer(this.memories, this.day));
	  	this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesHerbieListener(CALC_LEG_TIMES_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME, this.scenarioData.getNetwork()));
	}
}
