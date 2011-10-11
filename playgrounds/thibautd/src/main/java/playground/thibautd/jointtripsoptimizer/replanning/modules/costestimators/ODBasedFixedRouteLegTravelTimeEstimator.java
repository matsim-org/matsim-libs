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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
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
	// reference to the internal network link map
	private final Map<Id, ? extends Link> networkLinks;
	private final FixedRoutes fixedRoutes = new FixedRoutes();

	/**
	 * Constructs an instance.
	 *
	 * Do not call directly: use {@link JointPlanOptimizerLegTravelTimeEstimatorFactory}
	 * instead.
	 */
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
		this.networkLinks = network.getLinks();

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
		RouteWrapper routeInformation = fixedRoutes.getRoute(mode, actOrigin, actDestination);
		Route route = routeInformation.getWrappedRoute();

		LegImpl newLeg = new LegImpl(mode);

		if (mode.equals(TransportMode.car)) {
			double now = departureTime;
			now = this.processDeparture(actOrigin.getLinkId(), now);

			if (simLegInterpretation.equals(
						PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				now = this.processRouteTravelTime(
						routeInformation.getLinks(),
						now);
				now = this.processLink(
						this.networkLinks.get(actDestination.getLinkId()),
						now);
			}
			else if (simLegInterpretation.equals(
						PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				now = this.processLink(
						this.networkLinks.get(actOrigin.getLinkId()),
						now);
				now = this.processRouteTravelTime(
						routeInformation.getLinks(),
						now);
			}

			newLeg.setTravelTime(now - departureTime);
		}
		else {
			newLeg.setTravelTime(route.getTravelTime());
		}

		newLeg.setRoute(route);

		return newLeg;
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
		String mode = legIntermediate.getMode();

		if (mode.equals(TransportMode.car)) {
			double now = departureTime;
			now = this.processDeparture(actOrigin.getLinkId(), now);
			RouteWrapper routeInformation = fixedRoutes.getRoute(mode, actOrigin, actDestination);
			NetworkRoute route = (NetworkRoute) routeInformation.getWrappedRoute();
			NetworkRoute networkRoute;

			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				now = this.processRouteTravelTime(
						routeInformation.getLinks(),
						now);
				now = this.processLink(
						this.networkLinks.get(actDestination.getLinkId()),
						now);
			}
			else if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				now = this.processLink(
						this.networkLinks.get(actOrigin.getLinkId()),
						now);
				now = this.processRouteTravelTime(
						routeInformation.getLinks(),
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
			networkRoute.setDistance(route.getDistance());

			if (doModifyLeg) {
				legIntermediate.setRoute(networkRoute);
			}

			legTravelTimeEstimation = now - departureTime;
		}
		else {
			Route route = fixedRoutes.getRoute(mode, actOrigin, actDestination).getWrappedRoute();
			legTravelTimeEstimation = route.getTravelTime();

			if (doModifyLeg) {
				//TODO: clone route
				legIntermediate.setRoute(route);
			}
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

	/**
	 * Stores the routes of the plan.
	 * That is, the router will only be used for newly created ODs.
	 */
	private void initPlanSpecificInformation() {

	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	// /////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////

	/**
	 * map-like class, storing route information related to triplets
	 * origin-destination-mode.
	 */
	private class FixedRoutes {
		private final Map<Tuple<Id, Id>, Map<String, RouteWrapper>> fixedRoutes = 
			new HashMap<Tuple<Id, Id>, Map<String, RouteWrapper>>();

		public RouteWrapper getRoute(
				final String mode,
				final Activity origin,
				final Activity destination) {
			RouteWrapper route = null;
			Tuple<Id, Id> od = getOdTuple(origin, destination);

			Map<String, RouteWrapper> routesByMode = fixedRoutes.get(od);

			if (routesByMode == null) {
				routesByMode = new HashMap<String, RouteWrapper>();
				route = createRoute(mode, origin, destination);
				routesByMode.put(mode, route);
				fixedRoutes.put(od, routesByMode);
			}
			else {
				route = routesByMode.get(mode);

				if (route == null) {
					route = createRoute(mode, origin, destination);
					routesByMode.put(mode, route);
				}
			}

			return route;
		}

		private RouteWrapper createRoute(
				final String mode,
				final Activity actOrigin,
				final Activity actDestination) {
			if (mode.equals(TransportMode.car)) {
				Link startLink =
					networkLinks.get(actOrigin.getLinkId());
				Link endLink =
					networkLinks.get(actDestination.getLinkId());
				NetworkRoute route = (NetworkRoute) 
					plansCalcRoute.getRouteFactory().createRoute(
							TransportMode.car,
							startLink.getId(),
							endLink.getId());

				// calculate free speed route and cache it
				Path path =
					plansCalcRoute.getPtFreeflowLeastCostPathCalculator().calcLeastCostPath(
							startLink.getToNode(),
							endLink.getFromNode(),
							0.0);

				route.setLinkIds(
						startLink.getId(),
						NetworkUtils.getLinkIds(path.links),
						endLink.getId());

				// set route distance
				double distance = RouteUtils.calcDistance(route, network);

				route.setDistance(distance);

				return new RouteWrapper(route);
			}
			else {
				Leg leg = new LegImpl(mode);
				double travelTimeEst = plansCalcRoute.handleLeg(
						plan.getPerson(),
						leg,
						actOrigin,
						actDestination,
						FALSE_NOW);
				// just to be sure...
				leg.getRoute().setTravelTime(travelTimeEst);
				return new RouteWrapper(leg.getRoute());
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

		public void initPlanSpecificInformation() {
			if (plan != null) {
				Map<String, RouteWrapper> odInformation;
				Tuple<Id, Id> odTuple;
				Route route;
				for (PlanElement planElement : plan.getPlanElements()) {
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
								odInformation = new HashMap<String, RouteWrapper>();
								odInformation.put(leg.getMode(), new RouteWrapper(leg.getRoute()));
								this.fixedRoutes.put(
										odTuple,
										odInformation);
							}
							else if (!odInformation.containsKey(leg.getMode())) {
								// update information map for this OD
								odInformation.put(leg.getMode(), new RouteWrapper(leg.getRoute()));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * wraps a route and provides access to a list of its links,
	 * without having to research them in the network every time
	 * it is needed.
	 */
	private class RouteWrapper {
		private final Route route;
		private List<Link> links = null;

		// /////////////////////////////////////////////////////////////////////
		// constructor
		// /////////////////////////////////////////////////////////////////////
		public RouteWrapper(
				final Route route) {
			this.route = route;
		}

		// /////////////////////////////////////////////////////////////////////
		// new methods
		// /////////////////////////////////////////////////////////////////////
		public List<Link> getLinks() {
			if (links == null) {
				links = NetworkUtils.getLinks(
						network,
						((NetworkRoute) route).getLinkIds());
			}

			return links;
		}

		public Route getWrappedRoute() {
			return route;
		}

		// /////////////////////////////////////////////////////////////////////
		// route methods
		// /////////////////////////////////////////////////////////////////////
		//@Override
		//public double getDistance() {
		//	return route.getDistance();
		//}

		//@Override
		//public Id getEndLinkId() {
		//	return route.getEndLinkId();
		//}

		//@Override
		//public Id getStartLinkId() {
		//	return route.getStartLinkId();
		//}

		//@Override
		//public double getTravelTime() {
		//	return route.getTravelTime();
		//}

		//@Override
		//public void setDistance(double arg0) {
		//	route.setDistance(arg0);
		//}

		//@Override
		//public void setTravelTime(double arg0) {
		//	route.setTravelTime(arg0);
		//}
	}
}

