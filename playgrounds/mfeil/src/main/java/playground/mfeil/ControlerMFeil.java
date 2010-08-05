/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerMFeil.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.mfeil;



import java.io.FileNotFoundException;

import java.io.IOException;

import org.matsim.analysis.ScoreStats;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.LegHistogramListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.PlanomatModule;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.counts.CountControlerListener;
import org.matsim.counts.Counts;
import org.matsim.locationchoice.LocationChoice;

import playground.mfeil.MDSAM.ActivityTypeFinder;
import playground.mfeil.MDSAM.PlansConstructor;
import playground.mfeil.attributes.AgentsAttributesAdder;
import playground.mfeil.analysis.TravelStats;


/**
 * @author Matthias Feil
 * Adjusting the Controler in order to call the PlanomatX. Replaces also the StrategyManagerConfigLoader.
 */
public class ControlerMFeil extends Controler {
	
	private boolean createGraphs = true;
	private ScoreStats scoreStats = null;
	private TravelStats travelStats = null;
	private Counts counts = null;
	public static final String FILENAME_TRAVELTIMESTATS = "traveltimestats.txt";

	public ControlerMFeil (String [] args){
		super(args);
	}

	public ControlerMFeil (final Config config) {
		super(config);
	}

	@Override
	protected void loadData() {
		super.loadData();
//		if (!this.scenarioLoaded) {
//			this.loader = new ScenarioLoaderImpl(this.scenarioData);
//			this.loader.loadScenario();
//			this.network = loadNetwork();
//			this.population = loadPopulation();
//			this.scenarioLoaded = true;

			// loading income data!
			new AgentsAttributesAdder().loadIncomeData(this.scenarioData);

//			if (this.getScenario().getWorld() != null) {
//				new WorldCheck().run(this.getScenario().getWorld());
//			}
//		}
	}


		/*
		 * @return A fully initialized StrategyManager for the plans replanning.
		 */

	@Override
	protected StrategyManager loadStrategyManager() {

		final StrategyManager manager = new StrategyManager();
		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());

		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
			double rate = settings.getProbability();
			if (rate == 0.0) {
				continue;
			}
			String classname = settings.getModuleName();
			PlanStrategy strategy = null;

			if (classname.equals("PlanomatX")) {
				ActivityTypeFinder finder = new ActivityTypeFinder (this);
				//finder.run(this.getFacilities());
				strategy = new PlanStrategy(new RandomPlanSelector());
				PlanStrategyModule planomatXStrategyModule = new PlanomatXInitialiser(this, finder);
				strategy.addStrategyModule(planomatXStrategyModule);
			}
			else if  (classname.equals("ReRoute") || classname.equals("threaded.ReRoute")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new ReRoute(this));
			}
			else if (classname.equals("BestScore")) {
				strategy = new PlanStrategy(new BestPlanSelector());
			}
			else if (classname.equals("Planomat")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				PlanStrategyModule planomatStrategyModule = new PlanomatModule(this, this.getEvents(), this.getNetwork(), this.getScoringFunctionFactory(), this.createTravelCostCalculator(), this.getTravelTimeCalculator());
				strategy.addStrategyModule(planomatStrategyModule);
			}
			else if (classname.equals("Recycling")) {
				ActivityTypeFinder finder = new ActivityTypeFinder (this);
				finder.run(this.getFacilities());
				strategy = new PlanStrategy(new RandomPlanSelector());
				PlanStrategyModule module = new ScheduleRecycling(this, finder);
				strategy.addStrategyModule(module);
			}
			else if (classname.equals("TimeModeChoicer")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				PlanStrategyModule module = new TimeModeChoicerInitialiser(this);
				strategy.addStrategyModule(module);
			}
			else if (classname.equals("LocationChoice")) {
	    	strategy = new PlanStrategy(new ExpBetaPlanSelector(config.charyparNagelScoring()));
	    	strategy.addStrategyModule(new LocationChoice(this.getNetwork(), this, (this.getScenario()).getKnowledges()));
	    	strategy.addStrategyModule(new ReRoute(this));
				strategy.addStrategyModule(new TimeAllocationMutator(config));
			}
			else if (classname.equals("PlansConstructor")) {
		    	strategy = new PlanStrategy(new KeepSelected());
				strategy.addStrategyModule(new PlansConstructor(this));
			}
		
			manager.addStrategy(strategy, rate);
		}

		return manager;

	}

	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		//return new PlanomatXScoringFunctionFactory(this.getConfig().charyparNagelScoring());
		//return new JohScoringFunctionFactory();
		return new JohScoringFunctionEstimationFactory(this.network);
	}
	
	@Override
	protected void loadControlerListeners() {
		// optional: LegHistogram
		this.addControlerListener(new LegHistogramListener(this.events, this.createGraphs));

		// optional: score stats
		try {
			this.scoreStats = new ScoreStats(this.population, super.getControlerIO().getOutputFilename(FILENAME_SCORESTATS), this.createGraphs);
			this.addControlerListener(this.scoreStats);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// optional: travel distance stats
		try {
			this.travelStats = new TravelStats(this, this.population, this.network,
					super.getControlerIO().getOutputFilename(FILENAME_TRAVELDISTANCESTATS), super.getControlerIO().getOutputFilename(FILENAME_TRAVELTIMESTATS), this.createGraphs);
			this.addControlerListener(this.travelStats);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		
		// load counts, if requested
		if (this.config.counts().getCountsFileName() != null) {
			CountControlerListener ccl = new CountControlerListener(this.config);
			this.addControlerListener(ccl);
			this.counts = ccl.getCounts();
		}
	}
}
