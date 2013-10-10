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

package playground.vsp.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;

public class PersonPrepareForSimTask implements ScenarioSinkSource {

	private static final int NUMBER_OF_THREADS = 0;
	
	private ScenarioSink sink;
	
	private LeastCostPathCalculatorFactory routerFactory;
	
	private TravelDisutility travelCosts;
	
	private TravelTime travelTimes;

	@Override
	public void initialize(Scenario scenario) {
		this.sink.initialize(scenario);
	}

	@Override
	public void process(final Scenario scenario) {
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), NUMBER_OF_THREADS,
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {

			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				final TripRouterFactoryBuilderWithDefaults builder =
					new TripRouterFactoryBuilderWithDefaults();
				builder.setLeastCostPathCalculatorFactory(
					routerFactory );
				return new PersonPrepareForSim(
					new PlanRouter(
						builder.build(
							scenario ).instantiateAndConfigureTripRouter(
								new RoutingContextImpl(
									travelCosts,
									travelTimes ) ) ),
					scenario.getNetwork());
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
