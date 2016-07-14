/* *********************************************************************** *
 * project: org.matsim.*
 * DriverRoute.java
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
package org.matsim.contrib.socnetsim.jointtrips.population;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * @author thibautd
 */
public class DriverRoute implements Route , NetworkRoute {
	private final NetworkRoute netRoute;
	private final Set<Id<Person>> passengers = new TreeSet<Id<Person>>();

	public DriverRoute(final Id<Link> startLink , final Id<Link> endLink) {
		netRoute = new LinkNetworkRouteImpl( startLink , endLink );
	}

	public DriverRoute(final NetworkRoute r, final Collection<Id<Person>> passengers) {
		netRoute = r != null ? (NetworkRoute) r.clone() : new LinkNetworkRouteImpl( null , null );
		this.passengers.addAll( passengers );
	}

	public Collection<Id<Person>> getPassengersIds() {
		return Collections.unmodifiableSet(passengers);
	}

	public void addPassenger(final Id<Person> passenger) {
		passengers.add( passenger );
	}

	public void addPassengers(final Collection<Id<Person>> ps) {
		passengers.addAll( ps );
	}

	public boolean removePassenger(final Id<Person> passenger) {
		return passengers.remove( passenger );
	}

	public boolean removePassengers(final Collection<Id<Person>> toRemove) {
		return this.passengers.removeAll( toRemove );
	}

	public void setPassengerIds(final Collection<Id<Person>> ps) {
		passengers.clear();
		passengers.addAll( ps );
	}

	@Override
	public double getDistance() {
		return netRoute.getDistance();
	}

	@Override
	public void setLinkIds(
			final Id<Link> startLinkId,
			final List<Id<Link>> linkIds,
			final Id<Link> endLinkId) {
		netRoute.setLinkIds(startLinkId, linkIds, endLinkId);
	}

	@Override
	public void setTravelCost(double travelCost) {
		netRoute.setTravelCost(travelCost);
	}

	@Override
	public double getTravelCost() {
		return netRoute.getTravelCost();
	}

	@Override
	public List<Id<Link>> getLinkIds() {
		return netRoute.getLinkIds();
	}

	@Override
	public void setDistance(final double distance) {
		netRoute.setDistance(distance);
	}

	@Override
	public double getTravelTime() {
		return netRoute.getTravelTime();
	}

	@Override
	public NetworkRoute getSubRoute(
			final Id<Link> fromLinkId,
			final Id<Link> toLinkId) {
		return netRoute.getSubRoute(fromLinkId, toLinkId);
	}

	@Override
	public void setTravelTime(final double travelTime) {
		netRoute.setTravelTime(travelTime);
	}

	@Override
	public void setVehicleId(final Id vehicleId) {
		netRoute.setVehicleId(vehicleId);
	}

	@Override
	public Id getVehicleId() {
		return netRoute.getVehicleId();
	}

	@Override
	public Id<Link> getStartLinkId() {
		return netRoute.getStartLinkId();
	}

	@Override
	public Id<Link> getEndLinkId() {
		return netRoute.getEndLinkId();
	}

	@Override
	public void setStartLinkId(final Id<Link> linkId) {
		netRoute.setStartLinkId(linkId);
	}

	@Override
	public void setEndLinkId(final Id<Link> linkId) {
		netRoute.setEndLinkId(linkId);
	}

	@Override
	public DriverRoute clone() {
		DriverRoute c =
			new DriverRoute(
					netRoute,
					passengers);
		return c;
	}

	@Override
	public void setRouteDescription(
			final String routeDescription) {
		String[] info = routeDescription.trim().split( " " );
		String[] ps = info[0].split( "," );

		for (String p : ps) {
			passengers.add( Id.create( p , Person.class ) );
		}

		List<Id<Link>> ls = new ArrayList<Id<Link>>();
		for (int i=1; i < info.length; i++) {
			ls.add( Id.create( info[i] , Link.class ) );
		}
		setLinkIds( getStartLinkId() , ls , getEndLinkId() );
	}

	@Override
	public String getRouteDescription() {
		StringBuffer d = new StringBuffer();
		Iterator<Id<Person>> ps = passengers.iterator();
		if (ps.hasNext()) d.append( ps.next() );

		while (ps.hasNext()) {
			d.append( "," );
			d.append( ps.next() );
		}

		for (Id<Link> l : getLinkIds()) {
			d.append( " " );
			d.append( l );
		}

		return d.toString();
	}

	@Override
	public String getRouteType() {
		return "driver";
	}
	
	@Override
	public String toString() {
		return "[DriverRoute: delegate="+netRoute+"; passengers="+passengers+"]";
	}
}

