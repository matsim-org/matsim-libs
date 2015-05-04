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

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.anhorni.surprice.analysis.AgentAnalysisShutdownListener;
import playground.anhorni.surprice.analysis.ModeSharesControlerListener;
import playground.anhorni.surprice.scoring.SurpriceScoringFunctionFactory;
import playground.anhorni.surprice.warmstart.AdaptNextDay;

public class DayControler extends Controler {
	
	private AgentMemories memories = new AgentMemories();
	private String day;	
	private ObjectAttributes preferences;
	private Population populationPreviousDay = null;
	private TerminationCriterionScoreBased terminationCriterion = null;
		
	public DayControler(final Config config, AgentMemories memories, String day, ObjectAttributes preferences, Population populationPreviousDay) {
		super(config);	
		super.setOverwriteFiles(true);
		this.memories = memories;	
		this.day = day;
		this.preferences = preferences;
		this.populationPreviousDay = populationPreviousDay;

        this.setScoringFunctionFactory(
				new SurpriceScoringFunctionFactory(
			  			this, this.getConfig().planCalcScore(), getScenario().getNetwork(), this.memories, this.day, this.preferences)
				);
        this.loadMyControlerListeners();
	} 
				
	protected void setUp() {
		SurpriceTravelDisutilityFactoryImpl travelDisutilityFactory = new SurpriceTravelDisutilityFactoryImpl(this.day, this.memories, this.preferences, this);
		this.setTravelDisutilityFactory(travelDisutilityFactory);
		super.setUp();	
	}
	
	private void setTermination(double stoppingRate) {
		this.terminationCriterion = new TerminationCriterionScoreBased(stoppingRate, this);
		super.setTerminationCriterion(this.terminationCriterion);
	}
	
	private void loadMyControlerListeners() {
//		super.loadControlerListeners();
		//this.addControlerListener(new ScoringFunctionResetter()); TODO: check if really not necessary anymore!
	  	this.addControlerListener(new Memorizer(this.memories, this.day));
	  	this.addControlerListener(new ModeSharesControlerListener("times"));
	  	this.addControlerListener(new ModeSharesControlerListener("distances"));  	
	  	this.addControlerListener(new AgentAnalysisShutdownListener(this.day, this.getControlerIO().getOutputPath()));
	  	
	  	if (Boolean.parseBoolean(this.getConfig().findParam(Surprice.SURPRICE_RUN, "useRoadPricing"))) {	
	  		this.addControlerListener(new RoadPricing(this.preferences));
		}
	  	double stoppingCriterionVal = Double.parseDouble(this.getConfig().findParam(Surprice.SURPRICE_RUN, "stoppingCriterionVal"));
	  	if (stoppingCriterionVal > 0.0) {	
	  		this.setTermination(stoppingCriterionVal);
	  	}
	  	if (Boolean.parseBoolean(this.getConfig().findParam(Surprice.SURPRICE_RUN, "warmstart"))) {
	  		this.addControlerListener(new AdaptNextDay(this.populationPreviousDay));
	  	}
	}
	
	public int getFinalIteration() {
		if (this.terminationCriterion == null) {
			return this.getConfig().controler().getLastIteration();
		}
		else {
			return this.terminationCriterion.getFinalIteration();
		}
	}
}
