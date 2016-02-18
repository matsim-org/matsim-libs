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

package herbie.running.controler;

import herbie.running.config.HerbieConfigGroup;
import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

/**
 * Controler for the Herbie project.
 */
public class HerbieControler {

	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";

	private static final Logger log = Logger.getLogger(Controler.class);


//	@Override
//	protected void loadData() {
//		super.loadData();
//		this.setScenarioLoaded(true);
//	}

//	@Override
//	protected void setUp() {
//        HerbieScoringFunctionFactory herbieScoringFunctionFactory = new HerbieScoringFunctionFactory(
//				super.getConfig(),
//				this.herbieConfigGroup,
//				((FacilityPenalties) this.getScenario().getScenarioElement(FacilityPenalties.ELEMENT_NAME)).getFacilityPenalties(),
//                getScenario().getActivityFacilities(),
//                getScenario().getNetwork());
//		this.setScoringFunctionFactory(herbieScoringFunctionFactory);
//				
//		CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(getConfig().planCalcScore());
//		
//		final HerbieTravelCostCalculatorFactory costCalculatorFactory = new HerbieTravelCostCalculatorFactory(params, this.herbieConfigGroup);
//		TravelTime timeCalculator = super.getLinkTravelTimes();
//		PlanCalcScoreConfigGroup cnScoringGroup = null;
//		costCalculatorFactory.createTravelDisutility(timeCalculator, cnScoringGroup);
//
//		this.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				bindCarTravelDisutilityFactory().toInstance(costCalculatorFactory);
//			}
//		});
//
//		super.setUp();
//	}
	
	

	public HerbieControler(String[] args) {

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
			final Controler controler = new Controler(args);
			ConfigUtils.addOrGetModule(controler.getConfig(), HerbieConfigGroup.GROUP_NAME, HerbieConfigGroup.class);
			controler.addOverridingModule(new HerbieModule());
			controler.run();
		}
		System.exit(0);
	}

}
