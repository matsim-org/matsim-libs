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

import java.util.ArrayList;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.PlansCalcAreaTollRoute;
import org.matsim.roadpricing.RoadPricing;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.TravelDisutilityIncludingToll;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.anhorni.surprice.analysis.ModeSharesControlerListener;
import playground.anhorni.surprice.analysis.SupriceBoxPlot;
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
	  	
	  	this.printPrefs();
	}
	
	private void printPrefs() {
		SupriceBoxPlot boxPlotPrefs = new SupriceBoxPlot("Prefs", "pref", "prefs");
		ArrayList<Double> alpha = new ArrayList<Double>();
		ArrayList<Double> gamma = new ArrayList<Double>();
		
		for (Person p : this.population.getPersons().values()) {
			SurpriceScoringFunctionFactory sf = (SurpriceScoringFunctionFactory) this.getScoringFunctionFactory().createNewScoringFunction(p.getSelectedPlan());
			alpha.add(sf.getAlpha() + sf.getAlphaTrip());
			gamma.add(sf.getGamma() + sf.getGammaTrip());
		}
		boxPlotPrefs.addValuesPerCategory(alpha, "alpha", "alpha");
		boxPlotPrefs.addValuesPerCategory(gamma, "gamma", "gamma");
		boxPlotPrefs.createChart();
		boxPlotPrefs.saveAsPng(this.getControlerIO().getOutputFilename("prefs.png"), 800, 600);	
	}
	
	@Override
	public PlanAlgorithm createRoutingAlgorithm(TravelDisutility travelCosts, TravelTime travelTimes) {
		
		RoadPricingSchemeImpl scheme = (RoadPricingSchemeImpl) this.scenarioData.getScenarioElement(RoadPricingScheme.class);
		
		if (scheme.getType().equals("area")) {		
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();
		return new PlansCalcAreaTollRoute(this.config.plansCalcRoute(), this.network, travelCosts,
				travelTimes, this.getLeastCostPathCalculatorFactory(), routeFactory, 
				(RoadPricingSchemeImpl) this.scenarioData.getScenarioElement(RoadPricingScheme.class));
		}
		else {
			final TravelDisutilityFactory previousTravelCostCalculatorFactory = super.getTravelDisutilityFactory();
			TravelDisutility costsIncludingTolls = new TravelDisutilityIncludingToll(previousTravelCostCalculatorFactory.createTravelDisutility(
					travelTimes, this.config.planCalcScore()), 
					(RoadPricingSchemeImpl) this.scenarioData.getScenarioElement(RoadPricingScheme.class));
			return super.createRoutingAlgorithm(costsIncludingTolls, travelTimes);
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
