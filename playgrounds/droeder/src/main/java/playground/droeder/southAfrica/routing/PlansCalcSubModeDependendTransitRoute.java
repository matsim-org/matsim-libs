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
package playground.droeder.southAfrica.routing;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.LegRouter;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.droeder.southAfrica.replanning.PtSubModePtInteractionRemover;

/**
 * @author droeder
 *
 */
public class PlansCalcSubModeDependendTransitRoute extends PlansCalcTransitRoute{
	
	
	private class PtSubModeLegHandler implements LegRouter {

		@Override
		public double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
			return handlePtPlan(person, leg, fromAct, toAct, depTime);
		}

	}

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(PlansCalcSubModeDependendTransitRoute.class);
	
	private PtSubModePtInteractionRemover remover = new PtSubModePtInteractionRemover();
	private PtSubModeDependendRouter router;

	/**
	 * @param config
	 * @param network
	 * @param costCalculator
	 * @param travelTimes
	 * @param factory
	 * @param routeFactory
	 * @param transitConfig
	 * @param transitRouter
	 * @param transitSchedule
	 */
	public PlansCalcSubModeDependendTransitRoute(
			PlansCalcRouteConfigGroup config, Network network,
			TravelDisutility costCalculator,
			TravelTime travelTimes,
			LeastCostPathCalculatorFactory factory,
			ModeRouteFactory routeFactory, TransitConfigGroup transitConfig,
			TransitRouter transitRouter, TransitSchedule transitSchedule) {
		super(config, network, costCalculator, travelTimes, factory, routeFactory,
				transitConfig, transitRouter, transitSchedule);
		if(!(transitRouter instanceof PtSubModeDependendRouter)){
			throw new IllegalArgumentException("the transitRouter needs to be an instance of 'PtSubModeDependendRouter'. ABORT!");
		}
		this.router = (PtSubModeDependendRouter) transitRouter;
		// add the default
		this.addLegHandler(TransportMode.pt, new PtSubModeLegHandler());
		// add all other modes (maybe pt again)
		for (String transitMode : transitConfig.getTransitModes()) {
			this.addLegHandler(transitMode, new PtSubModeLegHandler());
		}
	}
	
	@Override
	protected void handlePlan(Person person, final Plan plan) {
		this.remover.run(plan);
		super.handlePlan(person, plan);
	}

	protected double handlePtPlan(Person person, final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
//		//use own method if leg is a transit-leg (not only pt!)
//		if (this.config.getTransitModes().contains(leg.getMode())) {
			List<Leg> legs= this.router.calcRoute(person, leg, fromAct, toAct, depTime);
			for(int i = 0; i < legs.size(); i++) {
				//not very nice, but legMode needs to be replaced here, because TransportMode.pt is 'hardcoded' in TransitRouterImpl... 
				if(!legs.get(i).getMode().equals(TransportMode.transit_walk) && this.router.calculatedRouteForMode(leg.getMode())){
					legs.get(i).setMode(leg.getMode());
				}
			}
			super.getLegReplacements().add(new Tuple<Leg, List<Leg>>(leg, legs));
			double travelTime = 0.0;
			if (legs != null) {
				for (Leg leg2 : legs) {
					travelTime += leg2.getTravelTime();
				}
			}
			return travelTime;
//		}
//		//otherwise use original
//		return super.handleLeg(person, leg, fromAct, toAct, depTime);
	}
}
