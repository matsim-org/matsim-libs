/* *********************************************************************** *
 * project: org.matsim.*
 * ReRoute.java
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

package playground.telaviv.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.old.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Uses {@link org.matsim.core.router.Dijkstra} for calculating the routes of plans during Replanning.
 *
 * @author mrieser
 */
public class ReRouteDijkstra extends AbstractMultithreadedModule {

	private Scenario scenario;

	public ReRouteDijkstra(Scenario scenario) {
		super(scenario.getConfig().global());
		this.scenario = scenario;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlansCalcRoute(
				scenario.getConfig().plansCalcRoute(), 
				scenario.getNetwork(), 
				getReplanningContext().getTravelCostCalculator(), 
				getReplanningContext().getTravelTimeCalculator(), 
				new DijkstraFactory(), 
				((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory());
	}

}
