/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.pseudosimulation.replanning.modules;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import playground.pieter.pseudosimulation.controler.listeners.MobSimSwitcher;

class PSimPlanRouter extends PlanRouter {

	public PSimPlanRouter(TripRouter routingHandler, ActivityFacilities facilities) {
		super(routingHandler, facilities);
		// TODO Auto-generated constructor stub
	}

	public PSimPlanRouter(TripRouter routingHandler) {
		super(routingHandler);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(Plan plan) {
		if (MobSimSwitcher.isQSimIteration)
			super.run(plan);

	}

}
