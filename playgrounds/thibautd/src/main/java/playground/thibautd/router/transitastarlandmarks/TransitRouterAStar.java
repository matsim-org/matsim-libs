/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.router.transitastarlandmarks;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.util.Landmarker;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PieSlicesLandmarker;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author thibautd
 */
public class TransitRouterAStar implements TransitRouter {

    private final TransitRouterNetwork transitNetwork;

    private final MultiNodeAStarLandmarks dijkstra;
    private final TransitRouterConfig config;
    private final TransitDisutilityWithMinimum travelDisutility;

    private final PreparedTransitSchedule preparedTransitSchedule;

	public TransitRouterAStar(final Config config, final TransitSchedule schedule) {
		this(
				ConfigUtils.addOrGetModule(
						config,
						TransitRouterAStarConfigGroup.GROUP_NAME,
						TransitRouterAStarConfigGroup.class ),
				new TransitRouterConfig( config ) ,
				schedule );
	}

    public TransitRouterAStar(
			final TransitRouterAStarConfigGroup astarConfig,
			final TransitRouterConfig config,
			final TransitSchedule schedule) {
        this.preparedTransitSchedule = new PreparedTransitSchedule(schedule);
        this.config = config;
        this.travelDisutility =
				new TransitDisutilityWithMinimum(
						config,
						preparedTransitSchedule);
        this.transitNetwork = TransitRouterNetwork.createFromSchedule(schedule, config.getBeelineWalkConnectionDistance());
		final PreProcessLandmarks preprocess =
				new PreProcessLandmarks(
						travelDisutility,
						createLandmarker( astarConfig ),
						astarConfig.getNLandmarks() );

		preprocess.run( transitNetwork );
        this.dijkstra = new MultiNodeAStarLandmarks(
				astarConfig.getInitiallyActiveLandmarks(),
				astarConfig.getOverdoFactor(),
				this.transitNetwork,
				preprocess,
				this.travelDisutility,
				this.travelDisutility );
    }

	private Landmarker createLandmarker( TransitRouterAStarConfigGroup astarConfig ) {
		switch ( astarConfig.getLandmarkComputation() ) {
			case degree:
				return new DegreeBasedLandmarker();
			case pieSlice:
				return new PieSlicesLandmarker( new Rectangle2D.Double() );
			case centrality:
				return new CentralityBasedLandmarker( travelDisutility );
		}
		throw new RuntimeException( "unknown landmarker "+astarConfig.getLandmarkComputation() );
	}

	MultiNodeAStarLandmarks getAStarAlgorithm() {
		return dijkstra;
	}

	MultiNodeDijkstra getEquivalentDijkstra() {
		return new MultiNodeDijkstra( transitNetwork , travelDisutility , travelDisutility );
	}

    Iterable<MultiNodeAStarLandmarks.InitialNode> locateWrappedNearestTransitNodes(Person person, Coord coord, double departureTime) {
        Collection<TransitRouterNetwork.TransitRouterNetworkNode> nearestNodes = this.transitNetwork.getNearestNodes(coord, this.config.getSearchRadius());
        if (nearestNodes.size() < 2) {
            // also enlarge search area if only one stop found, maybe a second one is near the border of the search area
            TransitRouterNetwork.TransitRouterNetworkNode nearestNode = this.transitNetwork.getNearestNode(coord);
            double distance = CoordUtils.calcEuclideanDistance( coord, nearestNode.stop.getStopFacility().getCoord() );
            nearestNodes = this.transitNetwork.getNearestNodes(coord, distance + this.config.getExtensionRadius());
        }
        List<MultiNodeAStarLandmarks.InitialNode> wrappedNearestNodes = new ArrayList<>();
        for (TransitRouterNetwork.TransitRouterNetworkNode node : nearestNodes) {
            Coord toCoord = node.stop.getStopFacility().getCoord();
            double initialTime = getWalkTime(person, coord, toCoord);
            double initialCost = getWalkDisutility(person, coord, toCoord);
            wrappedNearestNodes.add(
					new MultiNodeAStarLandmarks.InitialNode(
							node,
							initialCost,
							initialTime + departureTime ) );
        }
        return wrappedNearestNodes;
    }

    private double getWalkTime(Person person, Coord coord, Coord toCoord) {
        return travelDisutility.getTravelTime(person, coord, toCoord);
    }

    private double getWalkDisutility(Person person, Coord coord, Coord toCoord) {
        return travelDisutility.getTravelDisutility( person, coord, toCoord );
    }

    @Override
    public List<Leg> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime, final Person person) {
        // find possible start stops
        Iterable<MultiNodeAStarLandmarks.InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNodes(person, fromCoord, departureTime);
        // find possible end stops
        Iterable<MultiNodeAStarLandmarks.InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNodes(person, toCoord, departureTime);

        // find routes between start and end stops
        LeastCostPathCalculator.Path p = this.dijkstra.calcLeastCostPath(wrappedFromNodes, wrappedToNodes, person);

        if (p == null) {
            return null;
        }

        double directWalkCost = getWalkDisutility(person, fromCoord, toCoord);
        double pathCost = p.travelCost +
				getCost( p.nodes.get( 0 ) , wrappedFromNodes ) +
				getCost( p.nodes.get( p.nodes.size() - 1 ) , wrappedToNodes );

        if (directWalkCost < pathCost) {
            return this.createDirectWalkLegList(null, fromCoord, toCoord);
        }
        return convertPathToLegList(departureTime, p, fromCoord, toCoord, person);
    }

	private double getCost( Node node, Iterable<MultiNodeAStarLandmarks.InitialNode> wrappedToNodes ) {
		for ( MultiNodeAStarLandmarks.InitialNode in : wrappedToNodes ) {
			if ( in.node == node ) return in.initialCost;
		}
		throw new RuntimeException( node+" not in "+wrappedToNodes );
	}

	private List<Leg> createDirectWalkLegList(Person person, Coord fromCoord, Coord toCoord) {
        List<Leg> legs = new ArrayList<>();
        Leg leg = new LegImpl(TransportMode.transit_walk);
        double walkTime = getWalkTime( person, fromCoord, toCoord );
        leg.setTravelTime(walkTime);
        Route walkRoute = new GenericRouteImpl(null, null);
        walkRoute.setTravelTime(walkTime);
        leg.setRoute(walkRoute);
        legs.add( leg );
        return legs;
    }

    protected List<Leg> convertPathToLegList(double departureTime, LeastCostPathCalculator.Path path, Coord fromCoord, Coord toCoord, Person person) {
	    // now convert the path back into a series of legs with correct routes
	    double time = departureTime;
	    List<Leg> legs = new ArrayList<>();
	    Leg leg;
	    TransitLine line = null;
	    TransitRoute route = null;
	    TransitStopFacility accessStop = null;
	    TransitRouteStop transitRouteStart = null;
	    TransitRouterNetwork.TransitRouterNetworkLink prevLink = null;
	    int transitLegCnt = 0;
	    for (Link ll : path.links) {
		    TransitRouterNetwork.TransitRouterNetworkLink link = (TransitRouterNetwork.TransitRouterNetworkLink) ll;
		    if (link.getLine() == null) {
			    // (it must be one of the "transfer" links.) finish the pt leg, if there was one before...
			    TransitStopFacility egressStop = link.fromNode.stop.getStopFacility();
			    if (route != null) {
				    leg = new LegImpl(TransportMode.pt);
				    ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
				    double arrivalOffset = (link.getFromNode().stop.getArrivalOffset() != Time.UNDEFINED_TIME) ? link.fromNode.stop.getArrivalOffset() : link.fromNode.stop.getDepartureOffset();
				    double arrivalTime = this.preparedTransitSchedule.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
				    ptRoute.setTravelTime(arrivalTime - time);
				    leg.setRoute(ptRoute);
				    leg.setTravelTime(arrivalTime - time);
				    time = arrivalTime;
				    legs.add(leg);
				    transitLegCnt++;
				    accessStop = egressStop;
			    }
			    line = null;
			    route = null;
			    transitRouteStart = null;
		    } else {
			    // (a real pt link)
			    if (link.getRoute() != route) {
				    // the line changed
				    TransitStopFacility egressStop = link.fromNode.stop.getStopFacility();
				    if (route == null) {
					    // previously, the agent was on a transfer, add the walk leg
					    transitRouteStart = ((TransitRouterNetwork.TransitRouterNetworkLink) ll).getFromNode().stop;
					    if (accessStop != egressStop) {
						    if (accessStop != null) {
							    leg = new LegImpl(TransportMode.transit_walk);
							    double walkTime = getWalkTime(person, accessStop.getCoord(), egressStop.getCoord());
							    Route walkRoute = new GenericRouteImpl(accessStop.getLinkId(), egressStop.getLinkId());
							    walkRoute.setTravelTime(walkTime);
							    leg.setRoute(walkRoute);
							    leg.setTravelTime(walkTime);
							    time += walkTime;
							    legs.add(leg);
						    } else { // accessStop == null, so it must be the first walk-leg
								    leg = new LegImpl(TransportMode.transit_walk);
						    double walkTime = getWalkTime(person, fromCoord, egressStop.getCoord());
						    leg.setTravelTime(walkTime);
						    time += walkTime;
						    legs.add(leg);
						    }
					    }
				    }
				    line = link.getLine();
				    route = link.getRoute();
				    accessStop = egressStop;
			    }
		    }
		    prevLink = link;
	    }
	    if (route != null) {
		    // the last part of the path was with a transit route, so add the pt-leg and final walk-leg
		    leg = new LegImpl(TransportMode.pt);
		    TransitStopFacility egressStop = prevLink.toNode.stop.getStopFacility();
		    ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, line, route, egressStop);
		    leg.setRoute(ptRoute);
		    double arrivalOffset = ((prevLink).toNode.stop.getArrivalOffset() != Time.UNDEFINED_TIME) ?
				    (prevLink).toNode.stop.getArrivalOffset()
				    : (prevLink).toNode.stop.getDepartureOffset();
				    double arrivalTime = this.preparedTransitSchedule.getNextDepartureTime(route, transitRouteStart, time) + (arrivalOffset - transitRouteStart.getDepartureOffset());
				    leg.setTravelTime(arrivalTime - time);
				    legs.add(leg);
				    transitLegCnt++;
				    accessStop = egressStop;
	    }
	    if (prevLink != null) {
		    leg = new LegImpl( TransportMode.transit_walk);
		    double walkTime;
		    if (accessStop == null) {
			    walkTime = getWalkTime(person, fromCoord, toCoord);
		    } else {
			    walkTime = getWalkTime(person, accessStop.getCoord(), toCoord);
		    }
		    leg.setTravelTime(walkTime);
		    legs.add(leg);
	    }
	    if (transitLegCnt == 0) {
		    // it seems, the agent only walked
		    legs.clear();
		    leg = new LegImpl(TransportMode.transit_walk);
		    double walkTime = getWalkTime(person, fromCoord, toCoord);
		    leg.setTravelTime(walkTime);
		    legs.add(leg);
	    }
	    return legs;
    }

    protected MultiNodeAStarLandmarks getDijkstra() {
        return dijkstra;
    }

    protected TransitRouterConfig getConfig() {
        return config;
    }

}
