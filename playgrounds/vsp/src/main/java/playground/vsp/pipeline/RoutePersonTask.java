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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.population.algorithms.PersonPrepareForSim;

public class RoutePersonTask implements PersonSinkSource {

	private PersonPrepareForSim personPrepareForSim;
	private PersonSink sink;

	@Override
	public void setSink(PersonSink sink) {
		this.sink = sink;
	}

	@Override
	public void process(Person person) {
		personPrepareForSim.run(person);
		sink.process(person);
	}

	public RoutePersonTask( final Scenario scenario ) {
		super();
		TravelTime travelTimes =
			new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(
					scenario.getNetwork(),
					scenario.getConfig().travelTimeCalculator()).getLinkTravelTimes();
		TravelDisutility travelCosts =
			new TravelTimeAndDistanceBasedTravelDisutilityFactory().createTravelDisutility(
					travelTimes,
					scenario.getConfig().planCalcScore());
		personPrepareForSim =
			new PersonPrepareForSim(
					new PlanRouter(
						new TripRouterFactoryBuilderWithDefaults().build(
							scenario ).instantiateAndConfigureTripRouter(
								new RoutingContextImpl(
									travelCosts,
									travelTimes ) ) ),
					scenario.getNetwork());
	}

	@Override
	public void complete() {
		sink.complete();
	}
	
}
