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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.population.Leg;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.transitSchedule.api.TransitSchedule;

import playground.marcel.pt.config.TransitConfigGroup;
import playground.marcel.pt.router.TransitRouter;
import playground.marcel.pt.router.TransitRouterConfig;
import playground.marcel.pt.routes.ExperimentalTransitRoute;

/**
 * @author mrieser
 */
public class PlansCalcTransitRoute extends PlansCalcRoute {

	private final TransitLegsRemover transitLegsRemover = new TransitLegsRemover();
	private final TransitRouterConfig routerConfig = new TransitRouterConfig();
	private final TransitRouter transitRouter;
	private final TransitConfigGroup transitConfig;
	private final TransitSchedule schedule;

	private PlanImpl currentPlan = null;
	private final List<Tuple<Leg, List<Leg>>> legReplacements = new LinkedList<Tuple<Leg, List<Leg>>>();

	public PlansCalcTransitRoute(final PlansCalcRouteConfigGroup config, final NetworkLayer network,
			final TravelCost costCalculator, final TravelTime timeCalculator,
			final LeastCostPathCalculatorFactory factory, final TransitSchedule schedule,
			final TransitConfigGroup transitConfig) {
		super(config, network, costCalculator, timeCalculator, factory);

		this.schedule = schedule;
		this.transitConfig = transitConfig;
		this.transitRouter = new TransitRouter(schedule, this.routerConfig);
	}

	@Override
	public void handlePlan(final PlanImpl plan) {
//		System.out.println("Person: " + plan.getPerson().getId());
		this.transitLegsRemover.run(plan);
		this.currentPlan = plan;
		this.legReplacements.clear();
		super.handlePlan(plan);
		this.replaceLegs();
		this.currentPlan = null;

	}

	@Override
	public double handleLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		if (this.transitConfig.getTransitModes().contains(leg.getMode())) {
			return this.handlePtPlan(leg, fromAct, toAct, depTime);
		}
		return super.handleLeg(leg, fromAct, toAct, depTime);
	}

	private double handlePtPlan(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		List<Leg> legs= this.transitRouter.calcRoute(fromAct.getCoord(), toAct.getCoord(), depTime);
		this.legReplacements.add(new Tuple<Leg, List<Leg>>(leg, legs));

		double travelTime = 0.0;
		if (legs != null) {
			for (Leg leg2 : legs) {
				travelTime += leg2.getTravelTime();
			}
		}
		return travelTime;
	}

	private void replaceLegs() {
		Iterator<Tuple<Leg, List<Leg>>> replacementIterator = this.legReplacements.iterator();
		if (!replacementIterator.hasNext()) {
			return;
		}
		List<PlanElement> planElements = this.currentPlan.getPlanElements();
		Tuple<Leg, List<Leg>> currentTuple = replacementIterator.next();
		for (int i = 0; i < this.currentPlan.getPlanElements().size(); i++) {
			PlanElement pe = planElements.get(i);
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (leg == currentTuple.getFirst()) {
					// do the replacement
					if (currentTuple.getSecond() != null) {
						// first and last leg do not have the route set, as the start or end  link is unknown.
						Leg firstLeg = currentTuple.getSecond().get(0);
						Link fromLink = ((ActivityImpl) planElements.get(i-1)).getLink();
						Link toLink = null;
						if (currentTuple.getSecond().size() > 1) { // at least one pt leg available
							toLink = ((RouteWRefs) currentTuple.getSecond().get(1).getRoute()).getStartLink();
						} else {
							toLink = ((ActivityImpl) planElements.get(i+1)).getLink();
						}
						firstLeg.setRoute(new GenericRouteImpl(fromLink, toLink));

						Leg lastLeg = currentTuple.getSecond().get(currentTuple.getSecond().size() - 1);
						toLink = ((ActivityImpl) planElements.get(i+1)).getLink();
						if (currentTuple.getSecond().size() > 1) { // at least one pt leg available
							fromLink = ((RouteWRefs) currentTuple.getSecond().get(currentTuple.getSecond().size() - 2).getRoute()).getStartLink();
						}
						lastLeg.setRoute(new GenericRouteImpl(fromLink, toLink));

						boolean isFirstLeg = true;
						Coord nextCoord = null;
						for (Leg leg2 : currentTuple.getSecond()) {
							if (isFirstLeg) {
								planElements.set(i, leg2);
								isFirstLeg = false;
							} else {
								i++;
								if (leg2.getRoute() instanceof ExperimentalTransitRoute) {
									ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) leg2.getRoute();
									planElements.add(i, new ActivityImpl("pt interaction", this.schedule.getFacilities().get(tRoute.getAccessStopId()).getCoord(), (LinkImpl) tRoute.getStartLink()));
									nextCoord = this.schedule.getFacilities().get(tRoute.getEgressStopId()).getCoord();
								} else { // walk legs don't have a coord, use the coord from the last egress point
									planElements.add(i, new ActivityImpl("pt interaction", nextCoord, (LinkImpl) ((RouteWRefs) leg2.getRoute()).getStartLink()));
								}
								i++;
								planElements.add(i, leg2);
							}
						}
					}
					if (!replacementIterator.hasNext()) {
						return;
					}
					currentTuple = replacementIterator.next();
				}
			}
		}

	}

}
