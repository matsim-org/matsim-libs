/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterWrapper.java
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
package org.matsim.core.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * Wraps a {@link TransitRouter}.
 *
 * @author thibautd
 */
public class TransitRouterWrapper implements RoutingModule {
	private final TransitRouter router;
	private final RoutingModule walkRouter;
	private final TransitSchedule transitSchedule;
	private final Network network;


	public TransitRouterWrapper(
			final TransitRouter router,
			final TransitSchedule transitSchedule,
			Network network, final RoutingModule walkRouter) {
		if (router == null) {
			throw new NullPointerException("The router object is null, but is required later.");
		}
		this.router = router;
		this.transitSchedule = transitSchedule;
		this.network = network;
		if (walkRouter == null) {
			throw new NullPointerException("The walkRouter object is null, but is required later.");
		}
		this.walkRouter = walkRouter;
	}

	/**
	 * Just links to {@link TransitRouter#calcRoute(Facility, Facility, double, Person)}.
	 *
	 * @return the list of legs returned by the transit router.
	 */
	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {
		List<? extends PlanElement> baseTrip = router.calcRoute(request);

		// the previous approach was to return null when no trip was found and
		// not to replace the trip if so.
		// However, this makes the output of routing modules more tricky to handle.
		// Thus, every module should return a valid trip. When available, the "main
		// mode" flag should be put to the mode of the routing module.
		return baseTrip != null ?
				fillWithActivities(baseTrip, request) :
					null;
	}

	/**
	 * This treats the TransitRouter as a third-party interface, where missing fields
	 * must be filled in (distance, travel-time in routes).
	 */
	private List<PlanElement> fillWithActivities(
			final List<? extends PlanElement> baseTrip, RoutingRequest request) {
		final Facility fromFacility = request.getFromFacility();
		final Facility toFacility = request.getToFacility();
		final double departureTime = request.getDepartureTime();
		final Person person = request.getPerson();
		
		List<PlanElement> trip = new ArrayList<>();
		Coord nextCoord = null;
		int i = 0;
		for (PlanElement pe : baseTrip) {
			Leg leg = (Leg)pe;
			if (i == 0) {
				// (access leg)
				Facility firstToFacility;
				if (baseTrip.size() > 1) { // at least one pt leg available
					TransitPassengerRoute tRoute = (TransitPassengerRoute) ((Leg)baseTrip.get(1)).getRoute();
					firstToFacility = this.transitSchedule.getFacilities().get(tRoute.getAccessStopId());
				} else {
					firstToFacility = toFacility;
				}
				// (*)
				Route route = createWalkRoute(fromFacility, departureTime, person,
						leg.getTravelTime().seconds(), firstToFacility, request.getAttributes());
				leg.setRoute(route);
			} else {
				if (leg.getRoute() instanceof TransitPassengerRoute) {
					TransitPassengerRoute tRoute = (TransitPassengerRoute) leg.getRoute();
					tRoute.setTravelTime(leg.getTravelTime().seconds());
					tRoute.setDistance(RouteUtils.calcDistance(tRoute, transitSchedule, network));
					Activity act = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(this.transitSchedule.getFacilities().get(tRoute.getAccessStopId()).getCoord(), tRoute.getStartLinkId(), TransportMode.pt);
					trip.add(act);
					nextCoord = this.transitSchedule.getFacilities().get(tRoute.getEgressStopId()).getCoord();
				} else { 
					// it is not an instance of an ExperimentalTransitRoute so it must be a (transit) walk leg.

					// walk legs don't have a coord, use the coord from the last egress point.  yyyy But I don't understand why in one case we take "nextCoord", while in the
					// other case we retrieve the facility from the previous route.

					if (i == baseTrip.size() - 1) {
						// if this is the last leg, we don't believe the leg from the TransitRouter.  Why?

						TransitPassengerRoute tRoute = (TransitPassengerRoute) ((Leg)baseTrip.get(baseTrip.size() - 2)).getRoute();
						Facility lastFromFacility = this.transitSchedule.getFacilities().get(tRoute.getEgressStopId());

						Route route = createWalkRoute(lastFromFacility, departureTime, person,
								leg.getTravelTime().seconds(), toFacility, request.getAttributes());
						leg.setRoute(route);
					}
					Activity act = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(nextCoord, leg.getRoute().getStartLinkId(), TransportMode.pt);
					trip.add(act);
				}
			}
			trip.add(leg);
			i++;
		}
		return trip;
	}

	private Route createWalkRoute(final Facility fromFacility, double departureTime, Person person, double travelTime, Facility firstToFacility, Attributes routingAttributes) {
		// yyyy I extracted this method to make a bit more transparent that it is used twice.  But I don't know why it is done in this way
		// (take distance from newly computed walk leg, but take travelTime from elsewhere).  Possibly, the problem is that the TransitRouter 
		// historically just does not compute the distances.  kai, may'17
		
		Route route = RouteUtils.createGenericRouteImpl(fromFacility.getLinkId(), firstToFacility.getLinkId());
		final List<? extends PlanElement> walkRoute = walkRouter.calcRoute(DefaultRoutingRequest.of(fromFacility, firstToFacility, departureTime, person, routingAttributes));
		route.setDistance(((Leg) walkRoute.get(0)).getRoute().getDistance());
		route.setTravelTime(travelTime);
		return route;
	}

} 
