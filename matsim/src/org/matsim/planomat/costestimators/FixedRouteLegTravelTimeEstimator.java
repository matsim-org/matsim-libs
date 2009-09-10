/* *********************************************************************** *
 * project: org.matsim.*
 * FixedRouteLegTravelTimeEstimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.planomat.costestimators;

import java.util.HashMap;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

/**
 * Implementation of <code>LegTravelTimeEstimator</code>
 * which estimates the travel time of a fixed route.
 *
 * @author meisterk
 *
 */
public class FixedRouteLegTravelTimeEstimator extends AbstractLegTravelTimeEstimator {

	protected final TravelTime linkTravelTimeEstimator;
	protected final DepartureDelayAverageCalculator tDepDelayCalc;
	private final PlansCalcRoute plansCalcRoute;
	private final PlanomatConfigGroup.SimLegInterpretation simLegInterpretation;

	protected FixedRouteLegTravelTimeEstimator(
			PlanImpl plan,
			TravelTime linkTravelTimeEstimator,
			DepartureDelayAverageCalculator depDelayCalc,
			PlansCalcRoute plansCalcRoute,
			PlanomatConfigGroup.SimLegInterpretation simLegInterpretation) {
		super(plan);
		this.linkTravelTimeEstimator = linkTravelTimeEstimator;
		this.tDepDelayCalc = depDelayCalc;
		this.plansCalcRoute = plansCalcRoute;
		this.simLegInterpretation = simLegInterpretation;

		if (plan != null) {
			this.initPlanSpecificInformation(plan);
		}
		
	}

	public LegImpl getNewLeg(
			TransportMode mode, 
			ActivityImpl actOrigin,
			ActivityImpl actDestination, 
			double departureTime) {
		// TODO Auto-generated method stub
		return null;
	}

	public double getLegTravelTimeEstimation(Id personId, double departureTime,
			ActivityImpl actOrigin, ActivityImpl actDestination,
			LegImpl legIntermediate, boolean doModifyLeg) {

		double legTravelTimeEstimation = 0.0;

		int legIndex = this.plan.getActLegIndex(legIntermediate);

		if (legIntermediate.getMode().equals(TransportMode.car)) {

			// if no fixed route is given, generate free speed route for that leg in a lazy manner
			if (!this.fixedRoutes.containsKey(legIndex)) {

				LegImpl newLeg = new LegImpl(TransportMode.car);
				Link startLink = actOrigin.getLink();
				Link endLink = actDestination.getLink();
				NetworkRouteWRefs newRoute = (NetworkRouteWRefs) this.plansCalcRoute.getRouteFactory().createRoute(TransportMode.car, startLink, endLink);

				// calculate free speed route and cache it
				Path path = this.plansCalcRoute.getPtFreeflowLeastCostPathCalculator().calcLeastCostPath(
						actOrigin.getLink().getToNode(), 
						actDestination.getLink().getFromNode(), 
						0.0);

				newRoute.setLinks(startLink, path.links, endLink);
				newLeg.setRoute(newRoute);

				HashMap<TransportMode, LegImpl> legInformation = new HashMap<TransportMode, LegImpl>();
				legInformation.put(legIntermediate.getMode(), newLeg);

				this.fixedRoutes.put(legIndex, legInformation);

			}

			double now = departureTime;
			now = this.processDeparture(actOrigin.getLink(), now);

			NetworkRouteWRefs route = ((NetworkRouteWRefs) this.fixedRoutes.get(legIndex).get(legIntermediate.getMode()).getRoute());
			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				now = this.processRouteTravelTime(route.getLinks(), now);
				now = this.processLink(actDestination.getLink(), now);
			} else if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				now = this.processLink(actOrigin.getLink(), now);
				now = this.processRouteTravelTime(route.getLinks(), now);
			}

			NetworkRouteWRefs networkRoute = (NetworkRouteWRefs) this.plansCalcRoute.getRouteFactory().createRoute(
					TransportMode.car, 
					actOrigin.getLink(), 
					actDestination.getLink());
			networkRoute.setLinks(actOrigin.getLink(), route.getLinks(), actDestination.getLink());
			legIntermediate.setRoute(networkRoute);

			legTravelTimeEstimation = now - departureTime;

		} else {

			legTravelTimeEstimation = this.plansCalcRoute.handleLeg(legIntermediate, actOrigin, actDestination, departureTime);

		}

		return legTravelTimeEstimation;

	}

	protected double processDeparture(final LinkImpl link, final double start) {

		double departureDelayEnd = start + this.tDepDelayCalc.getLinkDepartureDelay(link, start);
		return departureDelayEnd;

	}

	protected double processRouteTravelTime(final List<Link> route, final double start) {

		double now = start;

		for (Link link : route) {
			now = this.processLink(link, now);
		}
		return now;

	}

	protected double processLink(final Link link, final double start) {

		double linkEnd = start + this.linkTravelTimeEstimator.getLinkTravelTime(link, start);
		return linkEnd;

	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

//	public void resetPlanSpecificInformation() {
//		this.fixedRoutes.clear();
////		this.currentPlan = null;
//	}

	private HashMap<Integer, HashMap<TransportMode, LegImpl>> fixedRoutes = new HashMap<Integer, HashMap<TransportMode, LegImpl>>();
//	private PlanImpl currentPlan;

	private void initPlanSpecificInformation(PlanImpl plan) {

//		this.currentPlan = plan;

		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof LegImpl) {
				LegImpl leg = (LegImpl) planElement;
				if (leg.getRoute() instanceof NetworkRouteWRefs) {
					HashMap<TransportMode, LegImpl> legInformation = new HashMap<TransportMode, LegImpl>();
					legInformation.put(leg.getMode(), new LegImpl(leg));
					this.fixedRoutes.put(this.plan.getActLegIndex(leg), legInformation);
				}
			}
		}

	}

}
