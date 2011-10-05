/* *********************************************************************** *
 * project: org.matsim.*
 * HerbieEnergyFlowsController.java
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

package playground.christoph.energyflows.controller;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;
import herbie.running.controler.listeners.ScoreElements;
import herbie.running.scoring.HerbieScoringFunctionFactory;
import herbie.running.scoring.HerbieTravelCostCalculatorFactory;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

public class HerbieEnergyFlowsController extends EnergyFlowsController {

	final private static Logger log = Logger.getLogger(HerbieEnergyFlowsController.class);
	
	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
	
	private final HerbieConfigGroup herbieConfigGroup = new HerbieConfigGroup();
	
	public HerbieEnergyFlowsController(String[] args) {
		super(args);
		super.config.addModule(HerbieConfigGroup.GROUP_NAME, this.herbieConfigGroup);
		super.setOverwriteFiles(true);
	}

//	@Override
//	protected void loadData() {
//		super.loadData();
//		this.network = this.scenarioData.getNetwork();
//		this.population = this.scenarioData.getPopulation();
//		this.scenarioLoaded = true;
//	}
	
	@Override
	protected void setUp() {
		HerbieScoringFunctionFactory herbieScoringFunctionFactory = new HerbieScoringFunctionFactory(
				super.config,
				this.herbieConfigGroup,
				this.getFacilityPenalties(),
				this.getFacilities(),
				this.getNetwork());
		this.setScoringFunctionFactory(herbieScoringFunctionFactory);
				
		CharyparNagelScoringParameters params = herbieScoringFunctionFactory.getParams();
		
		HerbieTravelCostCalculatorFactory costCalculatorFactory = new HerbieTravelCostCalculatorFactory(params, this.herbieConfigGroup);
		PersonalizableTravelTime timeCalculator = super.getTravelTimeCalculator();
		PlanCalcScoreConfigGroup cnScoringGroup = null;
		costCalculatorFactory.createTravelCostCalculator(timeCalculator, cnScoringGroup);
		this.setTravelCostCalculatorFactory(costCalculatorFactory);
				
		super.setUp();
		
	}
	
	@Override
	protected void loadControlerListeners() {
		super.loadControlerListeners();
		this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesHerbieListener(CALC_LEG_TIMES_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME, this.scenarioData.getNetwork()));
//		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
	}

//	@Override
//	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelCost travelCosts, final PersonalizableTravelTime travelTimes) {
//		PlanAlgorithm router = null;
//		router = super.createRoutingAlgorithm(travelCosts, travelTimes);
//		return router;
//	}
	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: EnergyFlowsController config-file rerouting-share");
			System.out.println();
		} else if (args.length != 2) {
			log.error("Unexpected number of input arguments!");
			log.error("Expected path to a config file (String) and rerouting share (double, 0.0 ... 1.0) for transit agents.");
			System.exit(0);
		} else {
			final HerbieEnergyFlowsController controler = new HerbieEnergyFlowsController(args);
			controler.run();
		}
		
		System.exit(0);
	}
}
