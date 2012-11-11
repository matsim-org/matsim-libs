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

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.PlansCalcAreaTollRoute;
import org.matsim.roadpricing.RoadPricing;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.anhorni.surprice.analysis.ModeSharesControlerListener;
import playground.anhorni.surprice.scoring.SurpriceScoringFunctionFactory;

public class DayControler extends Controler {
	
	private AgentMemories memories = new AgentMemories();
	private String day;	
	private ObjectAttributes preferences;
		
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
	}
	
	@Override
	public PlanAlgorithm createRoutingAlgorithm(TravelDisutility travelCosts, TravelTime travelTimes) {
		
		RoadPricingSchemeImpl scheme = (RoadPricingSchemeImpl) this.scenarioData.getScenarioElement(RoadPricingScheme.class);
		
		if (scheme.getType().equals("area")) {		
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();
		return new PlansCalcAreaTollRoute(this.config.plansCalcRoute(), this.network, travelCosts,
				travelTimes, this.getLeastCostPathCalculatorFactory(), routeFactory, (RoadPricingSchemeImpl) this.scenarioData.getScenarioElement(RoadPricingScheme.class));
		}
		else {
			return super.createRoutingAlgorithm(travelCosts, travelTimes);
		}
	}
	
	protected void loadControlerListeners() {
		super.loadControlerListeners();
		//this.addControlerListener(new ScoringFunctionResetter()); TODO: check if really not necessary anymore!
	  	this.addControlerListener(new Memorizer(this.memories, this.day));
	  	this.addControlerListener(new ModeSharesControlerListener("times"));
	  	this.addControlerListener(new ModeSharesControlerListener("distances"));
	  	
	  	if (Boolean.parseBoolean(this.config.findParam(Surprice.SURPRICE_RUN, "useRoadPricing"))) {	
	  		this.addControlerListener(new RoadPricing());
		}
	}
}
