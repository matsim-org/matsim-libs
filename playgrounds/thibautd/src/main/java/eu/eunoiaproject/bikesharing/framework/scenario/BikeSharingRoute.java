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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;

/**
 * Defines a route for bike sharing legs.
 * @author thibautd
 */
public class BikeSharingRoute implements GenericRoute {
	private final Route routeDelegate;
	private Id<ActivityFacility> originStation;
	private Id<ActivityFacility> destinationStation;

	public BikeSharingRoute(
			final Facility originStation,
			final Facility destinationStation) {
		this( new GenericRouteImpl(
					originStation.getLinkId(),
					destinationStation.getLinkId() ) ,
				originStation.getId(),
				destinationStation.getId() );
	}

	public BikeSharingRoute(
			final Id<ActivityFacility> originStation,
			final Id<ActivityFacility> destinationStation) {
		this( new GenericRouteImpl( null , null ) ,
				originStation,
				destinationStation );
	}

	public BikeSharingRoute(
			final Route routeDelegate,
			final Id<ActivityFacility> originStation,
			final Id<ActivityFacility> destinationStation) {
		this.routeDelegate = routeDelegate;
		this.originStation = originStation;
		this.destinationStation = destinationStation;
	}

	@Override
	public BikeSharingRoute clone() {
		return new BikeSharingRoute(
				routeDelegate.clone(),
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

	public Id<ActivityFacility> getOriginStation() {
		return originStation;
	}

	public Id<ActivityFacility> getDestinationStation() {
		return destinationStation;
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
		if ( stations.length != 2 ) throw new IllegalArgumentException( routeDescription );
		this.originStation = Id.create( stations[ 0 ], ActivityFacility.class );
		this.destinationStation = Id.create( stations[ 1 ], ActivityFacility.class );
	}

	@Override
	public String getRouteDescription() {
		return originStation+" "+destinationStation;
	}

	@Override
	public String getRouteType() {
		return "bikeSharing";
	}
}

