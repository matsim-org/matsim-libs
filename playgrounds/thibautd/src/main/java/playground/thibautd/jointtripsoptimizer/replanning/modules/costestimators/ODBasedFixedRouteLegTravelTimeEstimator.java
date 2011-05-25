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
import org.matsim.core.population.routes.AbstractRoute;
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
 * For all existing ODs in the initial plans, the first found route is used.
 * For newly created OD (by toggling joint trips for example), the {@link
 * PlansCalcRoute} instance passed to controler is used as a router.
 *
 * This class assumes no time-dependance of travel times for non-car modes, as:
 * <ul>
 * <li> this is the case with the default routers, if we trust the documentation.
 * <u>however</u>, evidences show that Time-dependant tt is sometimes used (for
 * example in PseudoTransit).</li>
 * </li> the way {@link PlansCalcRoute} is implemented for those modes is very
 * expensive: basically, it computes the least-cost path each time it is asked
 * to handle such a mode. To handle such a mode with time dependance, {@link
 * TravelTime} should be implemented for them, and this class modified to treat
 * them as it treats car.
 * </ul>
 *
 * This allows to take "toggle" decoding into account in a "natural" way.
 *
 * @author thibautd
 */
public class ODBasedFixedRouteLegTravelTimeEstimator implements LegTravelTimeEstimator {

	private static final double FALSE_NOW = 12*3600d;

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

		this.initPlanSpecificInformation();
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
		LegImpl newLeg = null;
		Tuple<Id, Id> odTuple = getOdTuple(actOrigin, actDestination);
		Map<String, LegImpl> legInformation = this.fixedRoutes.get(odTuple);

		if (legInformation == null) {
			legInformation = new HashMap<String, LegImpl>();
			this.fixedRoutes.put(odTuple, legInformation);
		}

		if (legInformation.containsKey(mode)) {
			newLeg = legInformation.get(mode);
		}
		else {
			newLeg = new LegImpl(mode);

			createRoute(mode, actOrigin, newLeg, actDestination);

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

		return new LegImpl(newLeg);
	}

	@Override
	//TODO: refactoring: way too long to be maintanable
	public double getLegTravelTimeEstimation(
			final Id personId,
			final double departureTime,
			final Activity actOrigin,
			final Activity actDestination,
			final Leg legIntermediate,
			final boolean doModifyLeg) {
		double legTravelTimeEstimation = 0.0;
		Tuple<Id, Id> odTuple = getOdTuple(actOrigin, actDestination);
		String mode = legIntermediate.getMode();

		if (mode.equals(TransportMode.car)) {
			double now = departureTime;
			now = this.processDeparture(actOrigin.getLinkId(), now);
			Map<String, LegImpl> odInformation = this.fixedRoutes.get(odTuple);
			NetworkRoute route = (NetworkRoute) getFixedRoute(odTuple, actOrigin, actDestination, mode);
			NetworkRoute networkRoute;

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
			Route route = getFixedRoute(odTuple, actOrigin, actDestination, mode);
			// XXX do not actually sets the route for the leg!
			legTravelTimeEstimation = route.getTravelTime();
			Route legRoute = legIntermediate.getRoute();
			legRoute.setDistance(route.getDistance());
		}

		return legTravelTimeEstimation;
	}

	private Route getFixedRoute(
			final Tuple<Id, Id> odTuple,
			final Activity actOrigin,
			final Activity actDestination,
			final String mode) {
		Map<String, LegImpl> odInformation = this.fixedRoutes.get(odTuple);
		LegImpl infoLeg;

		if (odInformation == null) {
			odInformation = new HashMap<String, LegImpl>();
			infoLeg = new LegImpl(mode);
			createRoute(mode, actOrigin, infoLeg, actDestination);
			odInformation.put(mode, infoLeg);
			this.fixedRoutes.put(odTuple, odInformation);
		}
		else {
			infoLeg = odInformation.get(mode);
			
			if (infoLeg == null) {
				infoLeg = new LegImpl(mode);
				createRoute(mode, actOrigin, infoLeg, actDestination);
				odInformation.put(mode, infoLeg);
			}
		}

		return infoLeg.getRoute();
	}

	private void createRoute(
			final String mode,
			final Activity actOrigin,
			final LegImpl infoLeg,
			final Activity actDestination) {
		if (mode.equals(TransportMode.car)) {
			Link startLink =
				this.network.getLinks().get(actOrigin.getLinkId());
			Link endLink =
				this.network.getLinks().get(actDestination.getLinkId());
			NetworkRoute route = (NetworkRoute) 
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

			route.setLinkIds(
					startLink.getId(),
					NetworkUtils.getLinkIds(path.links),
					endLink.getId());
			infoLeg.setRoute(route);
		}
		else {
			double travelTimeEst = this.plansCalcRoute.handleLeg(
					this.plan.getPerson(),
					infoLeg,
					actOrigin,
					actDestination,
					FALSE_NOW);
			// just to be sure...
			infoLeg.getRoute().setTravelTime(travelTimeEst);
		}
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

	/**
	 * Stores the routes of the plan.
	 * That is, the router will only be used for newly created ODs.
	 */
	private void initPlanSpecificInformation() {
		if (this.plan != null) {
			Map<String, LegImpl> odInformation;
			Tuple<Id, Id> odTuple;
			Route route;
			for (PlanElement planElement : this.plan.getPlanElements()) {
				if (planElement instanceof LegImpl) {
					LegImpl leg = (LegImpl) planElement;
					// TODO this should be possible for all types of routes.
					// Then we could cache e.g. the original pt routes, too.
					// however, LegImpl cloning constructor does not yet handle
					// generic routes correctly
					route = leg.getRoute();
					if (route instanceof NetworkRoute) {
						odTuple = getOdTuple(route);
						odInformation = this.fixedRoutes.get(odTuple);

						if (odInformation == null) {
							// create information map for this od
							odInformation = new HashMap<String, LegImpl>();
							odInformation.put(leg.getMode(), new LegImpl(leg));
							this.fixedRoutes.put(
									odTuple,
									odInformation);
						}
						else if (!odInformation.containsKey(leg.getMode())) {
							// update information map for this OD
							odInformation.put(leg.getMode(), new LegImpl(leg));
						}
					}
				}
			}
		}
	}

	private Tuple<Id, Id> getOdTuple(
			final Activity actOrigin,
			final Activity actDestination) {
		return new Tuple<Id, Id>(
				actOrigin.getLinkId(),
				actDestination.getLinkId());
	}


	private Tuple<Id, Id> getOdTuple(final Route route) {
		return new Tuple<Id, Id>(
				route.getStartLinkId(),
				route.getEndLinkId());
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}

