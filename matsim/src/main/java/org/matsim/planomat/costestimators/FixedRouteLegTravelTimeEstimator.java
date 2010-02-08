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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;

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
	private final Network network;

	protected FixedRouteLegTravelTimeEstimator(
			Plan plan,
			TravelTime linkTravelTimeEstimator,
			DepartureDelayAverageCalculator depDelayCalc,
			PlansCalcRoute plansCalcRoute,
			PlanomatConfigGroup.SimLegInterpretation simLegInterpretation,
			Network network) {
		super(plan);
		this.linkTravelTimeEstimator = linkTravelTimeEstimator;
		this.tDepDelayCalc = depDelayCalc;
		this.plansCalcRoute = plansCalcRoute;
		this.simLegInterpretation = simLegInterpretation;
		this.network = network;

		this.initPlanSpecificInformation();

	}

	private HashMap<Integer, EnumMap<TransportMode, LegImpl>> fixedRoutes = new HashMap<Integer, EnumMap<TransportMode, LegImpl>>();

	@Override
	public LegImpl getNewLeg(
			TransportMode mode,
			ActivityImpl actOrigin,
			ActivityImpl actDestination,
			int legPlanElementIndex,
			double departureTime) {

		EnumMap<TransportMode, LegImpl> legInformation = null;
		if (this.fixedRoutes.containsKey(legPlanElementIndex)) {
			legInformation = this.fixedRoutes.get(legPlanElementIndex);
		} else {
			legInformation = new EnumMap<TransportMode, LegImpl>(TransportMode.class);
			this.fixedRoutes.put(legPlanElementIndex, legInformation);
		}

		LegImpl newLeg = null;
		if (legInformation.containsKey(mode)) {
			newLeg = legInformation.get(mode);
		} else {
			newLeg = new LegImpl(mode);

			if (mode.equals(TransportMode.car)) {
				Link startLink = this.network.getLinks().get(actOrigin.getLinkId());
				Link endLink = this.network.getLinks().get(actDestination.getLinkId());
				NetworkRouteWRefs newRoute = (NetworkRouteWRefs) this.plansCalcRoute.getRouteFactory().createRoute(TransportMode.car, startLink.getId(), endLink.getId());

				// calculate free speed route and cache it
				Path path = this.plansCalcRoute.getPtFreeflowLeastCostPathCalculator().calcLeastCostPath(
						startLink.getToNode(),
						endLink.getFromNode(),
						0.0);

				newRoute.setLinkIds(startLink.getId(), NetworkUtils.getLinkIds(path.links), endLink.getId());
				newLeg.setRoute(newRoute);
			} else {
				this.plansCalcRoute.handleLeg(newLeg, actOrigin, actDestination, departureTime);
			}

			legInformation.put(mode, newLeg);
		}

		if (mode.equals(TransportMode.car)) {

			double now = departureTime;
			now = this.processDeparture(actOrigin.getLinkId(), now);

			NetworkRouteWRefs route = ((NetworkRouteWRefs) newLeg.getRoute());
			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				now = this.processRouteTravelTime(NetworkUtils.getLinks(this.network, route.getLinkIds()), now);
				now = this.processLink(this.network.getLinks().get(actDestination.getLinkId()), now);
			} else if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				now = this.processLink(this.network.getLinks().get(actOrigin.getLinkId()), now);
				now = this.processRouteTravelTime(NetworkUtils.getLinks(this.network, route.getLinkIds()), now);
			}

			newLeg.setTravelTime(now - departureTime);

		}

		return newLeg;
	}

	@Override
	public double getLegTravelTimeEstimation(Id personId, double departureTime,
			ActivityImpl actOrigin, ActivityImpl actDestination,
			LegImpl legIntermediate, boolean doModifyLeg) {

		double legTravelTimeEstimation = 0.0;

		int legIndex = ((PlanImpl) this.plan).getActLegIndex(legIntermediate);

		if (legIntermediate.getMode().equals(TransportMode.car)) {

			// if no fixed route is given, generate free speed route for that leg in a lazy manner
			if (!this.fixedRoutes.containsKey(legIndex)) {

				LegImpl newLeg = new LegImpl(TransportMode.car);
				Link startLink = this.network.getLinks().get(actOrigin.getLinkId());
				Link endLink = this.network.getLinks().get(actDestination.getLinkId());
				NetworkRouteWRefs newRoute = (NetworkRouteWRefs) this.plansCalcRoute.getRouteFactory().createRoute(TransportMode.car, startLink.getId(), endLink.getId());

				// calculate free speed route and cache it
				Path path = this.plansCalcRoute.getPtFreeflowLeastCostPathCalculator().calcLeastCostPath(
						startLink.getToNode(),
						endLink.getFromNode(),
						0.0);

				newRoute.setLinkIds(startLink.getId(), NetworkUtils.getLinkIds(path.links), endLink.getId());
				newLeg.setRoute(newRoute);

				EnumMap<TransportMode, LegImpl> legInformation = new EnumMap<TransportMode, LegImpl>(TransportMode.class);
				legInformation.put(legIntermediate.getMode(), newLeg);

				this.fixedRoutes.put(legIndex, legInformation);

			}

			double now = departureTime;
			now = this.processDeparture(actOrigin.getLinkId(), now);

			NetworkRouteWRefs route = ((NetworkRouteWRefs) this.fixedRoutes.get(legIndex).get(legIntermediate.getMode()).getRoute());
			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				now = this.processRouteTravelTime(NetworkUtils.getLinks(this.network, route.getLinkIds()), now);
				now = this.processLink(this.network.getLinks().get(actDestination.getLinkId()), now);
			} else if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				now = this.processLink(this.network.getLinks().get(actOrigin.getLinkId()), now);
				now = this.processRouteTravelTime(NetworkUtils.getLinks(this.network, route.getLinkIds()), now);
			}

			NetworkRouteWRefs networkRoute = (NetworkRouteWRefs) this.plansCalcRoute.getRouteFactory().createRoute(
					TransportMode.car,
					actOrigin.getLinkId(),
					actDestination.getLinkId());
			networkRoute.setLinkIds(actOrigin.getLinkId(), route.getLinkIds(), actDestination.getLinkId());
			legIntermediate.setRoute(networkRoute);

			legTravelTimeEstimation = now - departureTime;

		} else {

			legTravelTimeEstimation = this.plansCalcRoute.handleLeg(legIntermediate, actOrigin, actDestination, departureTime);

		}

		return legTravelTimeEstimation;

	}

	protected double processDeparture(final Id linkId, final double start) {

		double departureDelayEnd = start + this.tDepDelayCalc.getLinkDepartureDelay(linkId, start);
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

	private void initPlanSpecificInformation() {

		if (this.plan != null) {
			for (PlanElement planElement : this.plan.getPlanElements()) {
				if (planElement instanceof LegImpl) {
					LegImpl leg = (LegImpl) planElement;
					// TODO this should be possible for all types of routes. Then we could cache e.g. the original pt routes, too.
					// however, LegImpl cloning constructor does not yet handle generic routes correctly
					if (leg.getRoute() instanceof NetworkRouteWRefs) {
						EnumMap<TransportMode, LegImpl> legInformation = new EnumMap<TransportMode, LegImpl>(TransportMode.class);
						legInformation.put(leg.getMode(), new LegImpl(leg));
						this.fixedRoutes.put(((PlanImpl) this.plan).getActLegIndex(leg), legInformation);
					}
				}
			}
		}

	}

}
