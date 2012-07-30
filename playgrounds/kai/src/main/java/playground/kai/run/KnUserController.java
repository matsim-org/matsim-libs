/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.kai.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.LegRouter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

public class KnUserController {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Config config = null ;
		final Scenario sc = null ;
		final EventsManager eventsManager = null ;
		KnSimplifiedController ctrl = new KnSimplifiedController( /*sc*/ ) ;
		ctrl.setRoutingBuilder( new RoutingBuilder() {
			public PlanAlgorithm createRoutingAlgorithm() {

				// factory to generate routes:
				final ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (sc.getPopulation().getFactory())).getModeRouteFactory();

				final TravelTimeCalculator travelTime = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(sc.getNetwork(), config.travelTimeCalculator());
				eventsManager.addHandler(travelTime);

				// travel disutility (generalized cost)
				final TravelDisutility travelDisutility = new TravelTimeAndDistanceBasedTravelDisutility(travelTime, config.planCalcScore());

				// define the factory for the "computer science" router.  Needs to be a factory because it might be used multiple
				// times (e.g. for car router, pt router, ...)
				final LeastCostPathCalculatorFactory leastCostPathFactory = new DijkstraFactory();

				// plug it together
				final PlansCalcRoute plansCalcRoute = new PlansCalcRoute(config.plansCalcRoute(), sc.getNetwork(), travelDisutility, 
						travelTime, leastCostPathFactory, routeFactory);
				
				LegRouter legHandler = null;
				plansCalcRoute.addLegHandler("pt", legHandler) ;

				// return it:
				return plansCalcRoute;
				
			}
		}) ;
		
		
		
		ctrl.run();
	}

}
