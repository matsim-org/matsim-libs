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
import org.matsim.core.api.experimental.IdFactory;
import org.matsim.core.population.routes.GenericRoute;

/**
 * @author thibautd
 */
public class AccessEgressNetworkBasedTeleportationRoute implements GenericRoute /*, NetworkRoute */ {
	private double accessTime = Double.NaN;
	private double egressTime = Double.NaN;
	private double linkTime = Double.NaN;

	private double distance = Double.NaN;

	private Id startLinkId = null;
	private Id endLinkId = null;

	private List<Id> links = Collections.emptyList();
	private final IdFactory idFactory;

	public AccessEgressNetworkBasedTeleportationRoute(
			final IdFactory idFactory) {
		this.idFactory = idFactory;
	}
	
	public AccessEgressNetworkBasedTeleportationRoute(
			final IdFactory idFactory,
			final Id startLinkId,
			final Id endLinkId ) {
		this.startLinkId = startLinkId;
		this.endLinkId = endLinkId;
		this.idFactory = idFactory;
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
	public Id getStartLinkId() {
		return startLinkId;
	}

	@Override
	public Id getEndLinkId() {
		return endLinkId;
	}

	@Override
	public void setStartLinkId(Id linkId) {
		this.startLinkId = linkId;
	}

	@Override
	public void setEndLinkId(Id linkId) {
		this.endLinkId = linkId;
	}

	@Override
	public void setRouteDescription(
			final Id startLinkId,
			final String routeDescription,
			final Id endLinkId) {
		setStartLinkId( startLinkId );

		final JSONObject json = new JSONObject( new JSONTokener( routeDescription ) );
		
		setAccessTime( json.getDouble( "accessTime" ) );
		setEgressTime( json.getDouble( "egressTime" ) );
		setLinkTime( json.getDouble( "onLinksTime" ) );

		final JSONArray linksArray = json.getJSONArray( "links" );

		final List<Id> ids = new ArrayList<Id>( linksArray.length() );
		for ( int i=0; i < linksArray.length(); i++ ) {
			ids.add( idFactory.createId( linksArray.getString( i ) ) );
		}

		this.links = ids;

		setEndLinkId( endLinkId );
	}

	public List<Id> getLinks() {
		return Collections.unmodifiableList( links );
	}

	public void setLinks( final List<Id> newLinks ) {
		this.links = new ArrayList<Id>( newLinks );
	}

	@Override
	public String getRouteDescription() {
		final JSONObject json = new JSONObject( );
		
		json.put( "accessTime" , getAccessTime() );
		json.put( "egressTime" , getEgressTime() );
		json.put( "onLinksTime" , getLinkTime() );

		json.put( "links" , new JSONArray( links ) );

		return json.toString();
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
				new AccessEgressNetworkBasedTeleportationRoute( idFactory );

		// not the most efficient way, but the safest facing refactorings.
		clone.setRouteDescription(
				getStartLinkId(),
				getRouteDescription(),
				getEndLinkId() );

		return clone;
	}
}

