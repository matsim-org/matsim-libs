/* *********************************************************************** *
 * project: org.matsim.*
 * JointHerbieControler.java
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
package playground.thibautd.cliquessim.herbie.run;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;
import herbie.running.scoring.HerbieTravelCostCalculatorFactory;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.thibautd.cliquessim.population.ScenarioWithCliques;
import playground.thibautd.cliquessim.run.JointControler;
import playground.thibautd.herbie.HerbiePlanBasedScoringFunctionFactory;
import playground.thibautd.parknride.scoring.ParkingPenaltyFactory;

/**
 * @author thibautd
 */
public class JointHerbieControler extends JointControler {
	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";

	private final HerbieConfigGroup herbieConfigGroup;
	private ParkingPenaltyFactory penaltyFactory;

	private static final Logger log = Logger.getLogger(Controler.class);
	
	public JointHerbieControler( final ScenarioWithCliques scenario ) {
		super( scenario );
		herbieConfigGroup = (HerbieConfigGroup) super.config.getModule(HerbieConfigGroup.GROUP_NAME);
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
		HerbiePlanBasedScoringFunctionFactory herbieScoringFunctionFactory =
			new HerbiePlanBasedScoringFunctionFactory(
				super.config,
				this.herbieConfigGroup,
				this.getFacilityPenalties(),
				this.getFacilities(),
				this.getNetwork());

		this.setScoringFunctionFactory( herbieScoringFunctionFactory );
				
		CharyparNagelScoringParameters params = herbieScoringFunctionFactory.getParams();
		
		HerbieTravelCostCalculatorFactory costCalculatorFactory = new HerbieTravelCostCalculatorFactory(params, this.herbieConfigGroup);
		TravelTime timeCalculator = super.getTravelTimeCalculator();
		PlanCalcScoreConfigGroup cnScoringGroup = null;
		costCalculatorFactory.createTravelDisutility(timeCalculator, cnScoringGroup);
		
		this.setTravelDisutilityFactory(costCalculatorFactory);
		
		super.setUp();

		// set the TransitRouterFactory rather than a RoutingModuleFactory, so that
		// if some parts of the code use this method, everything should be consistent.
		//setTransitRouterFactory(
		//		new HerbieTransitRouterFactory( 
		//			getScenario().getTransitSchedule(),
		//			new TransitRouterConfig(
		//				config.planCalcScore(),
		//				config.plansCalcRoute(),
		//				config.transitRouter(),
		//				config.vspExperimental()),
		//			herbieConfigGroup,
		//			new TravelScoringFunction( params, herbieConfigGroup ) ) );
	}
	
	
	private double reroutingShare = 0.05;

	 ///**
	 // * Create and return a TransitStrategyManager which filters transit agents
	 // * during the replanning phase. They either keep their selected plan or
	 // * replan it.
	 // */
	 //@Override
	 //protected StrategyManager loadStrategyManager() {
	 // log.info("loading TransitStrategyManager - using rerouting share of " + reroutingShare);
	 // StrategyManager manager = new TransitStrategyManager(this, reroutingShare);
	 // StrategyManagerConfigLoader.load(this, manager);
	 // return manager;
	 //}

	@Override
	protected void loadControlerListeners() {
		super.loadControlerListeners();
		//this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesHerbieListener(CALC_LEG_TIMES_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME, this.scenarioData.getNetwork()));
//		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
	}

	//@Override
	//public PlanAlgorithm createRoutingAlgorithm(final TravelDisutility travelCosts, final PersonalizableTravelTime travelTimes) {
	//	PlanAlgorithm router = null;
	//	router = super.createRoutingAlgorithm(travelCosts, travelTimes);
	//	return router;
	//}
}
