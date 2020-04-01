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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.matsim.core.config.groups.PlansCalcRouteConfigGroup.*;


/**
 * This wraps a "computer science" {@link LeastCostPathCalculator}, which routes from a node to another node, into something that
 * routes from a {@link Facility} to another {@link Facility}, as we need in MATSim.
 * 
 * @author thibautd, nagel
 */
public final class NetworkRoutingInclAccessEgressModule implements RoutingModule {
	private static final Logger log = Logger.getLogger( NetworkRoutingInclAccessEgressModule.class );

	private final String mode;
	private final PopulationFactory populationFactory;

	private final Network filteredNetwork;
	private final LeastCostPathCalculator routeAlgo;
	private final Scenario scenario;
	private final RoutingModule accessEgressToNetworkRouter;
	private final Config config;

	NetworkRoutingInclAccessEgressModule(
		  final String mode,
		  final LeastCostPathCalculator routeAlgo, Scenario scenario, Network filteredNetwork,
		  final RoutingModule accessEgressToNetworkRouter) {
		Gbl.assertNotNull(scenario.getNetwork());
		Gbl.assertIf( scenario.getNetwork().getLinks().size()>0 ) ; // otherwise network for mode probably not defined
		this.filteredNetwork = filteredNetwork ;
		this.routeAlgo = routeAlgo;
		this.mode = mode;
		this.scenario = scenario;
		this.populationFactory = scenario.getPopulation().getFactory() ;
		this.config = scenario.getConfig();
		this.accessEgressToNetworkRouter = accessEgressToNetworkRouter;
		if ( !scenario.getConfig().plansCalcRoute().isInsertingAccessEgressWalk() ) {
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

		Link accessActLink = FacilitiesUtils.decideOnLink(fromFacility, filteredNetwork );

		Link egressActLink = FacilitiesUtils.decideOnLink(toFacility, filteredNetwork );
		
		double now = departureTime ;

		List<PlanElement> result = new ArrayList<>() ;

		// === access:
		{
			now = addBushwhackingLegFromFacilityToLinkIfNecessary( fromFacility, person, accessActLink, now, result, populationFactory, mode,
					scenario.getConfig() );
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
			addBushwhackingLegFromLinkToFacilityIfNecessary( toFacility, person, egressActLink, now, result, populationFactory, mode,
					scenario.getConfig() );
		}

		return result ;
	}
	
	private void addBushwhackingLegFromLinkToFacilityIfNecessary( final Facility toFacility, final Person person,
									    final Link egressActLink, double now, final List<PlanElement> result,
									    final PopulationFactory populationFactory, final String stageActivityType,
									    Config config ) {

		log.debug( "do bushwhacking leg from link=" + egressActLink.getId() + " to facility=" + toFacility.toString() ) ;

		if( isNotNeedingBushwhackingLeg( toFacility ) ) {
			return;
		}

		Coord startCoord = egressActLink.getToNode().getCoord() ;
		Gbl.assertNotNull( startCoord );

		final Id<Link> startLinkId = egressActLink.getId();

		// check whether we already have an identical interaction activity directly before
		PlanElement lastPlanElement = result.get( result.size() - 1 );
		if ( lastPlanElement instanceof Leg ) {
			final Activity interactionActivity = createInteractionActivity( startCoord, startLinkId, stageActivityType );
			result.add( interactionActivity ) ;
		} else {
			// don't add another (interaction) activity
			// TODO: assuming that this is an interaction activity, e.g. walk - drt interaction - walk
			// Not clear what we should do if it is not an interaction activity (and how that could happen).
		}

		Id<Link> endLinkId = toFacility.getLinkId();
		if ( endLinkId==null ) {
			endLinkId = startLinkId;
		}

		if (mode.equals(TransportMode.walk)) {
			Leg egressLeg = populationFactory.createLeg( TransportMode.non_network_walk ) ;
			egressLeg.setDepartureTime( now );
		routeBushwhackingLeg(person, egressLeg, startCoord, toFacility.getCoord(), now, startLinkId, endLinkId, populationFactory, config ) ;
			result.add( egressLeg ) ;
		} else {
			Facility fromFacility = FacilitiesUtils.wrapLink(egressActLink);
			result.addAll(accessEgressToNetworkRouter.calcRoute(fromFacility, toFacility, now, person));
		}
	}

	private static boolean isNotNeedingBushwhackingLeg( Facility toFacility ){
		if ( toFacility.getCoord() == null ) {
			// facility does not have a coordinate; we cannot bushwhack
			return true;
		}
		// trip ends on link; no need to bushwhack (this is, in fact, not totally clear: might be link on network of other mode)
		return toFacility instanceof LinkWrapperFacility;
	}

	private double addBushwhackingLegFromFacilityToLinkIfNecessary( final Facility fromFacility, final Person person,
									      final Link accessActLink, double now, final List<PlanElement> result,
									      final PopulationFactory populationFactory, final String stageActivityType,
									      Config config ) {
		if ( isNotNeedingBushwhackingLeg( fromFacility ) ) {
			return now ;
		}

		Coord endCoord  = accessActLink.getToNode().getCoord() ;
		// yyyy think about better solution: this may generate long walks along the link. (e.g. orthogonal projection)
		Gbl.assertNotNull(endCoord);

		if (mode.equals(TransportMode.walk)) {
			Leg accessLeg = populationFactory.createLeg( TransportMode.non_network_walk ) ;
			accessLeg.setDepartureTime( now );

			final Id<Link> startLinkId = fromFacility.getLinkId() ;
			if ( startLinkId==null ){
				accessActLink.getId();
			}

		now += routeBushwhackingLeg(person, accessLeg, fromFacility.getCoord(), endCoord, now, startLinkId, accessActLink.getId(), populationFactory,
				config ) ;
			// yyyy might be possible to set the link ids to null. kai & dominik, may'16

			result.add( accessLeg ) ;
		} else {
			Facility toFacility = FacilitiesUtils.wrapLink(accessActLink);
			List<? extends PlanElement> accessTrip = accessEgressToNetworkRouter.calcRoute(fromFacility, toFacility, now, person);
			for (PlanElement planElement: accessTrip) {
				now = TripRouter.calcEndOfPlanElement( now, planElement, config ) ;
			}
			result.addAll(accessTrip);
		}

		final Activity interactionActivity = createInteractionActivity(endCoord, accessActLink.getId(), stageActivityType );
		result.add( interactionActivity ) ;

		return now;
	}
	
	private static Activity createInteractionActivity( final Coord interactionCoord, final Id<Link> interactionLink, final String mode ) {
		Activity act = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(interactionCoord, interactionLink, mode);
		act.setMaximumDuration(0.0);
		return act;
	}

	private static double routeBushwhackingLeg( Person person, Leg leg, Coord fromCoord, Coord toCoord, double depTime,
						    Id<Link> dpLinkId, Id<Link> arLinkId, PopulationFactory pf, Config config ) {
		ModeRoutingParams params = null ;
		ModeRoutingParams tmp;
		final Map<String, ModeRoutingParams> paramsMap = config.plansCalcRoute().getModeRoutingParams();
		if ( (tmp = paramsMap.get( TransportMode.non_network_walk ) ) != null ){
			params = tmp;
		} else if ( (tmp = paramsMap.get(  TransportMode.walk ) ) != null ) {
			params = tmp ;
		} else{
			params = new ModeRoutingParams();
			// old defaults
			params.setBeelineDistanceFactor( 1.3 );
			params.setTeleportedModeSpeed( 2.0 );
		}
		return routeBushwhackingLeg(person, leg, fromCoord, toCoord, depTime, dpLinkId, arLinkId, pf, params);
	}

	static double routeBushwhackingLeg(Person person, Leg leg, Coord fromCoord, Coord toCoord, double depTime,
			Id<Link> dpLinkId, Id<Link> arLinkId, PopulationFactory pf, ModeRoutingParams params) {
		// I don't think that it makes sense to use a RoutingModule for this, since that again makes assumptions about how to
		// map facilities, and if you follow through to the teleportation routers one even finds activity wrappers, which is yet another
		// complication which I certainly don't want here.  kai, dec'15

		// dpLinkId, arLinkId need to be in Route for lots of code to function.   So I am essentially putting in the "street address"
		// for completeness. Note that if we are walking to a parked car, this can be different from the car link id!!  kai, dec'15

		// make simple assumption about distance and walking speed
		double dist = CoordUtils.calcEuclideanDistance(fromCoord,toCoord);

		// create an empty route, but with realistic travel time
		Route route =pf.getRouteFactories().createRoute(Route.class, dpLinkId, arLinkId ); 

		Gbl.assertNotNull( params );
		double beelineDistanceFactor = params.getBeelineDistanceFactor();
		double networkTravelSpeed = params.getTeleportedModeSpeed();

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
	public String toString() {
		return "[NetworkRoutingModule: mode="+this.mode+"]";
	}


	/*package (Tests)*/ double routeLeg(Person person, Leg leg, Link fromLink, Link toLink, double depTime) {
		double travTime;

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link


		if (toLink != fromLink) { // (a "true" route)

			Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, leg.getMode());
			Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehicleId);
			Path path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime, person, vehicle);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");

			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime((int) path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, 1.0,1.0,this.filteredNetwork ) );
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
