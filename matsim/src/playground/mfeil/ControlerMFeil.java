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


import org.matsim.config.groups.StrategyConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.modules.PlanomatOptimizeTimes;
import org.matsim.replanning.modules.ReRoute;
import org.matsim.replanning.modules.StrategyModule;
import org.matsim.replanning.selectors.BestPlanSelector;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.scoring.*;


/**
 * @author Matthias Feil
 * Adjusting the Controler in order to call the PlanomatX. Replaces also the StrategyManagerConfigLoader.
 */
public class ControlerMFeil extends org.matsim.controler.Controler {
	
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
				StrategyModule planomatXStrategyModule = new PlanomatX12Initialiser(this);
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
				StrategyModule planomatStrategyModule = new PlanomatOptimizeTimes(this);
				strategy.addStrategyModule(planomatStrategyModule);
			}
			else if (classname.equals("TimeOptimizer")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(
						super.network, 
						super.getTraveltimeBinSize());
				LegTravelTimeEstimator legTravelTimeEstimator = Gbl.getConfig().planomat().getLegTravelTimeEstimator(
						super.getTravelTimeCalculator(), 
						super.getTravelCostCalculator(), 
						tDepDelayCalc, 
						super.network);
				StrategyModule timeOptStrategyModule = new TimeOptInitialiser(legTravelTimeEstimator, this.scoringFunctionFactory);
				strategy.addStrategyModule(timeOptStrategyModule);
			}
			else if (classname.equals("Clustering")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				StrategyModule module = new RecyclingModule(this);
				strategy.addStrategyModule(module);
			}
		
			manager.addStrategy(strategy, rate);
		}

		return manager;

	}
	
	@Override
		protected ScoringFunctionFactory loadScoringFunctionFactory() {
			return new JohScoringFunctionFactory();
	}
	
}
