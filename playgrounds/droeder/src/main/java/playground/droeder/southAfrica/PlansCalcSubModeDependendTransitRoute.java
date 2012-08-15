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
package playground.droeder.southAfrica;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author droeder
 *
 */
public class PlansCalcSubModeDependendTransitRoute implements PlanAlgorithm {

	/**
	 * @param plansCalcRoute
	 * @param network
	 * @param travelCosts
	 * @param travelTimes
	 * @param leastCostPathCalculatorFactory
	 * @param routeFactory
	 * @param transit
	 * @param createTransitRouter
	 * @param transitSchedule
	 */
	public PlansCalcSubModeDependendTransitRoute(
			PlansCalcRouteConfigGroup plansCalcRoute, Network network,
			TravelDisutility travelCosts, PersonalizableTravelTime travelTimes,
			LeastCostPathCalculatorFactory leastCostPathCalculatorFactory,
			ModeRouteFactory routeFactory, TransitConfigGroup transit,
			TransitRouter createTransitRouter, TransitSchedule transitSchedule) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(Plan plan) {
		// TODO Auto-generated method stub

	}

}
