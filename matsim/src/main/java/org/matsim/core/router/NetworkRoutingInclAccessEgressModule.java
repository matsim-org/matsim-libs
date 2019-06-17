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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.util.ArrayList;
import java.util.List;


/**
 * This wraps a "computer science" {@link LeastCostPathCalculator}, which routes from a node to another node, into something that
 * routes from a {@link Facility} to another {@link Facility}, as we need in MATSim.
 * 
 * @author thibautd, nagel
 */
public final class NetworkRoutingInclAccessEgressModule implements RoutingModule {
	private static final Logger log = Logger.getLogger( NetworkRoutingInclAccessEgressModule.class );

	private final class AccessEgressStageActivityTypes implements StageActivityTypes {
		@Override public boolean isStageActivity(String activityType) {
			if ( NetworkRoutingInclAccessEgressModule.this.stageActivityType.equals( activityType ) ) {
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
			return other.isStageActivity(NetworkRoutingInclAccessEgressModule.this.stageActivityType) ;
		}
		@Override public int hashCode() {
			return NetworkRoutingInclAccessEgressModule.this.stageActivityType.hashCode() ;
		}
	}


	private final String mode;
	private final PopulationFactory populationFactory;

	private final Network network;
	private final LeastCostPathCalculator routeAlgo;
	private String stageActivityType;

	public NetworkRoutingInclAccessEgressModule(
			final String mode,
			final PopulationFactory populationFactory,
			final Network network,
			final LeastCostPathCalculator routeAlgo,
			PlansCalcRouteConfigGroup calcRouteConfig) {
		Gbl.assertNotNull(network);
		Gbl.assertIf( network.getLinks().size()>0 ) ; // otherwise network for mode probably not defined
		this.network = network;
		this.routeAlgo = routeAlgo;
		this.mode = mode;
		this.populationFactory = populationFactory;
		this.stageActivityType = this.mode + " interaction";
		if ( !calcRouteConfig.isInsertingAccessEgressWalk() ) {
			throw new RuntimeException("trying to use access/egress but not switched on in config.  "
					+ "currently not supported; there are too many other problems") ;
		}
	}

	@Override
	public synchronized List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		// I need this "synchronized" since I want mobsim agents to be able to call this during the mobsim.  So when the
		// mobsim is multi-threaded, multiple agents might call this here at the same time.  kai, nov'17
		
		Gbl.assertNotNull(fromFacility);
		Gbl.assertNotNull(toFacility);

		Link accessActLink = FacilitiesUtils.decideOnLink(fromFacility, network );

		Link egressActLink = FacilitiesUtils.decideOnLink(toFacility, network );
		
		double now = departureTime ;

		List<PlanElement> result = new ArrayList<>() ;

		// === access:
		{
			now = addBushwhackingLegFromFacilityToLinkIfNecessary( fromFacility, person, accessActLink, now, result, populationFactory, stageActivityType );
		}

		// === compute the network leg:
		{
			Leg newLeg = this.populationFactory.createLeg( this.mode );
			newLeg.setDepartureTime( now );
			now += routeLeg( person, newLeg, accessActLink, egressActLink, now );

			result.add( newLeg ) ;
//			log.warn( newLeg );
		}

		// === egress:
		{
			addBushwhackingLegFromLinkToFacilityIfNecessary( toFacility, person, egressActLink, now, result, populationFactory, stageActivityType );
		}

		return result ;
	}
	
	public static void addBushwhackingLegFromLinkToFacilityIfNecessary( final Facility toFacility, final Person person,
												   final Link egressActLink, double now, final List<PlanElement> result,
												   final PopulationFactory populationFactory, final String stageActivityType ) {
		if ( toFacility.getCoord() != null ) { // otherwise the trip ends directly on the link; no need to bushwhack

			Coord egressActCoord = egressActLink.getToNode().getCoord() ;
			Gbl.assertNotNull( egressActCoord );

			final Activity interactionActivity = createInteractionActivity( egressActCoord, egressActLink.getId(), stageActivityType );
			result.add( interactionActivity ) ;
//				log.warn( interactionActivity );

			Leg egressLeg = populationFactory.createLeg( TransportMode.egress_walk ) ;
			egressLeg.setDepartureTime( now );
			routeBushwhackingLeg(person, egressLeg, egressActCoord, toFacility.getCoord(), now, egressActLink.getId(),
					egressActLink.getId(), populationFactory ) ;
			result.add( egressLeg ) ;
//				log.warn( egressLeg );
		}
		//			log.warn( "===" );
	}
	
	public static double addBushwhackingLegFromFacilityToLinkIfNecessary( final Facility fromFacility, final Person person,
												     final Link accessActLink, double now, final List<PlanElement> result, final PopulationFactory populationFactory, final String stageActivityType ) {
		if ( fromFacility.getCoord() != null && ! ( fromFacility instanceof LinkWrapperFacility ) ) {
			// otherwise the trip starts directly on the link; no need to bushwhack

			Coord accessActCoord  = accessActLink.getToNode().getCoord() ;
			// yyyy think about better solution: this may generate long walks along the link.
			// (e.g. orthogonal projection)
			Gbl.assertNotNull(accessActCoord);

			Leg accessLeg = populationFactory.createLeg( TransportMode.access_walk ) ;
			accessLeg.setDepartureTime( now );
			now += routeBushwhackingLeg(person, accessLeg, fromFacility.getCoord(), accessActCoord, now, accessActLink.getId(),
					accessActLink.getId(), populationFactory ) ;
			// yyyy might be possible to set the link ids to null. kai & dominik, may'16
			
			result.add( accessLeg ) ;
//				log.warn( accessLeg );

			final Activity interactionActivity = createInteractionActivity(accessActCoord, accessActLink.getId(), stageActivityType );
			result.add( interactionActivity ) ;
//				log.warn( interactionActivity );
		}
		return now;
	}
	
	private static Activity createInteractionActivity( final Coord interactionCoord, final Id<Link> interactionLink, final String stageActivityType ) {
		Activity act = PopulationUtils.createActivityFromCoordAndLinkId(stageActivityType, interactionCoord, interactionLink);
		act.setMaximumDuration(0.0);
		return act;
	}


	private static double routeBushwhackingLeg(Person person, Leg leg, Coord fromCoord, Coord toCoord, double depTime,
			Id<Link> dpLinkId, Id<Link> arLinkId, PopulationFactory pf) {
		// I don't think that it makes sense to use a RoutingModule for this, since that again makes assumptions about how to
		// map facilities, and if you follow through to the teleportation routers one even finds activity wrappers, which is yet another
		// complication which I certainly don't want here.  kai, dec'15

		// dpLinkId, arLinkId need to be in Route for lots of code to function.   So I am essentially putting in the "street address"
		// for completeness. Note that if we are walking to a parked car, this can be different from the car link id!!  kai, dec'15

		// make simple assumption about distance and walking speed
		double dist = CoordUtils.calcEuclideanDistance(fromCoord,toCoord);

		// create an empty route, but with realistic travel time
		Route route =pf.getRouteFactories().createRoute(Route.class, dpLinkId, arLinkId ); 

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
		return travTime;
	}


	@Override
	public StageActivityTypes getStageActivityTypes() {
		return new AccessEgressStageActivityTypes() ;
	}

	@Override
	public String toString() {
		return "[NetworkRoutingModule: mode="+this.mode+"]";
	}


	/*package (Tests)*/ double routeLeg(Person person, Leg leg, Link fromLink, Link toLink, double depTime) {
		double travTime = 0;

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

		if (toLink != fromLink) { // (a "true" route)

			Path path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime, person, null);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");

			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime((int) path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, 1.0,1.0,this.network));
			leg.setRoute(route);
			travTime = (int) path.travelTime;

		} else {
			// create an empty route == staying on place if toLink == endLink
			// note that we still do a route: someone may drive from one location to another on the link. kai, dec'15
			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);

		return travTime;
	}

}
