/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoiceStrategyManagerConfigLoader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.ReRouteDijkstra;
import org.matsim.core.replanning.modules.ReRouteLandmarks;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;

import playground.telaviv.locationchoice.LocationChoicePlanModule;

public class LocationChoiceStrategyManagerConfigLoader extends StrategyManagerConfigLoader {

	/**
	 * Reads and instantiates the strategy modules specified in the config-object.
	 *
	 * @param controler the {@link Controler} that provides miscellaneous data for the replanning modules
	 * @param manager the {@link StrategyManager} to be configured according to the configuration
	 */
	public static void load(final Controler controler, final StrategyManager manager) {
		Config config = controler.getConfig();
		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());

		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
			double rate = settings.getProbability();
			if (rate == 0.0) {
				continue;
			}
			String classname = settings.getModuleName();

			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
			}

			PlanStrategy strategy = loadStrategy(controler, classname, settings);

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
					manager.changeWeightOfStrategy(strategy, 0.0);
				}
			}
		}
	}

	protected static PlanStrategy loadStrategy(final Controler controler, final String name, final StrategyConfigGroup.StrategySettings settings)
	{
		PlanStrategy strategy = null;
		
		Network network = controler.getNetwork();
		PersonalizableTravelCost travelCostCalc = controler.createTravelCostCalculator();
		PersonalizableTravelTime travelTimeCalc = controler.getTravelTimeCalculator();
		Config config = controler.getConfig();
		Scenario scenario = controler.getScenario();
		
	if (name.equals("LocationChoice_ReRoute")) {
		strategy = new PlanStrategy(new RandomPlanSelector());
		strategy.addStrategyModule(new LocationChoicePlanModule(scenario));
		strategy.addStrategyModule(new ReRoute(controler));
	} else if (name.equals("LocationChoice_ReRoute_Dijkstra")) {
		strategy = new PlanStrategy(new RandomPlanSelector());
		strategy.addStrategyModule(new LocationChoicePlanModule(scenario));
		strategy.addStrategyModule(new ReRouteDijkstra(config, network, travelCostCalc, travelTimeCalc));
	} else if (name.equals("LocationChoice_ReRoute_Landmarks")) {
		strategy = new PlanStrategy(new RandomPlanSelector());
		strategy.addStrategyModule(new LocationChoicePlanModule(scenario));
		strategy.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeCost(config.charyparNagelScoring())));		
	} else {
		strategy = StrategyManagerConfigLoader.loadStrategy(controler, name, settings);
	}
		return strategy;
	}}
