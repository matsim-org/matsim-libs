/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingDriverRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.hitchiking.population;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;
import playground.thibautd.hitchiking.HitchHikingConstants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author thibautd
 */
public class HitchHikingDriverRoute extends AbstractRoute {
	private static final String PU_DO_SEP = "|";
	private static final String DO_DO_SEP = ";";

	private Id<Link> puLinkId = null;
	private List<Id<Link>> doLinksIds = null;

	public HitchHikingDriverRoute(
			final Id<Link> startLinkId,
			final Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
	}

	public HitchHikingDriverRoute(
			final Id<Link> startLinkId,
			final Id<Link> endLinkId,
			final Id<Link> puLinkId,
			final List<Id<Link>> doLinksIds) {
		this( startLinkId , endLinkId );
		this.puLinkId = puLinkId;
		this.doLinksIds = new ArrayList<>( doLinksIds );
	}

	@Override
	public void setRouteDescription(
			final String routeDescription) {
		String[] puAndDos = routeDescription.trim().split( PU_DO_SEP );
		puLinkId = Id.create( puAndDos[0].trim() , Link.class );

		doLinksIds = new ArrayList<>();
		for (String id : puAndDos[1].split( DO_DO_SEP )) {
			doLinksIds.add( Id.create( id , Link.class ) );
		}
	}

	@Override
	public String getRouteDescription() {
		StringBuffer b = new StringBuffer();

		b.append( puLinkId );
		b.append( PU_DO_SEP );
		
		Iterator<Id<Link>> ids = doLinksIds.iterator();
		b.append( ids.next() );
		while (ids.hasNext()) {
			b.append( DO_DO_SEP );
			b.append( ids.next() );
		}

		return b.toString();
	}

	@Override
	public String getRouteType() {
		return HitchHikingConstants.DRIVER_MODE;
	}

	public Id<Link> getPickUpLinkId() {
		return puLinkId;
	}

	/**
	 * @return the list of drop-off points, ordered from
	 * the prefered one to the worst one.
	 */
	public List<Id<Link>> getDropOffLinksIds() {
		return doLinksIds;
	}

	@Override
	public HitchHikingDriverRoute clone() {
		return new HitchHikingDriverRoute(
				getStartLinkId(),
				getEndLinkId(),
				getPickUpLinkId(),
				getDropOffLinksIds());
	}
}

