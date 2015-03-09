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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a {@link TransitRouter}.
 *
 * @author thibautd
 */
public class TransitRouterWrapper implements RoutingModule {
    private static final StageActivityTypes CHECKER =
            new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
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
     * Just links to {@link TransitRouter#calcRoute(Coord, Coord, double, Person)}.
     *
     * @return the list of legs returned by the transit router.
     */
    @Override
    public List<? extends PlanElement> calcRoute(
            final Facility fromFacility,
            final Facility toFacility,
            final double departureTime,
            final Person person) {
        List<Leg> baseTrip = router.calcRoute(
                fromFacility.getCoord(),
                toFacility.getCoord(),
                departureTime,
                person);

        // the previous approach was to return null when no trip was found and
        // not to replace the trip if so.
        // However, this makes the output of routing modules more tricky to handle.
        // Thus, every module should return a valid trip. When available, the "main
        // mode" flag should be put to the mode of the routing module.
        return baseTrip != null ?
                fillWithActivities(baseTrip, fromFacility, toFacility, departureTime, person) :
                walkRouter.calcRoute(fromFacility, toFacility, departureTime, person);
    }

    /**
     * This treats the TransitRouter as a third-party interface, where missing fields
     * must be filled in (distance, travel-time in routes).
     */
    private List<PlanElement> fillWithActivities(
            final List<Leg> baseTrip,
            final Facility fromFacility,
            final Facility toFacility, double departureTime, Person person) {
        List<PlanElement> trip = new ArrayList<>();
        Coord nextCoord = null;
        int i = 0;
        for (Leg leg : baseTrip) {
            if (i == 0) {
                Facility firstToFacility;
                if (baseTrip.size() > 1) { // at least one pt leg available
                    ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) baseTrip.get(1).getRoute();
                    firstToFacility = this.transitSchedule.getFacilities().get(tRoute.getAccessStopId());
                } else {
                    firstToFacility = toFacility;
                }
                Route route = new GenericRouteImpl(fromFacility.getLinkId(), firstToFacility.getLinkId());
                final List<? extends PlanElement> walkRoute = walkRouter.calcRoute(fromFacility, firstToFacility, departureTime, person);
                route.setDistance(((Leg) walkRoute.get(0)).getRoute().getDistance());
                route.setTravelTime(leg.getTravelTime());
                leg.setRoute(route);
                trip.add(leg);
            } else {
                if (leg.getRoute() instanceof ExperimentalTransitRoute) {
                    ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) leg.getRoute();
                    tRoute.setTravelTime(leg.getTravelTime());
                    tRoute.setDistance(RouteUtils.calcDistance(tRoute, transitSchedule, network));
                    ActivityImpl act =
                            new ActivityImpl(
                                    PtConstants.TRANSIT_ACTIVITY_TYPE,
                                    this.transitSchedule.getFacilities().get(tRoute.getAccessStopId()).getCoord(),
                                    tRoute.getStartLinkId());
                    act.setMaximumDuration(0.0);
                    trip.add(act);
                    nextCoord = this.transitSchedule.getFacilities().get(tRoute.getEgressStopId()).getCoord();
                } else { // walk legs don't have a coord, use the coord from the last egress point
                    if (i == baseTrip.size() - 1) {
                        ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) baseTrip.get(baseTrip.size() - 2).getRoute();
                        Facility lastFromFacility = this.transitSchedule.getFacilities().get(tRoute.getEgressStopId());
                        Route route = new GenericRouteImpl(lastFromFacility.getLinkId(), toFacility.getLinkId());
                        final List<? extends PlanElement> walkRoute = walkRouter.calcRoute(lastFromFacility, toFacility, departureTime, person);
                        route.setDistance(((Leg) walkRoute.get(0)).getRoute().getDistance());
                        route.setTravelTime(leg.getTravelTime());
                        leg.setRoute(route);
                    }
                    ActivityImpl act =
                            new ActivityImpl(
                                    PtConstants.TRANSIT_ACTIVITY_TYPE,
                                    nextCoord,
                                    leg.getRoute().getStartLinkId());
                    act.setMaximumDuration(0.0);
                    trip.add(act);
                }
                trip.add(leg);
            }
            i++;
        }
        return trip;
    }

    @Override
    public StageActivityTypes getStageActivityTypes() {
        return CHECKER;
    }
} 
