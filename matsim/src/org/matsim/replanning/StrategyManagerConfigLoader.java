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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.config.groups.StrategyConfigGroup;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.locationchoice.LocationChoice;
import org.matsim.network.NetworkLayer;
import org.matsim.replanning.modules.ChangeLegMode;
import org.matsim.replanning.modules.ExternalModule;
import org.matsim.replanning.modules.PlanomatModule;
import org.matsim.replanning.modules.ReRoute;
import org.matsim.replanning.modules.ReRouteDijkstra;
import org.matsim.replanning.modules.ReRouteLandmarks;
import org.matsim.replanning.modules.StrategyModule;
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
import org.matsim.socialnetworks.replanning.RandomFacilitySwitcherF;
import org.matsim.socialnetworks.replanning.RandomFacilitySwitcherK;
import org.matsim.socialnetworks.replanning.SNPickFacilityFromAlter;

/**
 * Loads the strategy modules specified in the config-file. This class offers
 * backwards-compatibility to the old StrategyManager where the complete class-
 * names were given in the configuration.
 *
 * @author mrieser
 */
public class StrategyManagerConfigLoader {

	private static final Logger log = Logger.getLogger(StrategyManagerConfigLoader.class);

	/**
	 * Reads and instantiates the strategy modules specified in the config-object.
	 *
	 * @param controler the {@link Controler} that provides miscellaneous data for the replanning modules
	 * @param config the {@link Config} object containing the configuration for the strategyManager
	 * @param manager the {@link StrategyManager} to be configured according to the configuration
	 */
	public static void load(final Controler controler, final Config config, final StrategyManager manager) {

		NetworkLayer network = controler.getNetwork();
		TravelCost travelCostCalc = controler.getTravelCostCalculator();
		TravelTime travelTimeCalc = controler.getTravelTimeCalculator();
		Facilities facilities = controler.getFacilities();

		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());

		int externalCounter = 0;

		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
			double rate = settings.getProbability();
			if (rate == 0.0) {
				continue;
			}
			String classname = settings.getModuleName();

			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
			}
			PlanStrategy strategy = null;
			if (classname.equals("KeepLastSelected")) {
				strategy = new PlanStrategy(new KeepSelected());
			} else if (classname.equals("ReRoute") || classname.equals("threaded.ReRoute")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new ReRoute(controler));
			} else if (classname.equals("ReRoute_Dijkstra")) {
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
				String exePath = settings.getExePath();
				strategy.addStrategyModule(new ExternalModule(exePath, "ext" + externalCounter));
			} else if (classname.equals("Planomat")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				StrategyModule planomatStrategyModule = new PlanomatModule(controler.getNetwork(), controler.getEvents(), controler.getTravelTimeCalculator(), controler.getTravelCostCalculator(), controler.getScoringFunctionFactory());
				strategy.addStrategyModule(planomatStrategyModule);
//				setDecayingModuleProbability(manager, strategy, 100, rate); // FIXME [KM] Why "100" and not controler.firstIteration as in "PlanomatReRoute"
			} else if (classname.equals("PlanomatReRoute")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				StrategyModule planomatStrategyModule = new PlanomatModule(controler.getNetwork(), controler.getEvents(), controler.getTravelTimeCalculator(), controler.getTravelCostCalculator(), controler.getScoringFunctionFactory());
				strategy.addStrategyModule(planomatStrategyModule);
				strategy.addStrategyModule(new ReRoute(controler));
				setDecayingModuleProbability(manager, strategy, Gbl.getConfig().controler().getFirstIteration(), rate);
			} else if (classname.equals("BestScore")) {
				strategy = new PlanStrategy(new BestPlanSelector());
			} else if (classname.equals("SelectExpBeta")) {
				strategy = new PlanStrategy(new ExpBetaPlanSelector());
			} else if (classname.equals("ChangeExpBeta")) {
				strategy = new PlanStrategy(new ExpBetaPlanChanger());
			} else if (classname.equals("SelectRandom")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
			} else if (classname.equals("ChangeLegMode")) {
				strategy = new PlanStrategy(new RandomPlanSelector());
				strategy.addStrategyModule(new ChangeLegMode(config));
				strategy.addStrategyModule(new ReRoute(controler));
			} else if (classname.equals("SelectPathSizeLogit")) {
				strategy = new PlanStrategy(new PathSizeLogitSelector());
				// JH
			} else if (classname.equals("KSecLoc")){
//				System.out.println(" #### Choosing social network replanning algorithm");
				strategy = new PlanStrategy(new RandomPlanSelector());
				StrategyModule socialNetStrategyModule= new RandomFacilitySwitcherK(network, travelCostCalc, travelTimeCalc);
				strategy.addStrategyModule(socialNetStrategyModule);
			} else if (classname.equals("FSecLoc")){
				strategy = new PlanStrategy(new RandomPlanSelector());
				StrategyModule socialNetStrategyModule= new RandomFacilitySwitcherF(network, travelCostCalc, travelTimeCalc, facilities);
				strategy.addStrategyModule(socialNetStrategyModule);
			} else if (classname.equals("SSecLoc")){
				strategy = new PlanStrategy(new RandomPlanSelector());
				StrategyModule socialNetStrategyModule= new SNPickFacilityFromAlter(network,travelCostCalc,travelTimeCalc);
				strategy.addStrategyModule(socialNetStrategyModule);
				// JH
			} else if (classname.equals("LocationChoice")) {
		    	strategy = new PlanStrategy(new ExpBetaPlanSelector());
		    	strategy.addStrategyModule(new LocationChoice(controler.getNetwork(), controler));
				final PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
				preProcessRoutingData.run(network);
				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));
				strategy.addStrategyModule(new TimeAllocationMutator());
			}
			//if none of the strategies above could be selected we try to load the class by name
			else {
				//classes loaded by name must not be part of the matsim core
				if (classname.startsWith("org.matsim")) {
					log.error("Strategies in the org.matsim package must not be loaded by name!");
				}
				else {
					try {
						Class[] args = {Controler.class};
						Class<? extends PlanStrategy> klas = (Class<? extends PlanStrategy>) Class.forName(classname);
						Constructor<? extends PlanStrategy> c = klas.getConstructor(args);
						strategy = c.newInstance(controler);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}

			if (strategy == null) {
				Gbl.errorMsg("Could not initialize strategy named " + classname);
			}

			manager.addStrategy(strategy, rate);

			// now check if this modules should be disabled after some iterations
			if (settings.getDisableAfter() >= 0) {
				int maxIter = settings.getDisableAfter();
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
