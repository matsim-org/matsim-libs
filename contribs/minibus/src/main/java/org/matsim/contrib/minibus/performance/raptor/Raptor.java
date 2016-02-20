/* *********************************************************************** *
 * project: org.matsim.*
 * TranitRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.performance.raptor;

import java.util.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Wrapper for {@linkplain RaptorWalker}.
 *
 * @author aneumann
 */
public class Raptor implements TransitRouter {

    private final TransitRouterQuadTree transitRouterQuadTree;

    private final RaptorWalker raptorWalker;
    private final TransitRouterConfig config;
    private final RaptorDisutility raptorDisutility;

    // MAGIC NUMBERS BELOW THIS LINE
    int maxTransfers = 10;
    int graceRuns = 1;
    // END MAGIC NUMBERS

    public Raptor(TransitRouterQuadTree transitRouterQuadTree, RaptorDisutility raptorDisutility, TransitRouterConfig transitRouterConfig) {
    	this.transitRouterQuadTree = transitRouterQuadTree;
    	this.raptorDisutility = raptorDisutility;
    	this.config = transitRouterConfig;
        
        this.raptorWalker = new RaptorWalker(this.transitRouterQuadTree.getSearchData(), this.raptorDisutility, this.maxTransfers, this.graceRuns);
    }

	private Map<TransitStopFacility, InitialNode> locateWrappedNearestTransitStops(Person person, Coord coord, double departureTime) {
        Collection<TransitStopFacility> nearestTransitStops = this.transitRouterQuadTree.getNearestTransitStopFacilities(coord, this.config.getSearchRadius());
        if (nearestTransitStops.size() < 2) {
            // also enlarge search area if only one stop found, maybe a second one is near the border of the search area
            TransitStopFacility nearestTransitStop = this.transitRouterQuadTree.getNearestTransitStopFacility(coord);
            double distance = CoordUtils.calcEuclideanDistance(coord, nearestTransitStop.getCoord());
            nearestTransitStops = this.transitRouterQuadTree.getNearestTransitStopFacilities(coord, distance + this.config.getExtensionRadius());
        }
        Map<TransitStopFacility, InitialNode> wrappedNearestTransitStops2AccessCost = new LinkedHashMap<>();
        for (TransitStopFacility node : nearestTransitStops) {
            Coord toCoord = node.getCoord();
            double initialTime = getWalkTime( coord, toCoord);
            double initialCost = getWalkDisutility(coord, toCoord);
            wrappedNearestTransitStops2AccessCost.put(node, new InitialNode(initialCost, initialTime + departureTime));
        }
        return wrappedNearestTransitStops2AccessCost;
    }

    private double getWalkTime(Coord coord, Coord toCoord) {
        return this.raptorDisutility.getTravelTime(coord, toCoord);
    }

    private double getWalkDisutility(Coord coord, Coord toCoord) {
        return this.raptorDisutility.getTravelDisutility(coord, toCoord);
    }

    @Override
    public List<Leg> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime, final Person person) {
        // find possible start stops
        Map<TransitStopFacility, InitialNode> fromStops = this.locateWrappedNearestTransitStops(person, fromCoord, departureTime);
        // find possible end stops
        Map<TransitStopFacility, InitialNode> toStops = this.locateWrappedNearestTransitStops(person, toCoord, departureTime);

        // find routes between start and end stops
        RaptorRoute p = this.raptorWalker.calcLeastCostPath(fromStops, toStops);

        if (p == null) {
            return null;
        }

        double directWalkCost = getWalkDisutility(fromCoord, toCoord);
        double pathCost = p.getTravelCost();

        if (directWalkCost < pathCost) {
            return this.createDirectWalkLegList(null, fromCoord, toCoord);
        }
        return convertPathToLegList(departureTime, p, fromCoord, toCoord, person);
    }

    private List<Leg> createDirectWalkLegList(Person person, Coord fromCoord, Coord toCoord) {
        List<Leg> legs = new ArrayList<>();
        Leg leg = new LegImpl(TransportMode.transit_walk);
        double walkTime = getWalkTime(fromCoord, toCoord);
        leg.setTravelTime(walkTime);
        Route walkRoute = new GenericRouteImpl(null, null);
        walkRoute.setTravelTime(walkTime);
        leg.setRoute(walkRoute);
        legs.add(leg);
        return legs;
    }

    protected List<Leg> convertPathToLegList(double departureTime, RaptorRoute p, Coord fromCoord, Coord toCoord, Person person) {
    	// convert the route into a sequence of legs
    	List<Leg> legs = new ArrayList<>();
    	
    	// access leg
    	Leg accessLeg;
    	// check if first leg extends walking distance
    	if (p.getRoute().get(0).routeTaken == null) {
			// route starts with transfer - extend initial walk to that stop
    		accessLeg = createTransitWalkLeg(fromCoord, p.getRoute().get(0).toStop.getCoord());
    		p.getRoute().remove(0);
		} else {
			// do not extend it - add a regular walk leg
			// 
			accessLeg = createTransitWalkLeg(fromCoord, p.getRoute().get(0).fromStop.getCoord());
		}
    	
    	// egress leg
    	Leg egressLeg;
    	// check if first leg extends walking distance
    	if (p.getRoute().get(p.getRoute().size() - 1).routeTaken == null) {
			// route starts with transfer - extend initial walk to that stop
    		egressLeg = createTransitWalkLeg(p.getRoute().get(p.getRoute().size() - 1).fromStop.getCoord(), toCoord);
    		p.getRoute().remove(p.getRoute().size() - 1);
		} else {
			// do not extend it - add a regular walk leg
			// access leg
			egressLeg = createTransitWalkLeg(p.getRoute().get(p.getRoute().size() - 1).toStop.getCoord(), toCoord);
		}
    	
    	
    	// add very first leg
    	legs.add(accessLeg);
    	
    	// route segments are now in pt-walk-pt sequence
    	for (RouteSegment routeSegement : p.getRoute()) {
    		if (routeSegement.routeTaken == null) {
    			// transfer
    			legs.add(createTransferTransitWalkLeg(routeSegement));
			} else {
				// pt leg
				legs.add(createTransitLeg(routeSegement));
			}
		}
        
    	// add last leg
    	legs.add(egressLeg);

    	return legs;
    }

	private Leg createTransitLeg(RouteSegment routeSegment) {
		Leg leg = new LegImpl(TransportMode.pt);
		
		TransitStopFacility accessStop = routeSegment.fromStop;
        TransitStopFacility egressStop = routeSegment.toStop;
        
        ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, egressStop, routeSegment.lineTaken, routeSegment.routeTaken);
        leg.setRoute(ptRoute);

        leg.setTravelTime(routeSegment.travelTime);
        return leg;
	}

	private Leg createTransferTransitWalkLeg(RouteSegment routeSegement) {
		Leg leg = this.createTransitWalkLeg(routeSegement.fromStop.getCoord(), routeSegement.toStop.getCoord());
		Route walkRoute = new GenericRouteImpl(routeSegement.fromStop.getLinkId(), routeSegement.toStop.getLinkId());
        walkRoute.setTravelTime(leg.getTravelTime());
        leg.setRoute(walkRoute);
		
        return leg;
	}
	
	private Leg createTransitWalkLeg(Coord fromCoord, Coord toCoord) {
		Leg leg = new LegImpl(TransportMode.transit_walk);
        double walkTime = getWalkTime(fromCoord, toCoord);
        leg.setTravelTime(walkTime);
        return leg;
	}

    public TransitRouterQuadTree getTransitRouterNetwork() {
        return this.transitRouterQuadTree;
    }

    protected TransitRouterQuadTree getTransitNetwork() {
        return this.transitRouterQuadTree;
    }

    protected TransitRouterConfig getConfig() {
        return config;
    }

}
