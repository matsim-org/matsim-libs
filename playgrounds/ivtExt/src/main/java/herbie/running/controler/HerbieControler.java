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

import com.google.inject.Singleton;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;
import herbie.running.controler.listeners.ScoreElements;
import herbie.running.replanning.TransitStrategyManager;
import herbie.running.scoring.HerbieScoringFunctionFactory;
import herbie.running.scoring.HerbieTravelCostCalculatorFactory;

import org.apache.log4j.Logger;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalties;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import javax.inject.Provider;

/**
 * Controler for the Herbie project.
 */
public class HerbieControler extends Controler {

	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";

	private final HerbieConfigGroup herbieConfigGroup = new HerbieConfigGroup();

	private static final Logger log = Logger.getLogger(Controler.class);
	
	public HerbieControler(String[] args) {
		super(args);
		super.getConfig().addModule(this.herbieConfigGroup);
		super.setOverwriteFiles(true);
        addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
				bind(StrategyManager.class).toProvider(new com.google.inject.Provider<StrategyManager>() {
                    @Override
                    public StrategyManager get() {
                        return new Provider<StrategyManager>() {
                            @Override
                            public StrategyManager get() {
                                return myLoadStrategyManager();
                            }
                        }.get();
                    }
                }).in(Singleton.class);
			}
        });
        this.loadMyControlerListeners();
        throw new RuntimeException(Gbl.SET_UP_IS_NOW_FINAL) ;
	}

	@Override
	protected void loadData() {
		super.loadData();
		this.setScenarioLoaded(true);
	}

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
//				bindTravelDisutilityFactory().toInstance(costCalculatorFactory);
//			}
//		});
//
//		super.setUp();
//	}
	
	
	private double reroutingShare = 0.05;
	 /**
	  * Create and return a TransitStrategyManager which filters transit agents
	  * during the replanning phase. They either keep their selected plan or
	  * replan it.
	  */
	 private StrategyManager myLoadStrategyManager() {
	  log.info("loading TransitStrategyManager - using rerouting share of " + reroutingShare);
	  StrategyManager manager = new TransitStrategyManager(this, reroutingShare);
	  StrategyManagerConfigLoader.load(this, manager);
	  return manager;
	 }

	private void loadMyControlerListeners() {
//		super.loadControlerListeners();
		this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesHerbieListener(CALC_LEG_TIMES_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME, this.getScenario().getNetwork()));
//		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
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
