/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatStrategyManagerConfigLoader.java
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

package org.matsim.planomat;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.modules.ExternalModule;
import org.matsim.replanning.modules.PlanomatExe;
import org.matsim.replanning.modules.PlanomatOptimizeTimes;
import org.matsim.replanning.modules.ReRouteDijkstra;
import org.matsim.replanning.modules.ReRouteLandmarks;
import org.matsim.replanning.modules.StrategyModuleI;
import org.matsim.replanning.modules.TimeAllocationMutator;
import org.matsim.replanning.selectors.BestPlanSelector;
import org.matsim.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.replanning.selectors.KeepSelected;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;

/**
 * @deprecated the planomat-stuff is now integrated into the regular StrategyManagerConfigLoader
 */
@Deprecated
public class PlanomatStrategyManagerConfigLoader {

	public static void load(
			final Config config,
			final StrategyManager manager,
			final NetworkLayer network,
			final TravelCostI travelCostCalc,
			final TravelTimeI travelTimeCalc,
			final LegTravelTimeEstimator legTravelTimeEstimator) {

		String maxvalue = Gbl.getConfig().findParam("strategy", "maxAgentPlanMemorySize");
		if (maxvalue != null) {
			manager.setMaxPlansPerAgent(Integer.parseInt(maxvalue));
		}

		int externalCounter = 0;
		int i = 0;
		while (true) {
			i++;
			String modrate = "ModuleProbability_" + i;
			String ratevalue = Gbl.getConfig().findParam("strategy", modrate);
			if (ratevalue == null) break;
			double rate = Double.parseDouble(ratevalue);
			if (rate == 0.0) continue;
			String modname = "Module_" + i;
			String classname = Gbl.getConfig().getParam("strategy", modname);
			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
			}
			PlanStrategy strategy = null;
			if (classname.equals("KeepLastSelected")) {
				strategy = new PlanStrategy(new KeepSelected());
			} else if (classname.equals("ReRoute") || classname.equals("threaded.ReRoute")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new ReRouteDijkstra(network, travelCostCalc, travelTimeCalc));

				int controlerFirstIteration = Gbl.getConfig().controler().getFirstIteration();
				int controlerLastIteration = Gbl.getConfig().controler().getLastIteration();

				for (int iter = controlerFirstIteration; iter <= controlerLastIteration; iter++) {

					double newModuleProbability = PlanomatStrategyManagerConfigLoader.getDecayingModuleProbability(iter, 100, rate);
					manager.addChangeRequest(iter, strategy, newModuleProbability);

				}

			} else if (classname.equals("ReRoute_Landmarks")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
				preProcessRoutingData.run(network);
				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));
			} else if (classname.equals("TimeAllocationMutator") || classname.equals("threaded.TimeAllocationMutator")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new TimeAllocationMutator());
			} else if (classname.equals("ExternalModule")) {
				externalCounter++;
				strategy = new PlanStrategy(new RandomPlanSelector());
				String exePath = Gbl.getConfig().getParam("strategy", "ModuleExePath_" + i);
				strategy.addStrategyModule(new ExternalModule(exePath, "ext" + externalCounter));
			} else if (classname.equals("PlanomatExe")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				String exePath = Gbl.getConfig().getParam("strategy", "ModuleExePath_" + i);
				strategy.addStrategyModule(new PlanomatExe(exePath));
				strategy.addStrategyModule(new ReRouteDijkstra(network, travelCostCalc, travelTimeCalc));
			} else if (classname.equals("PlanomatTimeRouteExe")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				String exePath = Gbl.getConfig().getParam("strategy", "ModuleExePath_" + i);
				strategy.addStrategyModule(new PlanomatExe(exePath));
			} else if (classname.equals("Planomat")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				StrategyModuleI planomatStrategyModule = new PlanomatOptimizeTimes(legTravelTimeEstimator);
				strategy.addStrategyModule(planomatStrategyModule);

				int controlerFirstIteration = Gbl.getConfig().controler().getFirstIteration();
				int controlerLastIteration = Gbl.getConfig().controler().getLastIteration();

				for (int iter = controlerFirstIteration; iter <= controlerLastIteration; iter++) {

					double newModuleProbability = PlanomatStrategyManagerConfigLoader.getDecayingModuleProbability(iter, 100, rate);
					manager.addChangeRequest(iter, strategy, newModuleProbability);

				}

			} else if (classname.equals("PlanomatReRoute")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				StrategyModuleI planomatStrategyModule = new PlanomatOptimizeTimes(legTravelTimeEstimator);
				strategy.addStrategyModule(planomatStrategyModule);

				PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
				preProcessRoutingData.run(network);
				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));

				int controlerFirstIteration = Gbl.getConfig().controler().getFirstIteration();
				int controlerLastIteration = Gbl.getConfig().controler().getLastIteration();

				for (int iter = controlerFirstIteration; iter <= controlerLastIteration; iter++) {

					double newModuleProbability = PlanomatStrategyManagerConfigLoader.getDecayingModuleProbability(iter, controlerFirstIteration, rate);
					manager.addChangeRequest(iter, strategy, newModuleProbability);

				}
			} else if (classname.equals("BestScore")) {
				strategy = new PlanStrategy(new BestPlanSelector());
			} else if (classname.equals("SelectExpBeta")) {
				strategy = new PlanStrategy(new ExpBetaPlanSelector());
			} else if (classname.equals("SelectRandom")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
			}

			if (strategy == null) {
				Gbl.errorMsg("Could not initialize strategy named " + classname);
			}

			manager.addStrategy(strategy, rate);

			// now check if this modules should be disabled after some iterations
			String moditer = "ModuleDisableAfterIteration_" + i;
			String itervalue = Gbl.getConfig().findParam("strategy", moditer);
			if (itervalue != null) {
				int maxIter = Integer.MAX_VALUE;
				maxIter = Integer.parseInt(itervalue);
				manager.addChangeRequest(maxIter + 1, strategy, 0.0);
			}
		}

	}

	// HIER WEITER!!!
	public static double getDecayingModuleProbability(final int iteration, final int iterationStartDecay, final double pReplanInit) {

		double pReplan = 0.0;

		// everything hard wired...

		double pReplanFinal = 0.0;
		int iterOffset = 0;
		double slope = 1.0;

		// at first, use the typical replanning share from the config file
		if (iteration <= iterationStartDecay) {

			pReplan = 0.1;
		}
		// then use the decaying replanning share
		else {

			pReplan = Math.min(pReplanInit, slope / (iteration - iterationStartDecay + iterOffset) + pReplanFinal);

		}
		return pReplan;

	}

}
