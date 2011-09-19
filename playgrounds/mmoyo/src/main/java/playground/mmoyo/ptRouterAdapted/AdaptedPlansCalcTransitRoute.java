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

package playground.mmoyo.ptRouterAdapted;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class AdaptedPlansCalcTransitRoute extends PlansCalcTransitRoute {
	private static final Logger log = Logger.getLogger(AdaptedPlansCalcTransitRoute.class);

	//private final TransitActsRemover transitLegsRemover = new TransitActsRemover();
	//private final AdaptedTransitRouter adaptedTransitRouter;

	//private Plan currentPlan = null;
	//private final List<Tuple<Leg, List<Leg>>> legReplacements = new LinkedList<Tuple<Leg, List<Leg>>>();

	public AdaptedPlansCalcTransitRoute(final PlansCalcRouteConfigGroup config, final Network network,
			final PersonalizableTravelCost costCalculator, final PersonalizableTravelTime timeCalculator,
			final LeastCostPathCalculatorFactory factory, final ModeRouteFactory routeFactory, final TransitSchedule schedule,
			final TransitConfigGroup transitConfig, MyTransitRouterConfig myTransitRouterConfig) {
		//super(config, network, costCalculator, timeCalculator, factory);
		super(config, network, costCalculator, timeCalculator, factory, routeFactory, transitConfig, new AdaptedTransitRouter(myTransitRouterConfig, schedule));

		//this.adaptedTransitRouter = new AdaptedTransitRouter( myTransitRouterConfig, schedule);

//		// both "super" route algos are made to route only on the "car" network.  yy I assume this is since the non-car modes are handled
//		// here.  This is, in fact, NOT correct, since this does not handle bike, so would need to be fixed before production use).  kai, apr'10
//		LeastCostPathCalculator routeAlgo = super.getLeastCostPathCalculator();
//		if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
//			((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(EnumSet.of(TransportMode.car));
//		}
//		routeAlgo = super.getPtFreeflowLeastCostPathCalculator();
//		if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
//			((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(EnumSet.of(TransportMode.car));
//		}
		// I don't think the above lines are needed (configuring the car router ... but this is already done in the
		// super constructor).  kai, apr'10
	}

	@Override
	protected double handlePtPlan(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime, final Person person) {
		// yyyy this is, in fact, a misnomer, since it handles a pt leg, not a pt plan.  kai, apr'10
		// yy also, I would like a design explanation of why there is a "TransitRouter" between "plansCalcRoute" and
		// "LeastCostPathAlgo"?  kai, apr'10
		// yyyy but first step really is to remove code duplication as much as possible.  kai, apr'10

		//List<Leg> legs= this.adaptedTransitRouter.calcRoute(fromAct.getCoord(), toAct.getCoord(), depTime);
		List<Leg> legs= ((AdaptedTransitRouter)this.getTransitRouter()).calcRoute(fromAct.getCoord(), toAct.getCoord(), depTime, person);

//		this.legReplacements.add(new Tuple<Leg, List<Leg>>(leg, legs));
		super.getLegReplacements().add(new Tuple<Leg, List<Leg>>(leg, legs));

		double travelTime = 0.0;
		if (legs != null) {
			for (Leg leg2 : legs) {
				travelTime += leg2.getTravelTime();
			}
		}
		return travelTime;
	}

	// no functionality beyond this line.

	/*
	@Override // necessary in order to remove the "pt interaction" activities and corresponding legs from earlier pt plans.
	public void handlePlan(Person person, final Plan plan) {

		// remove "intermediate" legs from plan
		this.transitLegsRemover.run(plan);

		// yyyy I think plan could be passed as argument to replaceLegs().  Please modify code accordingly, or explain why this is not possible. kai, apr'10
		this.currentPlan = plan;

		this.legReplacements.clear();

		super.handlePlan(person, plan);

		// yy Please explain somewhere what the legReplacements are doing. kai, apr'10
		// yy Please explain why this is done at the level here, and not inside calcRoute.  kai, apr'10
		// yy Please explain how Marcel's code can work without this (does it?). kai, apr'10
		this.replaceLegs();

		this.currentPlan = null;
	}


	@Override
	// yy In the long term, would be better to not override this. kai, apr'10
	public double handleLeg(Person person, final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		if (this.transitConfig.getTransitModes().contains(leg.getMode())) {
			return this.handlePtPlan(leg, fromAct, toAct, depTime);
		}
		return super.handleLeg(person, leg, fromAct, toAct, depTime);
	}
	 */

	/*
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
						Id fromLinkId = ((ActivityImpl) planElements.get(i-1)).getLinkId();
						Id toLinkId = null;
						if (currentTuple.getSecond().size() > 1) { // at least one pt leg available
							toLinkId = ((RouteWRefs) currentTuple.getSecond().get(1).getRoute()).getStartLinkId();
						} else {
							toLinkId = ((ActivityImpl) planElements.get(i+1)).getLinkId();
						}
						firstLeg.setRoute(new GenericRouteImpl(fromLinkId, toLinkId));

						Leg lastLeg = currentTuple.getSecond().get(currentTuple.getSecond().size() - 1);
						toLinkId = ((ActivityImpl) planElements.get(i+1)).getLinkId();
						if (currentTuple.getSecond().size() > 1) { // at least one pt leg available
							fromLinkId = ((RouteWRefs) currentTuple.getSecond().get(currentTuple.getSecond().size() - 2).getRoute()).getEndLinkId();
						}
						lastLeg.setRoute(new GenericRouteImpl(fromLinkId, toLinkId));

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
									ActivityImpl act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, this.schedule.getFacilities().get(tRoute.getAccessStopId()).getCoord(), tRoute.getStartLinkId());
									act.setDuration(0.0);
									planElements.add(i, act);
									nextCoord = this.schedule.getFacilities().get(tRoute.getEgressStopId()).getCoord();
								} else { // walk legs don't have a coord, use the coord from the last egress point
									ActivityImpl act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, nextCoord, leg2.getRoute().getStartLinkId());
									act.setDuration(0.0);
									planElements.add(i, act);
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
	}*/

}
