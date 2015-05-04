/* *********************************************************************** *
 * project: org.matsim.*
 * EnergyFlowsController.java
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

import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.christoph.energyflows.replanning.TransitStrategyManager;

import javax.inject.Provider;

public class EnergyFlowsController extends Controler {

	final private static Logger log = Logger.getLogger(EnergyFlowsController.class);
	
	private double reroutingShare = 0.0;
	
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
	
	/**
	 * Add ScoreStats and TravelDistancePlots for transit and non-transit Subpopulations.
	 */
	private void loadMyControlerListeners() {
//		super.loadControlerListeners();
		
		Scenario nonTransitSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population nonTransitPopulation = nonTransitSc.getPopulation();

		Scenario transitSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population transitPopulation = transitSc.getPopulation();
		
		for(Person person : this.getScenario().getPopulation().getPersons().values()) {
			int id = Integer.valueOf(person.getId().toString());
			if (id < 1000000000) nonTransitPopulation.addPerson(person);
			else transitPopulation.addPerson(person);
		}
		
		// add subpopulation score stats
		ScoreStatsControlerListener nonTransitScoreStats = new ScoreStatsControlerListener(getConfig(), nonTransitPopulation, super.getControlerIO().getOutputFilename("nontransit" + ScoreStatsControlerListener.FILENAME_SCORESTATS), getConfig().controler().isCreateGraphs());
		this.addControlerListener(nonTransitScoreStats);
		ScoreStatsControlerListener transitScoreStats = new ScoreStatsControlerListener(getConfig(), transitPopulation, super.getControlerIO().getOutputFilename("transit" + ScoreStatsControlerListener.FILENAME_SCORESTATS), getConfig().controler().isCreateGraphs());
		this.addControlerListener(transitScoreStats);

		// add subpopulation travel distance stats
        TravelDistanceStats nonTransitTravelDistanceStats = new TravelDistanceStats(nonTransitPopulation, getScenario().getNetwork(), super.getControlerIO().getOutputFilename("nontransit" + FILENAME_TRAVELDISTANCESTATS), getConfig().controler().isCreateGraphs());
		this.addControlerListener(nonTransitTravelDistanceStats);
        TravelDistanceStats transitTravelDistanceStats = new TravelDistanceStats(transitPopulation, getScenario().getNetwork(), super.getControlerIO().getOutputFilename("transit" + FILENAME_TRAVELDISTANCESTATS), getConfig().controler().isCreateGraphs());
		this.addControlerListener(transitTravelDistanceStats);
	}
	
	/**
	 * We use a Scoring Function that get the Facility Opening Times from
	 * the Facilities instead of the Config File.
	 */
	@Override
	protected void setUp() {
		if (this.getScoringFunctionFactory() == null) {
			this.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(this.getConfig().planCalcScore(), this.getScenario()));
		}
		super.setUp();
	}
	
	public EnergyFlowsController(String[] args) {
		super(args[0]);
		this.reroutingShare = Double.parseDouble(args[1]);
		log.info("Use transit agent rerouting share of " + Double.parseDouble(args[1]));
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
	}

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
			final EnergyFlowsController controler = new EnergyFlowsController(args);
			controler.run();
		}
		
		System.exit(0);
	}

}