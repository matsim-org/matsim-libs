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
package org.matsim.contrib.av.intermodal.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Wraps a {@link TransitRouter}.
 *
 * @author thibautd, jbischoff
 */
public class VariableAccessTransitRouterWrapper implements RoutingModule {
    private static final StageActivityTypes CHECKER =
            new StageActivityTypesImpl();
    private final TransitRouter router;
    private final RoutingModule walkRouter;
    private final TransitSchedule transitSchedule;
    private final Network network;
    private final LeastCostPathCalculator routeAlgo;


    public VariableAccessTransitRouterWrapper(
            final TransitRouter router,
            final TransitSchedule transitSchedule,
            Network network, final RoutingModule walkRouter, final LeastCostPathCalculator routeAlgo) {
        if (router == null) {
            throw new NullPointerException("The router object is null, but is required later.");
        }
        this.router = router;
        this.transitSchedule = transitSchedule;
        this.network = network;
        if (walkRouter == null) {
            throw new NullPointerException("The walkRouter object is null, but is required later.");
        }
        if (routeAlgo == null) {
            throw new NullPointerException("The walkRouter object is null, but is required later.");
        }
        this.walkRouter = walkRouter;
        this.routeAlgo = routeAlgo ;
    }

    /**
     * Just links to {@link TransitRouter#calcRoute(Facility, Facility, double, Person)}.
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
                fromFacility,
                toFacility,
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
	    	if (leg.getMode().equals(TransportMode.car)){
	    		Id<Link> startLinkId =  leg.getRoute().getStartLinkId();
	    		Id<Link> endLinkId =  leg.getRoute().getEndLinkId();
	    		NetworkRoute nr = RouteUtils.createLinkNetworkRouteImpl(startLinkId, endLinkId);
	    		
	    		Path path = routeAlgo.calcLeastCostPath(network.getLinks().get(startLinkId).getToNode(), network.getLinks().get(endLinkId).getFromNode(), departureTime, person, null);
	    		nr.setLinkIds(startLinkId, NetworkUtils.getLinkIds(path.links), endLinkId);
	    		nr.setTravelCost(path.travelCost);
	    		nr.setTravelTime(path.travelTime);
	    		
	    		nr.setDistance(RouteUtils.calcDistance(nr, 1.0, 1.0, network));
	    		leg.setRoute(nr);
	    	}
	    	
		    if (i == 0) {
			    // (access leg)
			    Facility firstToFacility;
			    if ((baseTrip.size() > 1) &&  (baseTrip.get(1).getRoute() instanceof ExperimentalTransitRoute) ) { // at least one pt leg available
			    	
				    ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) baseTrip.get(1).getRoute();
				    firstToFacility = this.transitSchedule.getFacilities().get(tRoute.getAccessStopId());
			    } else {
				    firstToFacility = toFacility;
			    }
//			    Route route = new GenericRouteImpl(fromFacility.getLinkId(), firstToFacility.getLinkId());
//			    final List<? extends PlanElement> walkRoute = walkRouter.calcRoute(fromFacility, firstToFacility, departureTime, person);
//			    route.setDistance(((Leg) walkRoute.get(0)).getRoute().getDistance());
//			    route.setTravelTime(leg.getTravelTime());
//			    leg.setRoute(route);
			    trip.add(leg);
		    } else {
			    if (leg.getRoute() instanceof ExperimentalTransitRoute) {
				    ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) leg.getRoute();
				    tRoute.setTravelTime(leg.getTravelTime());
				    tRoute.setDistance(RouteUtils.calcDistance(tRoute, transitSchedule, network));
				    Activity act =
						    PopulationUtils.createActivityFromCoordAndLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, this.transitSchedule.getFacilities().get(tRoute.getAccessStopId()).getCoord(), tRoute.getStartLinkId());
				    act.setMaximumDuration(0.0);
				    trip.add(act);
				    nextCoord = this.transitSchedule.getFacilities().get(tRoute.getEgressStopId()).getCoord();
			    } else { // walk legs don't have a coord, use the coord from the last egress point
//				    if (i == baseTrip.size() - 1) {
//					    ExperimentalTransitRoute tRoute = (ExperimentalTransitRoute) baseTrip.get(baseTrip.size() - 2).getRoute();
//					    Facility lastFromFacility = this.transitSchedule.getFacilities().get(tRoute.getEgressStopId());
//					    Route route = new GenericRouteImpl(lastFromFacility.getLinkId(), toFacility.getLinkId());
//					    final List<? extends PlanElement> walkRoute = walkRouter.calcRoute(lastFromFacility, toFacility, departureTime, person);
//					    route.setDistance(((Leg) walkRoute.get(0)).getRoute().getDistance());
//					    route.setTravelTime(leg.getTravelTime());
//					    leg.setRoute(route);
//				    }
				    Activity act =
						    PopulationUtils.createActivityFromCoordAndLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, nextCoord, leg.getRoute().getStartLinkId());
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
