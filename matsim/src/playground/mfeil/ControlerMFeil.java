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


import org.matsim.core.api.replanning.PlanStrategyModule;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.PlanomatModule;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;


/**
 * @author Matthias Feil
 * Adjusting the Controler in order to call the PlanomatX. Replaces also the StrategyManagerConfigLoader.
 */
public class ControlerMFeil extends Controler {
	
	public ControlerMFeil (String [] args){
		super(args);
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
				strategy = new PlanStrategy(new RandomPlanSelector());
				PlanStrategyModule planomatXStrategyModule = new PlanomatX12Initialiser(this);
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
				PlanStrategyModule planomatStrategyModule = new PlanomatModule(this, this.getEvents(), this.getNetwork(), this.getScoringFunctionFactory(), this.getTravelCostCalculator(), this.getTravelTimeCalculator());
				strategy.addStrategyModule(planomatStrategyModule);
			}
			else if (classname.equals("TimeOptimizer")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				PlanStrategyModule timeOptStrategyModule = new TimeOptInitialiser(this);
				strategy.addStrategyModule(timeOptStrategyModule);
			}
			else if (classname.equals("Clustering")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				PlanStrategyModule module = new RecyclingModule1(this);
				strategy.addStrategyModule(module);
			}
			
			else if (classname.equals("TimeModeChoicer")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				PlanStrategyModule module = new TmcInitialiser(this);
				strategy.addStrategyModule(module);
			}
		
			manager.addStrategy(strategy, rate);
		}

		return manager;

	}
	
	@Override
		protected ScoringFunctionFactory loadScoringFunctionFactory() {
			//return new PlanomatXScoringFunctionFactory(this.getConfig().charyparNagelScoring());
			return new JohScoringFunctionFactory();
	}
	
}
