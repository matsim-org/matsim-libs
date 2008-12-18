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

package playground.jhackney.replanning;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.modules.ExternalModule;
import org.matsim.replanning.modules.ReRouteDijkstra;
import org.matsim.replanning.modules.ReRouteLandmarks;
import org.matsim.replanning.modules.TimeAllocationMutator;
import org.matsim.replanning.selectors.BestPlanSelector;
import org.matsim.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.replanning.selectors.KeepSelected;
import org.matsim.replanning.selectors.PathSizeLogitSelector;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

/**
 * Loads the strategy modules specified in the config-file. This class offers
 * backwards-compatibility to the old StrategyManager where the complete class-
 * names were given in the configuration.
 *
 * @author mrieser
 */
public class SocialStrategyManagerConfigLoader {

	/**
	 * Reads and instantiates the strategy modules specified in the config-object.
	 *
	 * @param config the configuration specifying which strategies should to be loaded
	 * @param manager the strategy-manager the instantiated strategies will be added to
	 * @param network the network strategy modules can use
	 * @param travelCostCalc the travel cost calculator strategy modules can make use of
	 * @param travelTimeCalc the travel time calculator strategy modules can make use of
	 * @param legTravelTimeEstimator an estimator for travel times between two locations
	 */
	public static void load(final Config config, final StrategyManager manager, final NetworkLayer network,
			final TravelCost travelCostCalc, final TravelTime travelTimeCalc, final LegTravelTimeEstimator legTravelTimeEstimator) {

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
				strategy.addStrategyModule(new ReRouteDijkstra(network, travelCostCalc, travelTimeCalc));
			} else if (classname.equals("ReRoute_Landmarks")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
				preProcessRoutingData.run(network);
				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));
			} else if (classname.equals("TimeAllocationMutator") || classname.equals("threaded.TimeAllocationMutator")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new TimeAllocationMutator());
			} else if (classname.equals("TimeAllocationMutator7200_ReRouteLandmarks")) {
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
//			} else if (classname.equals("Planomat")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				StrategyModule planomatStrategyModule = new PlanomatOptimizeTimes(legTravelTimeEstimator);
//				strategy.addStrategyModule(planomatStrategyModule);
//				setDecayingModuleProbability(manager, strategy, 100, rate); // FIXME [KM] Why "100" and not controler.firstIteration as in "PlanomatReRoute"
//			} else if (classname.equals("PlanomatReRoute")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				StrategyModule planomatStrategyModule = new PlanomatOptimizeTimes(legTravelTimeEstimator);
//				strategy.addStrategyModule(planomatStrategyModule);
//				PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
//				preProcessRoutingData.run(network);
//				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));
//				setDecayingModuleProbability(manager, strategy, Gbl.getConfig().controler().getFirstIteration(), rate);
			} else if (classname.equals("BestScore")) {
				strategy = new PlanStrategy(new BestPlanSelector());
			} else if (classname.equals("SelectExpBeta")) {
				strategy = new PlanStrategy(new ExpBetaPlanSelector());
			} else if (classname.equals("ChangeExpBeta")) {
				strategy = new PlanStrategy(new ExpBetaPlanChanger());
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

	/**
	 * Adds several changeRequests to the StrategyManager such that the given PlanStrategy will be
	 * chosen with decaying probability over the iterations.
	 *
	 * @param manager
	 * @param strategy
	 * @param iterationStartDecay
	 * @param pReplanInit
	 *
	 * @author kmeister
	 * @author mrieser
	 */
	private static void setDecayingModuleProbability(final StrategyManager manager, final PlanStrategy strategy, final int iterationStartDecay, final double pReplanInit) {
		// Originally from PlanomatStrategyManagerConfigLoader
//		double pReplan = 0.0;

		// everything hard wired...

		double pReplanFinal = 0.0;
		int iterOffset = 0;
		double slope = 1.0;

		int controlerFirstIteration = Gbl.getConfig().controler().getFirstIteration();
		int controlerLastIteration = Gbl.getConfig().controler().getLastIteration();
		for (int iter = controlerFirstIteration; iter <= controlerLastIteration; iter++) {

//			old code:
//		// at first, use the typical replanning share from the config file
//			if (iter <= iterationStartDecay) {
//				pReplan = 0.1; // I think that should be pReplanInit. /marcel,18jan2008
//			} else {
//				// then use the decaying replanning share
//				pReplan = Math.min(pReplanInit, slope / (iter - iterationStartDecay + iterOffset) + pReplanFinal);
//			}
//
//			manager.addChangeRequest(iter, strategy, pReplan);

//			new code: TODO [KM] please check that this does the same as the old code above. /marcel,18jan2008
			if (iter > iterationStartDecay) {
				double pReplan = Math.min(pReplanInit, slope / (iter - iterationStartDecay + iterOffset) + pReplanFinal);
				manager.addChangeRequest(iter, strategy, pReplan);
			}

		}
	}

}

