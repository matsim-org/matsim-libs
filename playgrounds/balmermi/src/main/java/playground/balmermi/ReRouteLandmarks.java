/* *********************************************************************** *
 * project: org.matsim.*
 * ReRouteLandmarks.java
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

package playground.balmermi;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.old.PlansCalcRoute;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ReRouteLandmarks extends AbstractMultithreadedModule  {

	private AStarLandmarksFactory factory;
	private PlansCalcRouteConfigGroup configGroup = null;
	private Scenario scenario;

	public ReRouteLandmarks(Scenario scenario) {
		super(scenario.getConfig().global());
		this.scenario = scenario;
		this.configGroup = scenario.getConfig().plansCalcRoute();
		this.factory = new AStarLandmarksFactory(
				scenario.getNetwork(), 
				new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore()), 
				scenario.getConfig().global().getNumberOfThreads());	
	}
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlansCalcRoute(
				this.configGroup, 
				scenario.getNetwork(), 
				getReplanningContext().getTravelDisutility(), 
				getReplanningContext().getTravelTime(), 
				this.factory, 
				((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory());
	}

}
