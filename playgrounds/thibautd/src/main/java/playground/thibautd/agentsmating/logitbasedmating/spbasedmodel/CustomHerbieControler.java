/* *********************************************************************** *
 * project: org.matsim.*
 * KTIControler.java
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

package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.controler.HerbieControler;
import herbie.running.replanning.TransitStrategyManager;
import herbie.running.scoring.HerbieScoringFunctionFactory;
import herbie.running.scoring.HerbieTravelCostCalculatorFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.population.algorithms.PlanAlgorithm;



/**
 * Identical to the herbie controler, but with a constructor accepting a loaded scenario.
 */
public class CustomHerbieControler extends Controler {

	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";

	private final HerbieConfigGroup herbieConfigGroup;

	private static final Logger log = Logger.getLogger(Controler.class);
	
	public CustomHerbieControler(final Scenario scenario) {
		super(scenario);
		this.herbieConfigGroup = (HerbieConfigGroup)
				super.getConfig().getModule( HerbieConfigGroup.GROUP_NAME );
		//super.setOverwriteFiles(true);
	}

	@Override
	protected void loadData() {
		super.loadData();
		this.network = this.scenarioData.getNetwork();
		this.population = this.scenarioData.getPopulation();
		this.scenarioLoaded = true;
	}

	@Override
	protected void setUp() {
		HerbieScoringFunctionFactory herbieScoringFunctionFactory = new HerbieScoringFunctionFactory(
				super.config,
				this.herbieConfigGroup,
				this.getFacilityPenalties(),
				this.getFacilities(),
				this.getNetwork());
		// this.setScoringFunctionFactory(herbieScoringFunctionFactory);
				
		CharyparNagelScoringParameters params = herbieScoringFunctionFactory.getParams();
		
		HerbieTravelCostCalculatorFactory costCalculatorFactory = new HerbieTravelCostCalculatorFactory(params, this.herbieConfigGroup);
		TravelTime timeCalculator = super.getTravelTimeCalculator();
		PlanCalcScoreConfigGroup cnScoringGroup = null;
		costCalculatorFactory.createTravelDisutility(timeCalculator, cnScoringGroup);
		
		this.setTravelDisutilityFactory(costCalculatorFactory);
		
		super.setUp();
	}
	
	
	private double reroutingShare = 0.05;
	 /**
	  * Create and return a TransitStrategyManager which filters transit agents
	  * during the replanning phase. They either keep their selected plan or
	  * replan it.
	  */
	 @Override
	 protected StrategyManager loadStrategyManager() {
	  log.info("loading TransitStrategyManager - using rerouting share of " + reroutingShare);
	  StrategyManager manager = new TransitStrategyManager(this, reroutingShare);
	  StrategyManagerConfigLoader.load(this, manager);
	  return manager;
	 }

	@Override
	protected void loadControlerListeners() {
		super.loadControlerListeners();
		// this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		// this.addControlerListener(new CalcLegTimesHerbieListener(CALC_LEG_TIMES_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		// this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME, this.scenarioData.getNetwork()));
//		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(final TravelDisutility travelCosts, final TravelTime travelTimes) {
		PlanAlgorithm router = null;
		router = super.createRoutingAlgorithm(travelCosts, travelTimes);
		return router;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new HerbieControler(args);
			controler.run();
		}
		System.exit(0);
	}
}
