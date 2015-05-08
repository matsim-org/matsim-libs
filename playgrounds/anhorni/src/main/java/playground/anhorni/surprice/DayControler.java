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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.anhorni.surprice.analysis.AgentAnalysisShutdownListener;
import playground.anhorni.surprice.analysis.ModeSharesControlerListener;
import playground.anhorni.surprice.scoring.SurpriceScoringFunctionFactory;
import playground.anhorni.surprice.warmstart.AdaptNextDay;

public class DayControler  {
	Controler cc ;
	
	private AgentMemories memories = new AgentMemories();
	private String day;	
	private ObjectAttributes preferences;
	private Population populationPreviousDay = null;
	private TerminationCriterionScoreBased terminationCriterion = null;
		
	public DayControler(final Config config, AgentMemories memories, String day, ObjectAttributes preferences, Population populationPreviousDay) {
//		super(config);
		cc = new Controler( config ) ;
		cc.setOverwriteFiles(true);
		this.memories = memories;	
		this.day = day;
		this.preferences = preferences;
		this.populationPreviousDay = populationPreviousDay;

		cc.setScoringFunctionFactory(
				new SurpriceScoringFunctionFactory(
						cc, cc.getConfig().planCalcScore(), cc.getScenario().getNetwork(), this.memories, this.day, this.preferences)
				);
        this.loadMyControlerListeners();
        
        throw new RuntimeException( Gbl.SET_UP_IS_NOW_FINAL + Gbl.RETROFIT_CONTROLER  ) ;
	} 
				
//	protected void setUp() {
//		final SurpriceTravelDisutilityFactoryImpl travelDisutilityFactory = new SurpriceTravelDisutilityFactoryImpl(this.day, this.memories, this.preferences, this);
//		this.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				bindTravelDisutilityFactory().toInstance(travelDisutilityFactory);
//			}
//		});
//		super.setUp();
//	}
	
	private void setTermination(double stoppingRate) {
		this.terminationCriterion = new TerminationCriterionScoreBased(stoppingRate, cc);
		cc.setTerminationCriterion(this.terminationCriterion);
	}
	
	private void loadMyControlerListeners() {
//		super.loadControlerListeners();
		//this.addControlerListener(new ScoringFunctionResetter()); TODO: check if really not necessary anymore!
		cc.addControlerListener(new Memorizer(this.memories, this.day));
		cc.addControlerListener(new ModeSharesControlerListener("times"));
		cc.addControlerListener(new ModeSharesControlerListener("distances"));  	
		cc.addControlerListener(new AgentAnalysisShutdownListener(this.day, cc.getControlerIO().getOutputPath()));
	  	
	  	if (Boolean.parseBoolean(cc.getConfig().findParam(Surprice.SURPRICE_RUN, "useRoadPricing"))) {	
	  		cc.addControlerListener(new RoadPricing(this.preferences));
		}
	  	double stoppingCriterionVal = Double.parseDouble(cc.getConfig().findParam(Surprice.SURPRICE_RUN, "stoppingCriterionVal"));
	  	if (stoppingCriterionVal > 0.0) {	
	  		this.setTermination(stoppingCriterionVal);
	  	}
	  	if (Boolean.parseBoolean(cc.getConfig().findParam(Surprice.SURPRICE_RUN, "warmstart"))) {
	  		cc.addControlerListener(new AdaptNextDay(this.populationPreviousDay));
	  	}
	}
	
	public int getFinalIteration() {
		if (this.terminationCriterion == null) {
			return cc.getConfig().controler().getLastIteration();
		}
		else {
			return this.terminationCriterion.getFinalIteration();
		}
	}

	public void run() {
		cc.run() ;
	}

	public Scenario getScenario() {
		return cc.getScenario() ;
	}
}
