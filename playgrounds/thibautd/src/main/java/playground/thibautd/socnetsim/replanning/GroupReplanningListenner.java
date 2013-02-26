/* *********************************************************************** *
 * project: org.matsim.*
 * GroupReplanningListenner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning;

import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.socnetsim.controller.ControllerRegistry;

/**
 * @author thibautd
 */
public class GroupReplanningListenner implements ReplanningListener {
	private final GroupStrategyManager strategyManager;
	private final ControllerRegistry registry;

	public GroupReplanningListenner(
			final ControllerRegistry registry,
			final GroupStrategyManager strategyManager) {
		this.registry = registry;
		this.strategyManager = strategyManager;
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		strategyManager.run(
				// this is not so nice, but the context returned by the event
				// delegates everything to Controler... And there is no clean way
				// to change that without touching to the core.
				new ReplanningContext() {
					@Override
					public TripRouterFactory getTripRouterFactory() {
						return registry.getTripRouterFactory();
					}

					@Override
					public TravelDisutility getTravelCostCalculator() {
						return registry.getTravelDisutilityFactory().createTravelDisutility(
							registry.getTravelTime().getLinkTravelTimes(),
							registry.getScenario().getConfig().planCalcScore() );
					}

					@Override
					public TravelTime getTravelTimeCalculator() {
						return registry.getTravelTime().getLinkTravelTimes();
					}

					@Override
					public ScoringFunctionFactory getScoringFunctionFactory() {
						return registry.getScoringFunctionFactory();
					}

					@Override
					public int getIteration() {
						return event.getIteration();
					}
				},
				registry.getJointPlans(),
				registry.getScenario().getPopulation() );
	}
}

