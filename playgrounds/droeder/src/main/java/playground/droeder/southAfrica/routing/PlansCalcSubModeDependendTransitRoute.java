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
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.droeder.southAfrica.replanning.FixedPtSubModePtInteractionRemover;
import playground.droeder.southAfrica.replanning.PlanStrategyReRoutePtFixedSubMode;

/**
 * @author droeder
 *
 */
public class PlansCalcSubModeDependendTransitRoute extends PlansCalcTransitRoute{
	private static final Logger log = Logger
			.getLogger(PlansCalcSubModeDependendTransitRoute.class);
	
	private FixedPtSubModePtInteractionRemover remover = new FixedPtSubModePtInteractionRemover();
	private PtSubModeDependendRouter router;
	private TransitConfigGroup config;

	/**
	 * @param config
	 * @param network
	 * @param costCalculator
	 * @param timeCalculator
	 * @param factory
	 * @param routeFactory
	 * @param transitConfig
	 * @param transitRouter
	 * @param transitSchedule
	 */
	public PlansCalcSubModeDependendTransitRoute(
			PlansCalcRouteConfigGroup config, Network network,
			TravelDisutility costCalculator,
			PersonalizableTravelTime timeCalculator,
			LeastCostPathCalculatorFactory factory,
			ModeRouteFactory routeFactory, TransitConfigGroup transitConfig,
			TransitRouter transitRouter, TransitSchedule transitSchedule) {
		super(config, network, costCalculator, timeCalculator, factory, routeFactory,
				transitConfig, transitRouter, transitSchedule);
		if(!(transitRouter instanceof PtSubModeDependendRouter)){
			throw new IllegalArgumentException("the transitRouter needs to be an instance of 'PtSubModeDependendRouter'. ABORT!");
		}
		this.router = (PtSubModeDependendRouter) transitRouter;
		this.config = transitConfig;
	}
	
	@Override
	protected void handlePlan(Person person, final Plan plan) {
		this.remover.run(plan);
		super.handlePlan(person, plan);
		/*
		 *  modify LegMode to original... should be moved to the 'remover' but needs to be called here,
		 *  because the a different 'remover'-class is called in super-class.
		 */
//		this.changeModesToOriginal(plan);
	}

//	/**
//	 * @param plan
//	 */
//	private void changeModesToOriginal(Plan plan) {
////		if(this.router.routeOnSameMode()) return;
//		//change legModes to original if 'routeOnSameMode' is not active
//		if(this.router.routeOnSameMode() &&
//				plan.getPerson().getCustomAttributes().containsKey(PlanStrategyReRoutePtFixedSubMode.ORIGINALLEGMODES)){
//			@SuppressWarnings("unchecked")
//			List<String> legModes = (List<String>) plan.getPerson().getCustomAttributes().
//					get(PlanStrategyReRoutePtFixedSubMode.ORIGINALLEGMODES);
//			
//			if(((legModes.size() * 2) + 1) != plan.getPlanElements().size()){
//				log.warn("Person " + plan.getPerson().getId() + " is probably no longer using original pt-subModes. " +
//						" Removing OriginalLegmodes...");
//				plan.getPerson().getCustomAttributes().remove(PlanStrategyReRoutePtFixedSubMode.ORIGINALLEGMODES);
//			}else{
//				//modify the planElements
//				for(int i = 1; i < plan.getPlanElements().size(); i += 2){
//					Leg l = (Leg) plan.getPlanElements().get(i);
//					String mode = legModes.get(i/2);
//					if(!l.getMode().equals(mode)){
//						log.info("Changing Legmode for person " + plan.getPerson().getId() + " from " + l.getMode() 
//								+ " to " + mode + ".");
//						l.setMode(mode);
//					}
//				}
//			}
//		}
//	}

	@Override
	public double handleLeg(Person person, final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		//use own method if leg is a transit-leg (not only pt)
		if (this.config.getTransitModes().contains(leg.getMode())) {
			List<Leg> legs= this.router.calcRoute(person, leg, fromAct, toAct, depTime);
			for(int i = 0; i < legs.size(); i++) {
				//not very nice, but legMode needs to be replaced here, because it's hardcoded in TransitRouterImpl... 
				if(!legs.get(i).getMode().equals(TransportMode.transit_walk)){
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
		}
		//otherwise use original
		return super.handleLeg(person, leg, fromAct, toAct, depTime);
	}
}
