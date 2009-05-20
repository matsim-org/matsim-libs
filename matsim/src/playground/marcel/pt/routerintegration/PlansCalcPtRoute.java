/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcPtRoute.java
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

package playground.marcel.pt.routerintegration;

import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Plan;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.mmoyo.TransitSimulation.SimplifyPtLegs;

public class PlansCalcPtRoute extends PlansCalcRoute {

	private final SimplifyPtLegs planSimplifier;
	
	public PlansCalcPtRoute(Network network, TravelCost costCalculator, TravelTime timeCalculator,
			LeastCostPathCalculatorFactory factory) {
		super(network, costCalculator, timeCalculator, factory);
		this.planSimplifier = new SimplifyPtLegs((NetworkLayer) network, null);
	}

	@Override
	public void handlePlan(Plan plan) {
		
		super.handlePlan(plan);
	}

}
