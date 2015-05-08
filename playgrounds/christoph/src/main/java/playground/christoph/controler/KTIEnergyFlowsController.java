/* *********************************************************************** *
 * project: org.matsim.*
 * KTIEnergyFlowsController.java
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

package playground.christoph.controler;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalties;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.households.HouseholdsReaderV10;
import playground.christoph.energyflows.controller.EnergyFlowsController;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.kti.controler.listeners.KtiPopulationPreparation;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.ScoreElements;
import playground.meisterk.kti.router.KtiLinkNetworkRouteFactory;
import playground.meisterk.kti.router.KtiPtRouteFactory;
import playground.meisterk.kti.router.KtiTravelCostCalculatorFactory;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.scenario.KtiScenarioLoaderImpl;
import playground.meisterk.kti.scoring.KTIYear3ScoringFunctionFactory;
import playground.meisterk.org.matsim.config.PlanomatConfigGroup;

import java.util.Map.Entry;

public class KTIEnergyFlowsController { 
//extends EnergyFlowsController {
//
//	final private static Logger log = Logger.getLogger(KTIEnergyFlowsController.class);
//	
//	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
//	protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
//	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
//	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
//
//	private final KtiConfigGroup ktiConfigGroup;
//	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
//	
//	public KTIEnergyFlowsController(String[] args) {
//		super(args);
//
//		/*
//		 * Create the empty object. They are filled in the loadData() method.
//		 */
//		this.ktiConfigGroup = new KtiConfigGroup();
//		this.plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo(this.ktiConfigGroup);
//		this.loadMyControlerListeners();
//		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE + Gbl.SET_UP_IS_NOW_FINAL
//				+ Gbl.LOAD_DATA_IS_NOW_FINAL ) ;
//		
//	}
//	
////	@Override
////	protected void loadData() {
////		if (!this.isScenarioLoaded()) {
////			
////			/*
////			 * The KTIConfigGroup is loaded as generic Module. We replace this
////			 * generic object with a KtiConfigGroup object and copy all its parameter.
////			 */
////			ConfigGroup module = this.getConfig().getModule(KtiConfigGroup.GROUP_NAME);
////			this.getConfig().removeModule(KtiConfigGroup.GROUP_NAME);
////			this.getConfig().addModule(this.ktiConfigGroup);
////			
////			for (Entry<String, String> entry : module.getParams().entrySet()) {
////				this.ktiConfigGroup.addParam(entry.getKey(), entry.getValue());
////			}
////				
////			/*
////			 * Use KTI route factories.
////			 */
////            ((PopulationFactoryImpl) getScenario().getPopulation().getFactory()).setRouteFactory(TransportMode.car, new KtiLinkNetworkRouteFactory(getScenario().getNetwork(), new PlanomatConfigGroup()));
////            ((PopulationFactoryImpl) getScenario().getPopulation().getFactory()).setRouteFactory(TransportMode.pt, new KtiPtRouteFactory(this.plansCalcRouteKtiInfo));
////
////			
////			KtiScenarioLoaderImpl loader = new KtiScenarioLoaderImpl(this.getScenario(), this.plansCalcRouteKtiInfo, this.ktiConfigGroup);
////			loader.loadScenario();
////			if (this.getConfig().scenario().isUseHouseholds()) {
////				this.loadHouseholds();
////			}
////			this.setScenarioLoaded(true);
////		}
////		
////		// connect facilities to links
////        new WorldConnectLocations(this.getConfig()).connectFacilitiesWithLinks(getScenario().getActivityFacilities(), (NetworkImpl) getScenario().getNetwork());
////	}
//	
//	private void loadHouseholds() {
//		if ((this.getScenario().getHouseholds() != null) && (this.getConfig().households() != null) && (this.getConfig().households().getInputFile() != null) ) {
//			String hhFileName = this.getConfig().households().getInputFile();
//			log.info("loading households from " + hhFileName);
//			new HouseholdsReaderV10(this.getScenario().getHouseholds()).parse(hhFileName);
//			log.info("households loaded.");
//		}
//		else {
//			log.info("no households file set in config or feature disabled, not able to load anything");
//		}
//	}
//	
////	@Override
////	protected void setUp() {
////
////        KTIYear3ScoringFunctionFactory kTIYear3ScoringFunctionFactory = new KTIYear3ScoringFunctionFactory(
////				this.getScenario(),
////				this.ktiConfigGroup,
////				((FacilityPenalties) this.getScenario().getScenarioElement(FacilityPenalties.ELEMENT_NAME)).getFacilityPenalties(),
////                getScenario().getActivityFacilities());
////		this.setScoringFunctionFactory(kTIYear3ScoringFunctionFactory);
////
////		final KtiTravelCostCalculatorFactory costCalculatorFactory = new KtiTravelCostCalculatorFactory(ktiConfigGroup);
////		this.addOverridingModule(new AbstractModule() {
////			@Override
////			public void install() {
////				bindTravelDisutilityFactory().toInstance(costCalculatorFactory);
////			}
////		});
////
////		super.setUp();
////	}
//	
//	private void loadMyControlerListeners() {
//
////		super.loadControlerListeners();
//
//		// the scoring function processes facility loads
//		this.addControlerListener(new FacilitiesLoadCalculator(((FacilityPenalties) this.getScenario().getScenarioElement(FacilityPenalties.ELEMENT_NAME)).getFacilityPenalties()));
//		this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
//		this.addControlerListener(new CalcLegTimesKTIListener(CALC_LEG_TIMES_KTI_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
//		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME));
//		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
//	}
//
////	@Override
////	public PlanAlgorithm createRoutingAlgorithm() {
////		return this.ktiConfigGroup.isUsePlansCalcRouteKti() ?
////				createKtiRoutingAlgorithm(
////						this.createTravelCostCalculator(),
////						this.getLinkTravelTimes()) :
////				super.createRoutingAlgorithm();
////	}
//
//	//private PlanAlgorithm createKtiRoutingAlgorithm(final TravelDisutility travelCosts, final TravelTime travelTimes) {
//	//	return new PlansCalcRouteKti(
//	//				super.getConfig().plansCalcRoute(),
//	//				super.network,
//	//				travelCosts,
//	//				travelTimes,
//	//				super.getLeastCostPathCalculatorFactory(),
//	//				((PopulationFactoryImpl) this.population.getFactory()).getModeRouteFactory(),
//	//				this.plansCalcRouteKtiInfo);
//	//}
//	
//	public static void main(final String[] args) {
//		if ((args == null) || (args.length == 0)) {
//			System.out.println("No argument given!");
//			System.out.println("Usage: KTIFlowsController config-file rerouting-share");
//			System.out.println();
//		} else if (args.length != 2) {
//			log.error("Unexpected number of input arguments!");
//			log.error("Expected path to a config file (String) and rerouting share (double, 0.0 ... 1.0) for transit agents.");
//			System.exit(0);
//		} else {
//			final KTIEnergyFlowsController controler = new KTIEnergyFlowsController(args);
//			controler.run();
//		}
//		
//		System.exit(0);
//	}
}
