/* *********************************************************************** *
 * project: org.matsim.*
 * AccessEgressNetworkBasedTeleportationRoute.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;

import org.matsim.contrib.socnetsim.utils.CollectionUtils;

/**
 * @author thibautd
 */
public class AccessEgressNetworkBasedTeleportationRoute implements GenericRoute , NetworkRoute {
	private double accessTime = Double.NaN;
	private double egressTime = Double.NaN;
	private double linkTime = Double.NaN;

	private double distance = Double.NaN;

	private Id<Link> startLinkId = null;
	private Id<Link> endLinkId = null;

	private List<Id<Link>> links = Collections.emptyList();

	private Id<Vehicle> vehicle = null;

	public AccessEgressNetworkBasedTeleportationRoute() {
	}
	
	public AccessEgressNetworkBasedTeleportationRoute(
			final Id<Link> startLinkId,
			final Id<Link> endLinkId ) {
		this.startLinkId = startLinkId;
		this.endLinkId = endLinkId;
	}

	@Override
	public double getDistance() {
		return distance;
	}

	@Override
	public void setDistance(double distance) {
		this.distance = distance;
	}

	@Override
	public double getTravelTime() {
		return accessTime + linkTime + egressTime;
	}

	@Override
	public void setTravelTime(double travelTime) {
		// do not set: computed from parts
	}

	@Override
	public Id<Link> getStartLinkId() {
		return startLinkId;
	}

	@Override
	public Id<Link> getEndLinkId() {
		return endLinkId;
	}

	@Override
	public void setStartLinkId(Id<Link> linkId) {
		this.startLinkId = linkId;
	}

	@Override
	public void setEndLinkId(Id<Link> linkId) {
		this.endLinkId = linkId;
	}

	@Override
	public void setRouteDescription(
			final Id<Link> startLinkId,
			final String routeDescription,
			final Id<Link> endLinkId) {
		setStartLinkId( startLinkId );

		final JSONObject json = new JSONObject( new JSONTokener( routeDescription ) );
		
		// do not use the json getDouble(), because we use the java string representation,
		// which allows non-finite values to be written
		setAccessTime( Double.parseDouble( (String) json.get( "accessTime" ) ) ); 
		setEgressTime( Double.parseDouble( (String) json.get( "egressTime" ) ) );
		setLinkTime( Double.parseDouble( (String) json.get( "onLinksTime" ) ) );

		final JSONArray linksArray = json.getJSONArray( "links" );

		final List<Id<Link>> ids = new ArrayList<Id<Link>>( linksArray.length() );
		for ( int i=0; i < linksArray.length(); i++ ) {
			ids.add( Id.create( linksArray.getString( i ), Link.class ) );
		}

		this.links = ids;

		setEndLinkId( endLinkId );
	}

	public List<Id<Link>> getLinks() {
		return Collections.unmodifiableList( links );
	}

	public void setLinks( final List<Id<Link>> newLinks ) {
		this.links = new ArrayList<Id<Link>>( newLinks );
	}

	@Override
	public String getRouteDescription() {
		final JSONObject json = new JSONObject( );
		
		json.put( "accessTime" , ""+getAccessTime() );
		json.put( "egressTime" , ""+getEgressTime() );
		json.put( "onLinksTime" , ""+getLinkTime() );

		json.put( "links" , new JSONArray( CollectionUtils.toString( links ) ) );

		return json.toString( );
	}

	@Override
	public String getRouteType() {
		return "accessEgressNetworkBased";
	}

	public double getAccessTime() {
		return accessTime;
	}

	public void setAccessTime(double accessTime) {
		this.accessTime = accessTime;
	}

	public double getEgressTime() {
		return egressTime;
	}

	public void setEgressTime(double egressTime) {
		this.egressTime = egressTime;
	}

	public double getLinkTime() {
		return linkTime;
	}

	public void setLinkTime(double linkTime) {
		this.linkTime = linkTime;
	}

	@Override
	public AccessEgressNetworkBasedTeleportationRoute clone() {
		final AccessEgressNetworkBasedTeleportationRoute clone =
				new AccessEgressNetworkBasedTeleportationRoute();

		// not the most efficient way, but the safest facing refactorings.
		clone.setRouteDescription(
				getStartLinkId(),
				getRouteDescription(),
				getEndLinkId() );
		clone.setDistance( getDistance() );

		return clone;
	}

	@Override
	public void setLinkIds( Id<Link> startLinkId , List<Id<Link>> linkIds , Id<Link> endLinkId ) {
		setStartLinkId( startLinkId );
		setLinks( linkIds );
		setEndLinkId( endLinkId );
		
	}

	@Override
	public void setTravelCost( double travelCost ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getTravelCost() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Id<Link>> getLinkIds() {
		return links;
	}

	@Override
	public NetworkRoute getSubRoute( Id<Link> fromLinkId , Id<Link> toLinkId ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setVehicleId( final Id<Vehicle> vehicleId ) {
		this.vehicle = vehicleId;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicle;
	}
}

