/* *********************************************************************** *
 * project: org.matsim.*
 * ODBasedFixedRouteLegTravelTimeEstimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;

/**
 * Essentially the same as {@link
 * org.matsim.planomat.costestimators.FixedRouteLegTravelTimeEstimator},
 * except that the routes are associated to O/D pairs and not to plan elements.
 * This allows to take "toggle" decoding into account in a "natural" way.
 *
 * @author thibautd
 */
public class ODBasedFixedRouteLegTravelTimeEstimator implements LegTravelTimeEstimator {

	private final Plan plan;
	protected final TravelTime linkTravelTimeEstimator;
	protected final DepartureDelayAverageCalculator tDepDelayCalc;
	private final PlansCalcRoute plansCalcRoute;
	private final PlanomatConfigGroup.SimLegInterpretation simLegInterpretation;
	private final Network network;
	private Map<Tuple<Id, Id>, Map<String, LegImpl>> fixedRoutes = 
		new HashMap<Tuple<Id, Id>, Map<String, LegImpl>>();

	protected ODBasedFixedRouteLegTravelTimeEstimator(
			final Plan plan,
			final TravelTime linkTravelTimeEstimator,
			final DepartureDelayAverageCalculator depDelayCalc,
			final PlansCalcRoute plansCalcRoute,
			final PlanomatConfigGroup.SimLegInterpretation simLegInterpretation,
			final Network network) {
		this.plan = plan;
		this.linkTravelTimeEstimator = linkTravelTimeEstimator;
		this.tDepDelayCalc = depDelayCalc;
		this.plansCalcRoute = plansCalcRoute;
		this.simLegInterpretation = simLegInterpretation;
		this.network = network;

		//this.initPlanSpecificInformation();
	}

	/*
	 * =========================================================================
	 * public methods
	 * =========================================================================
	 */

	@Override
	public LegImpl getNewLeg(
			final String mode,
			final Activity actOrigin,
			final Activity actDestination,
			final int legPlanElementIndex,
			final double departureTime) {
		Map<String, LegImpl> legInformation = null;
		LegImpl newLeg = null;
		Tuple<Id, Id> ODTuple = getODTuple(actOrigin, actDestination);

		if (this.fixedRoutes.containsKey(ODTuple)) {
			legInformation = this.fixedRoutes.get(ODTuple);
		}
		else {
			legInformation = new HashMap<String, LegImpl>();
			this.fixedRoutes.put(ODTuple, legInformation);
		}

		if (legInformation.containsKey(mode)) {
			newLeg = legInformation.get(mode);
		}
		else {
			newLeg = new LegImpl(mode);

			if (mode.equals(TransportMode.car)) {
				Link startLink = this.network.getLinks().get(actOrigin.getLinkId());
				Link endLink = this.network.getLinks().get(actDestination.getLinkId());
				NetworkRoute newRoute = (NetworkRoute) 
					this.plansCalcRoute.getRouteFactory().createRoute(
							TransportMode.car,
							startLink.getId(),
							endLink.getId());

				// calculate free speed route and cache it
				Path path = 
					this.plansCalcRoute.getPtFreeflowLeastCostPathCalculator().calcLeastCostPath(
							startLink.getToNode(),
							endLink.getFromNode(),
							0.0);

				newRoute.setLinkIds(
						startLink.getId(),
						NetworkUtils.getLinkIds(path.links),
						endLink.getId());
				newLeg.setRoute(newRoute);
			}
			else {
				this.plansCalcRoute.handleLeg(
						this.plan.getPerson(),
						newLeg,
						actOrigin,
						actDestination,
						departureTime);
			}

			legInformation.put(mode, newLeg);
		}

		if (mode.equals(TransportMode.car)) {
			double now = departureTime;
			now = this.processDeparture(actOrigin.getLinkId(), now);

			NetworkRoute route = ((NetworkRoute) newLeg.getRoute());
			if (simLegInterpretation.equals(
						PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				now = this.processRouteTravelTime(
						NetworkUtils.getLinks(
							this.network,
							route.getLinkIds()),
						now);
				now = this.processLink(
						this.network.getLinks().get(actDestination.getLinkId()),
						now);
			}
			else if (simLegInterpretation.equals(
						PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				now = this.processLink(
						this.network.getLinks().get(actOrigin.getLinkId()),
						now);
				now = this.processRouteTravelTime(
						NetworkUtils.getLinks(this.network, route.getLinkIds()),
						now);
			}

			newLeg.setTravelTime(now - departureTime);
		}

		return newLeg;
	}

	@Override
	public double getLegTravelTimeEstimation(
			final Id personId,
			final double departureTime,
			final Activity actOrigin,
			final Activity actDestination,
			final Leg legIntermediate,
			final boolean doModifyLeg) {
		double legTravelTimeEstimation = 0.0;
		Tuple<Id, Id> ODTuple = getODTuple(actOrigin, actDestination);

		if (legIntermediate.getMode().equals(TransportMode.car)) {
			double now = departureTime;
			now = this.processDeparture(actOrigin.getLinkId(), now);
			NetworkRoute route;
			NetworkRoute networkRoute;

			// if no fixed route is given, generate free speed route for that 
			// leg in a lazy manner
			if (!this.fixedRoutes.containsKey(ODTuple)) {
				LegImpl newLeg = new LegImpl(TransportMode.car);
				Link startLink =
					this.network.getLinks().get(actOrigin.getLinkId());
				Link endLink =
					this.network.getLinks().get(actDestination.getLinkId());
				NetworkRoute newRoute = (NetworkRoute) 
					this.plansCalcRoute.getRouteFactory().createRoute(
							TransportMode.car,
							startLink.getId(),
							endLink.getId());

				// calculate free speed route and cache it
				Path path =
					this.plansCalcRoute.getPtFreeflowLeastCostPathCalculator().calcLeastCostPath(
							startLink.getToNode(),
							endLink.getFromNode(),
							0.0);

				newRoute.setLinkIds(
						startLink.getId(),
						NetworkUtils.getLinkIds(path.links),
						endLink.getId());
				newLeg.setRoute(newRoute);

				Map<String, LegImpl> legInformation = new HashMap<String, LegImpl>();
				legInformation.put(legIntermediate.getMode(), newLeg);

				this.fixedRoutes.put(ODTuple, legInformation);
			}

			route = ((NetworkRoute) this.fixedRoutes.get(ODTuple).get(
					legIntermediate.getMode()).getRoute());

			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				now = this.processRouteTravelTime(
						NetworkUtils.getLinks(this.network, route.getLinkIds()),
						now);
				now = this.processLink(
						this.network.getLinks().get(actDestination.getLinkId()),
						now);
			}
			else if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				now = this.processLink(
						this.network.getLinks().get(actOrigin.getLinkId()),
						now);
				now = this.processRouteTravelTime(
						NetworkUtils.getLinks(this.network, route.getLinkIds()),
						now);
			}

			networkRoute =
				(NetworkRoute) this.plansCalcRoute.getRouteFactory().createRoute(
						TransportMode.car,
						actOrigin.getLinkId(),
						actDestination.getLinkId());
			networkRoute.setLinkIds(
					actOrigin.getLinkId(),
					route.getLinkIds(),
					actDestination.getLinkId());
			legIntermediate.setRoute(networkRoute);

			legTravelTimeEstimation = now - departureTime;
		}
		else {
			legTravelTimeEstimation = this.plansCalcRoute.handleLeg(
					this.plan.getPerson(),
					legIntermediate,
					actOrigin,
					actDestination,
					departureTime);
		}

		return legTravelTimeEstimation;
	}

	/*
	 * =========================================================================
	 * helper methods
	 * =========================================================================
	 */

	/**
	 * @return departure delay estimation.
	 */
	protected double processDeparture(final Id linkId, final double start) {
		double departureDelayEnd =
			start + this.tDepDelayCalc.getLinkDepartureDelay(linkId, start);

		return departureDelayEnd;
	}

	/**
	 * @return travel time estimation.
	 */
	protected double processRouteTravelTime(final List<Link> route, final double start) {
		double now = start;

		for (Link link : route) {
			now = this.processLink(link, now);
		}

		return now;
	}

	/**
	 * @return link traversal time estimation.
	 */
	protected double processLink(final Link link, final double start) {
		double linkEnd =
			start + this.linkTravelTimeEstimator.getLinkTravelTime(link, start);

		return linkEnd;
	}

	private void initPlanSpecificInformation() {
		if (this.plan != null) {
			for (PlanElement planElement : this.plan.getPlanElements()) {
				if (planElement instanceof LegImpl) {
					LegImpl leg = (LegImpl) planElement;
					// TODO this should be possible for all types of routes.
					// Then we could cache e.g. the original pt routes, too.
					// however, LegImpl cloning constructor does not yet handle
					// generic routes correctly
					if (leg.getRoute() instanceof NetworkRoute) {
						Map<String, LegImpl> legInformation = new HashMap<String, LegImpl>();
						legInformation.put(leg.getMode(), new LegImpl(leg));
						this.fixedRoutes.put(
								getODTuple(leg.getRoute()),
								legInformation);
					}
				}
			}
		}
	}

	private Tuple<Id, Id> getODTuple(
			final Activity actOrigin,
			final Activity actDestination) {
		return new Tuple<Id, Id>(
				actOrigin.getLinkId(),
				actDestination.getLinkId());
	}


	private Tuple<Id, Id> getODTuple(final Route route) {
		return new Tuple<Id, Id>(
				route.getStartLinkId(),
				route.getEndLinkId());
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}

