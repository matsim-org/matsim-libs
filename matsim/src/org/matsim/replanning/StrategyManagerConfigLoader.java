/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyManagerConfigLoader.java
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

package org.matsim.replanning;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.replanning.modules.ExternalModule;
import org.matsim.replanning.modules.PlanomatExe;
import org.matsim.replanning.modules.ReRoute;
import org.matsim.replanning.modules.ReRouteLandmarks;
import org.matsim.replanning.modules.TimeAllocationMutator;
import org.matsim.replanning.selectors.BestPlanSelector;
import org.matsim.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.replanning.selectors.KeepSelected;
import org.matsim.replanning.selectors.PathSizeLogitSelector;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;

/**
 * Loads the strategy modules specified in the config-file. This class offers
 * backwards-compatibility to the old StrategyManager where the complete class-
 * names were given in the configuration.
 *
 * @author mrieser
 */
public class StrategyManagerConfigLoader {

	/**
	 * Reads and instantiates the strategy modules specified in the config-object.
	 *
	 * @param config the configuration specifying which strategies should to be loaded
	 * @param manager the strategy-manager the instantiated strategies will be added to
	 * @param network the network strategy modules can use
	 * @param travelCostCalc the travel cost calculator strategy modules can make use of
	 * @param travelTimeCalc the travel time calculator strategy modules can make use of
	 */
	public static void load(final Config config, final StrategyManager manager, final NetworkLayer network, final TravelCostI travelCostCalc, final TravelTimeI travelTimeCalc) {

		String maxvalue = config.findParam("strategy", "maxAgentPlanMemorySize");
		if (maxvalue != null){
			manager.setMaxPlansPerAgent(Integer.parseInt(maxvalue));
		}

		int externalCounter = 0;
		int i = 0;
		while (true) {
			i++;
			String modrate = "ModuleProbability_" + i;
			String ratevalue = config.findParam("strategy", modrate);
			if (ratevalue == null) break;
			double rate = Double.parseDouble(ratevalue);
			if (rate == 0.0) continue;
			String modname = "Module_" + i;
			String classname = config.getParam("strategy", modname);
			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
			}
			PlanStrategy strategy = null;
			if (classname.equals("KeepLastSelected")) {
				strategy = new PlanStrategy(new KeepSelected());
			} else if (classname.equals("ReRoute") || classname.equals("threaded.ReRoute")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new ReRoute(network, travelCostCalc, travelTimeCalc));
			} else if (classname.equals("ReRoute_Landmarks")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
				preProcessRoutingData.run(network);
				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));
			} else if (classname.equals("TimeAllocationMutator") || classname.equals("threaded.TimeAllocationMutator")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new TimeAllocationMutator());
			}else if (classname.equals("TimeAllocationMutator7200_ReRouteLandmarks") || classname.equals("threaded.TimeAllocationMutator")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new TimeAllocationMutator(7200));
				PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
				preProcessRoutingData.run(network);
				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));
			} else if (classname.equals("ExternalModule")) {
				externalCounter++;
				strategy = new PlanStrategy(new RandomPlanSelector());
				String exePath = config.getParam("strategy", "ModuleExePath_" + i);
				strategy.addStrategyModule(new ExternalModule(exePath, "ext" + externalCounter));
			} else if (classname.equals("PlanomatExe")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				String exePath = config.getParam("strategy", "ModuleExePath_" + i);
				strategy.addStrategyModule(new PlanomatExe(exePath));
				strategy.addStrategyModule(new ReRoute(network, travelCostCalc, travelTimeCalc));
			} else if (classname.equals("PlanomatTimeRouteExe")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				String exePath = config.getParam("strategy", "ModuleExePath_" + i);
				strategy.addStrategyModule(new PlanomatExe(exePath));
			} else if (classname.equals("BestScore")) {
				strategy = new PlanStrategy(new BestPlanSelector());
			} else if (classname.equals("SelectExpBeta")) {
				strategy = new PlanStrategy(new ExpBetaPlanSelector());
			} else if (classname.equals("SelectRandom")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
			} else if (classname.equals("SelectPathSizeLogit")) {
				strategy = new PlanStrategy(new PathSizeLogitSelector());
			}


			if (strategy == null) {
				Gbl.errorMsg("Could not initialize strategy named " + classname);
			}

			manager.addStrategy(strategy, rate);

			// now check if this modules should be disabled after some iterations
			String moditer = "ModuleDisableAfterIteration_" + i;
			String itervalue = config.findParam("strategy", moditer);
			if (itervalue != null) {
				int maxIter = Integer.MAX_VALUE;
				maxIter = Integer.parseInt(itervalue);
				if (maxIter >= config.controler().getFirstIteration()) {
					manager.addChangeRequest(maxIter + 1, strategy, 0.0);
				} else {
					/* The controler starts at a later iteration than this change request is scheduled for.
					 * make the change right now.					 */
					manager.changeStrategy(strategy, 0.0);
				}
			}
		}
	}

}
