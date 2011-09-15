/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;

public class PersonPrepareForSimTask implements ScenarioSinkSource {

	private static final int NUMBER_OF_THREADS = 0;
	
	private ScenarioSink sink;
	
	private LeastCostPathCalculatorFactory routerFactory;
	
	private PersonalizableTravelCost travelCosts;
	
	private PersonalizableTravelTime travelTimes;

	@Override
	public void initialize(Scenario scenario) {
		this.sink.initialize(scenario);
	}

	@Override
	public void process(final Scenario scenario) {
		final ModeRouteFactory routeFactory = ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory();
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), NUMBER_OF_THREADS,
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {

			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), travelCosts, travelTimes, routerFactory, routeFactory), (NetworkImpl) scenario.getNetwork());
			}
		});
		sink.process(scenario);
	}

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

	public void setLeastCostPathCalculatorFactory(LeastCostPathCalculatorFactory routerFactory) {
		this.routerFactory = routerFactory;
	}
	
}
