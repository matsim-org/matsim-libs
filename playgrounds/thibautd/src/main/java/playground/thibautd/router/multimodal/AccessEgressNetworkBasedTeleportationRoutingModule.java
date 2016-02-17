/* *********************************************************************** *
 * project: org.matsim.*
 * AccessEgressNetworkBasedTeleportationRoutingModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.router.multimodal;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uses network routing to create teleportation routes, taking into acount
 * crowfly access to links.
 * This is necessary when using network speeds for walk or bike: otherwise,
 * it is impossible to go from a
 * home linked to the car/walk/bike network to a train station linked
 * to the railway network.
 * @author thibautd
 */
public class AccessEgressNetworkBasedTeleportationRoutingModule implements RoutingModule {
	private static final Logger log =
		Logger.getLogger(AccessEgressNetworkBasedTeleportationRoutingModule.class);

	private final String mode;
	private final NetworkImpl accessibleNetwork;
	private final double crowFlyDistanceFactor;
	private final double crowFlySpeed;
	private final LeastCostPathCalculator routeAlgo;

	public AccessEgressNetworkBasedTeleportationRoutingModule(
			final String mode,
			final Network accessibleNetwork,
			final double crowFlyDistanceFactor,
			final double crowFlySpeed,
			final LeastCostPathCalculator routeAlgo) {
		this.mode = mode;
		this.accessibleNetwork = (NetworkImpl) accessibleNetwork;
		this.crowFlySpeed = crowFlySpeed;
		this.crowFlyDistanceFactor = crowFlyDistanceFactor;
		this.routeAlgo = routeAlgo;
	}

	@Override
	public List<Leg> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		final Link fromLink =
			accessibleNetwork.getLinks().containsKey( fromFacility.getLinkId() ) ?
				accessibleNetwork.getLinks().get( fromFacility.getLinkId() ) :
				NetworkUtils.getNearestLink(accessibleNetwork, fromFacility.getCoord());
		final Link toLink =
			accessibleNetwork.getLinks().containsKey( toFacility.getLinkId() ) ?
				accessibleNetwork.getLinks().get( toFacility.getLinkId() ) :
				NetworkUtils.getNearestLink(accessibleNetwork, toFacility.getCoord());
		
		if ( fromLink == null || toLink == null ) {
			log.error( "  ==>  null from/to link for person " + person.getId().toString() );
		}
		if ( fromLink == null ) throw new RuntimeException( "fromLink "+fromFacility.getLinkId()+" missing." );
		if ( toLink == null ) throw new RuntimeException( "toLink "+toFacility.getLinkId()+" missing." );

		final double accessDistance = getCrowFlyDistance( fromFacility , fromLink );
		final double accessTime = accessDistance / crowFlySpeed;
		final Path path = calcPath( fromLink , toLink , departureTime + accessTime , person );
		final double egressDistance = getCrowFlyDistance( toFacility , toLink );
		final double egressTime = egressDistance / crowFlySpeed;

		assert !Double.isNaN( accessDistance );
		assert !Double.isNaN( accessTime );
		assert !Double.isNaN( egressDistance );
		assert !Double.isNaN( egressTime );

		final AccessEgressNetworkBasedTeleportationRoute route =
			new AccessEgressNetworkBasedTeleportationRoute();
		route.setStartLinkId( fromFacility.getLinkId() );
		route.setEndLinkId( toFacility.getLinkId() );
		route.setAccessTime( accessTime );
		route.setLinkTime( path.travelTime );
		route.setDistance( accessDistance + calcDistance( path ) + egressDistance );
		route.setLinks( toIds( path.links ) ); 
		route.setEgressTime( egressTime );

		final Leg leg = new LegImpl( mode );
		leg.setRoute( route );
		leg.setTravelTime( route.getTravelTime() );

		return Collections.singletonList( leg );
	}

	private static double calcDistance(final Path path) {
		double d = 0;
		for ( final Link l : path.links ) d += l.getLength();
		return d;
	}

	private static List<Id<Link>> toIds(final List<Link> links) {
		final List<Id<Link>> ids = new ArrayList<Id<Link>>( links.size() );
		for ( final Link l : links ) ids.add( l.getId() );
		return ids;
	}

	private double getCrowFlyDistance(
			final Facility facility,
			final Link link) {
		if ( facility.getLinkId().equals( link.getId() ) ) return 0;
		return crowFlyDistanceFactor * CoordUtils.calcEuclideanDistance( facility.getCoord() , link.getCoord() );
	}

	private Path calcPath( final Link startLink , final Link endLink , final double depTime , final Person person ) {
		if ( startLink == endLink ) {
			// do not drive/walk around, if we stay on the same link
			// create an empty route == staying on place if toLink == endLink
			return new Path(
					Collections.<Node>emptyList(),
					Collections.<Link>emptyList(),
					0d, 0d );
		}

		return this.routeAlgo.calcLeastCostPath(startLink.getToNode(), endLink.getFromNode(), depTime, person, null);
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}
}

