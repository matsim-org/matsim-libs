/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingRoute.java
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
package eu.eunoiaproject.bikesharing.framework.scenario;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;

/**
 * Defines a route for bike sharing legs.
 * @author thibautd
 */
public class BikeSharingRoute implements GenericRoute, NetworkRoute {
	private final NetworkRoute routeDelegate;
	private Id<BikeSharingFacility> originStation;
	private Id<BikeSharingFacility> destinationStation;

	public BikeSharingRoute(
			final Id<Link> oLink,
			final Id<Link> dLink) {
		this( new LinkNetworkRouteImpl(
					oLink,
					dLink ) ,
				null,
				null );
	}

	public BikeSharingRoute(
			final Facility originStation,
			final Facility destinationStation) {
		this( new LinkNetworkRouteImpl(
					originStation.getLinkId(),
					destinationStation.getLinkId() ) ,
				originStation.getId(),
				destinationStation.getId() );
	}

	public BikeSharingRoute(
			final NetworkRoute routeDelegate,
			final Id<BikeSharingFacility> originStation,
			final Id<BikeSharingFacility> destinationStation) {
		this.routeDelegate = routeDelegate;
		this.originStation = originStation;
		this.destinationStation = destinationStation;
	}

	@Override
	public BikeSharingRoute clone() {
		return new BikeSharingRoute(
				(NetworkRoute) routeDelegate.clone(),
				originStation,
				destinationStation);
	}

	@Override
	@Deprecated
	public double getDistance() {
		return routeDelegate.getDistance();
	}

	@Override
	public void setDistance(double distance) {
		routeDelegate.setDistance(distance);
	}

	@Override
	public double getTravelTime() {
		return routeDelegate.getTravelTime();
	}

	@Override
	public void setTravelTime(double travelTime) {
		routeDelegate.setTravelTime(travelTime);
	}

	@Override
	public Id<Link> getStartLinkId() {
		return routeDelegate.getStartLinkId();
	}

	@Override
	public Id<Link> getEndLinkId() {
		return routeDelegate.getEndLinkId();
	}

	@Override
	public void setStartLinkId(Id<Link> linkId) {
		routeDelegate.setStartLinkId(linkId);
	}

	@Override
	public void setEndLinkId(Id<Link> linkId) {
		routeDelegate.setEndLinkId(linkId);
	}

	public Id<BikeSharingFacility> getOriginStation() {
		return originStation;
	}

	public Id<BikeSharingFacility> getDestinationStation() {
		return destinationStation;
	}

	public void setOriginStation( final Id<BikeSharingFacility> originStation ) {
		this.originStation = originStation;
	}

	public void setDestinationStation( final Id<BikeSharingFacility> destinationStation ) {
		this.destinationStation = destinationStation;
	}

	// /////////////////////////////////////////////////////////////////////////
	// generic route (necessary for IO)
	@Override
	public void setRouteDescription(
			final Id<Link> startLinkId,
			final String routeDescription,
			final Id<Link> endLinkId) {
		this.routeDelegate.setStartLinkId( startLinkId );
		this.routeDelegate.setEndLinkId( endLinkId );
		final String[] stations = routeDescription.trim().split( " " );
		if ( stations.length < 2 ) throw new IllegalArgumentException( routeDescription );
		this.originStation = Id.create( stations[ 0 ], BikeSharingFacility.class );
		this.destinationStation = Id.create( stations[ 1 ], BikeSharingFacility.class );

		final List<Id<Link>> links = new ArrayList<Id<Link>>( stations.length - 2 );
		for ( int i=2; i < stations.length; i++ ) {
			links.add( Id.createLinkId( stations[ i ] ) );
		}
		setLinkIds( startLinkId , links , endLinkId );
	}

	@Override
	public String getRouteDescription() {
		final StringBuffer buff = new StringBuffer( originStation+" "+destinationStation );
		for ( Id<Link> id : getLinkIds() ) buff.append( " "+id );
		return buff.toString();
	}

	@Override
	public String getRouteType() {
		return "bikeSharing";
	}

	@Override
	public void setLinkIds( Id<Link> startLinkId , List<Id<Link>> linkIds , Id<Link> endLinkId ) {
		routeDelegate.setLinkIds( startLinkId, linkIds, endLinkId );	
	}

	@Override
	public void setTravelCost( double travelCost ) {
		routeDelegate.setTravelCost( travelCost );
	}

	@Override
	public double getTravelCost() {
		return routeDelegate.getTravelCost();
	}

	@Override
	public List<Id<Link>> getLinkIds() {
		return routeDelegate.getLinkIds();
	}

	@Override
	public NetworkRoute getSubRoute( Id<Link> fromLinkId , Id<Link> toLinkId ) {
		return routeDelegate.getSubRoute( fromLinkId, toLinkId );
	}

	@Override
	public void setVehicleId( Id<Vehicle> vehicleId ) {
		routeDelegate.setVehicleId( vehicleId );
		
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return routeDelegate.getVehicleId();
	}
}

