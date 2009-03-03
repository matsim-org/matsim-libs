/* *********************************************************************** *
 * project: org.matsim.*
 * Template.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.modules.template;

import org.matsim.config.Module;
import org.matsim.controler.Controler;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.LeastCostPathCalculatorFactory;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

import playground.marcel.modules.MatsimModule;

public class Template implements MatsimModule {

	public void init(final Controler c) {
		
		// possible things that could get loaded with a module:
		
		
		
		// *** RoutingAlgorithm *********************
		
		LeastCostPathCalculatorFactory routerAlgoFactory = new LeastCostPathCalculatorFactory() {
			public LeastCostPathCalculator createPathCalculator(Network network, TravelCost travelCosts, TravelTime travelTimes) {
				return null;
			}
		};
		c.setLeastCostPathCalculatorFactory(routerAlgoFactory);
		// DISCUSS directly set the algo-factory, or just register to be loaded depending on config?
		// what about conflicts, if multiple modules try to set that?
		
		
		
		
		// *** ReplanningModule *********************
		
		PlanStrategy strategy = new PlanStrategy(new RandomPlanSelector());
		c.getStrategyManager().addStrategy(strategy, 0.5);
		// TODO needs improvement, not directly add to StrategyManager, but offer option to be loaded via config
		// register PlanStrategy, PlanSelector, StrategyModule?
		
		
		
		
		// *** ScoringFunction *********************
		
		ScoringFunctionFactory scoringFactory = new ScoringFunctionFactory() {
			public ScoringFunction getNewScoringFunction(Plan plan) {
				return null;
			}
		};
		c.setScoringFunctionFactory(scoringFactory);
		// DISCUSS directly set the sf-factory, or just register to be loaded depending on config?
		
		
		
		
		// *** ConfigGroup *********************
		
		Module templateModule = new Module("Template");
		c.getConfig().addModule("Template", templateModule); // problem: must be called before Controler is fully initialized
		
		
		
		
		// *** EventHandler *********************
		
		AgentArrivalEventHandler eventHandler = new AgentArrivalEventHandler() {
			public void handleEvent(AgentArrivalEvent event) {
			}
			public void reset(int iteration) {
			}
		};
		c.getEvents().addHandler(eventHandler); // problem: getEvents() may return null if not yet initialized
		
		
		
		
		// *** ControlerListener *********************
		
		StartupListener cListener = new StartupListener() {
			public void notifyStartup(StartupEvent event) {
				System.out.println("startup");
			}
		};
		c.addControlerListener(cListener);
		
		
		
		
		// *** Mobsim *********************
		
		// TODO
		
		
		
		// *** TravelCostCalculator *********************
		
		TravelCost travelCostCalculator = new TravelCost() {
			public double getLinkTravelCost(Link link, double time) {
				return 0;
			}
		};
		c.setTravelCostCalculator(travelCostCalculator);
		// DISCUSS directly set the calculator, or just register to be loaded depending on config?
		
		
		
		
		// *** TravelTimeCalculator *********************
		
//		TravelTime travelTimeCalculator = new TravelTime() {
//			public double getLinkTravelTime(Link link, double time) {
//				return 0;
//			}
//		};
		// TODO
//		c.setTravelTimeCalculator(travelTimeCalculator);
		
	}

}
