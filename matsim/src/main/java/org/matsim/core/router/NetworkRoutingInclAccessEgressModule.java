/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.core.router;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;


/**
 * This wraps a "computer science" {@link LeastCostPathCalculator}, which routes from a node to another node, into something that
 * routes from a {@link Facility} to another {@link Facility}, as we need in MATSim.
 * 
 * @author thibautd, nagel
 */
public final class NetworkRoutingInclAccessEgressModule implements RoutingModule {

	private final class AccessEgressStageActivityTypes implements StageActivityTypes {
		@Override public boolean isStageActivity(String activityType) {
			if ( stageActivityType.equals( activityType ) ) {
				return true ;
			} else {
				return false ;
			}
		}
		@Override public boolean equals( Object obj ) {
			if ( !(obj instanceof AccessEgressStageActivityTypes) ) {
				return false ;
			}
			AccessEgressStageActivityTypes other = (AccessEgressStageActivityTypes) obj ;
			return other.isStageActivity(stageActivityType) ;
		}
		@Override public int hashCode() {
			return stageActivityType.hashCode() ;
		}
	}


	private final String mode;
	private final PopulationFactory populationFactory;

	private final Network network;
	private final ModeRouteFactory routeFactory;
	private final LeastCostPathCalculator routeAlgo;
	private String stageActivityType;

	public NetworkRoutingInclAccessEgressModule(
			final String mode,
			final PopulationFactory populationFactory,
			final Network network,
			final LeastCostPathCalculator routeAlgo,
			final ModeRouteFactory routeFactory, PlansCalcRouteConfigGroup calcRouteConfig) {
		this.network = network;
		this.routeAlgo = routeAlgo;
		this.routeFactory = routeFactory;
		this.mode = mode;
		this.populationFactory = populationFactory;
		this.stageActivityType = this.mode + " interaction";
		if ( !calcRouteConfig.isInsertingAccessEgressWalk() ) {
			throw new RuntimeException("trying to use access/egress but not switched on in config.  "
					+ "currently not supported; there are too many other problems") ;
		}
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {

		final Id<Link> accessActLinkId = fromFacility.getLinkId();
		final Id<Link> egressActLinkId = toFacility.getLinkId();
		double now = departureTime ;

		List<PlanElement> result = new ArrayList<>() ;

		// === access:
		{
			final Coord fromCoord = fromFacility.getCoord();
			if ( fromCoord != null ) { // otherwise the trip starts directly on the link; no need to bushwhack

				Coord accessActCoord = network.getLinks().get( fromFacility.getLinkId() ).getToNode().getCoord() ;
				// yy maybe use orthogonal distance instead?
				Gbl.assertNotNull( accessActCoord );

				Leg accessLeg = this.populationFactory.createLeg( TransportMode.access_walk ) ;
				accessLeg.setDepartureTime( now );
				now += routeBushwhackingLeg(person, accessLeg, fromCoord, accessActCoord, now, accessActLinkId, accessActLinkId ) ;
				// (in this setup, street address of starting point and link of interaction activity are the same)

				result.add( accessLeg ) ;

				result.add( createInteractionActivity(accessActCoord, accessActLinkId) ) ;
			}
		}

		// === compute the network leg:
		{
			Leg newLeg = populationFactory.createLeg( mode );
			newLeg.setDepartureTime( now );
			now += routeLeg( person, newLeg, accessActLinkId, egressActLinkId, now );

			result.add( newLeg ) ;
		}

		// === egress:
		{
			final Coord toCoord = toFacility.getCoord();
			if ( toCoord != null ) { // otherwise the trip ends directly on the link; no need to bushwhack

				Coord egressActCoord = network.getLinks().get( egressActLinkId ).getToNode().getCoord() ;
				// yy maybe use orthogonal distance instead?
				Gbl.assertNotNull( egressActCoord );

				result.add( createInteractionActivity( egressActCoord, egressActLinkId ) ) ;

				Leg egressLeg = this.populationFactory.createLeg( TransportMode.egress_walk ) ;
				egressLeg.setDepartureTime( now );
				now += routeBushwhackingLeg(person, egressLeg, egressActCoord, toCoord, now, egressActLinkId, egressActLinkId ) ;
				// (in this setup, link of interaction activity and street address of destination are the same)

				result.add( egressLeg ) ;
			}
		}

		return result ;
	}
	
	private ActivityImpl createInteractionActivity(final Coord interactionCoord, final Id<Link> interactionLink) {
		ActivityImpl act = new ActivityImpl( stageActivityType, interactionCoord, interactionLink);
		act.setMaximumDuration(0.0);
		return act;
	}


	private double routeBushwhackingLeg(Person person, Leg leg, Coord fromCoord, Coord toCoord, double depTime, Id<Link> dpLinkId, Id<Link> arLinkId) {
		// I don't think that it makes sense to use a RoutingModule for this, since that again makes assumptions about how to
		// map facilities, and if you follow throgh to the teleportation routers one even finds activity wrappers, which is yet another
		// complication which I certainly don't want here.  kai, dec'15
		
		// dpLinkId, arLinkId need to be in Route for lots of code to function.   So I am essentially putting in the "street address"
		// for completeness. Note that if we are walking to a parked car, this can be different from the car link id!!  kai, dec'15
		
		// make simple assumption about distance and walking speed
		double dist = CoordUtils.calcEuclideanDistance(fromCoord,toCoord);

		// create an empty route, but with realistic travel time
		Route route = this.routeFactory.createRoute(Route.class, dpLinkId, arLinkId ); 

		double beelineDistanceFactor = 1.3 ;
		double networkTravelSpeed = 2.0 ;
		// yyyyyy take this from config!

		double estimatedNetworkDistance = dist * beelineDistanceFactor;
		int travTime = (int) (estimatedNetworkDistance / networkTravelSpeed);
		route.setTravelTime(travTime);
		route.setDistance(estimatedNetworkDistance);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}


	@Override
	public StageActivityTypes getStageActivityTypes() {
		return new AccessEgressStageActivityTypes() ;
	}

	@Override
	public String toString() {
		return "[NetworkRoutingModule: mode="+mode+"]";
	}


	/*package (Tests)*/ double routeLeg(Person person, Leg leg, Id<Link> fromLinkId, Id<Link> toLinkId, double depTime) {
		double travTime = 0;
		Link fromLink = this.network.getLinks().get(fromLinkId);
		Link toLink = this.network.getLinks().get(toLinkId);

		/* Remove this and next three lines once debugged. */
		if(fromLink == null || toLink == null){
			Logger.getLogger(NetworkRoutingInclAccessEgressModule.class).error("  ==>  null from/to link for person " + person.getId().toString());
		}
		if (fromLink == null) throw new RuntimeException("fromLink "+fromLinkId+" missing.");
		if (toLink == null) throw new RuntimeException("toLink "+toLinkId+" missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

		//		CarRoute route = null;
		//		Path path = null;
		if (toLink != fromLink) {
			// (a "true" route)
			Path path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime, person, null);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			NetworkRoute route = this.routeFactory.createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime((int) path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, 1.0,1.0,this.network));
			leg.setRoute(route);
			travTime = (int) path.travelTime;
		} else {
			// create an empty route == staying on place if toLink == endLink
			// note that we still do a route: someone may drive from one location to another on the link. kai, dec'15
			NetworkRoute route = this.routeFactory.createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		if ( leg instanceof LegImpl ) {
			((LegImpl) leg).setArrivalTime(depTime + travTime); 
			// (not in interface!)
		}
		return travTime;
	}

}
