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
package playground.thibautd.socnetsim.population;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * @author thibautd
 */
public class DriverRoute implements GenericRoute , NetworkRoute {
	private final NetworkRoute netRoute;
	private final Set<Id> passengers = new TreeSet<Id>();

	public DriverRoute(final Id startLink , final Id endLink) {
		netRoute = new LinkNetworkRouteImpl( startLink , endLink );
	}

	public DriverRoute(final NetworkRoute r, final Collection<Id> passengers) {
		netRoute = r != null ? (NetworkRoute) r.clone() : new LinkNetworkRouteImpl( null , null );
		this.passengers.addAll( passengers );
	}

	public Collection<Id> getPassengersIds() {
		return Collections.unmodifiableSet(passengers);
	}

	public void addPassenger(final Id passenger) {
		passengers.add( passenger );
	}

	public void addPassengers(final Collection<Id> ps) {
		passengers.addAll( ps );
	}

	public boolean removePassenger(final Id passenger) {
		return passengers.remove( passenger );
	}

	public boolean removePassengers(final Collection<Id> toRemove) {
		return this.passengers.removeAll( toRemove );
	}

	public void setPassengerIds(final Collection<Id> ps) {
		passengers.clear();
		passengers.addAll( ps );
	}

	@Override
	public double getDistance() {
		return netRoute.getDistance();
	}

	@Override
	public void setLinkIds(
			final Id startLinkId,
			final List<Id> linkIds,
			final Id endLinkId) {
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
	public List<Id> getLinkIds() {
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
			final Id fromLinkId,
			final Id toLinkId) {
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
	public Id getStartLinkId() {
		return netRoute.getStartLinkId();
	}

	@Override
	public Id getEndLinkId() {
		return netRoute.getEndLinkId();
	}

	@Override
	public void setStartLinkId(final Id linkId) {
		netRoute.setStartLinkId(linkId);
	}

	@Override
	public void setEndLinkId(final Id linkId) {
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
			final Id startLinkId,
			final String routeDescription,
			final Id endLinkId) {
		String[] info = routeDescription.trim().split( " " );
		String[] ps = info[0].split( "," );

		for (String p : ps) {
			passengers.add( new IdImpl( p ) );
		}

		List<Id> ls = new ArrayList<Id>();
		for (int i=1; i < info.length; i++) {
			ls.add( new IdImpl( info[i] ) );
		}
		setLinkIds( startLinkId , ls , endLinkId );
	}

	@Override
	public String getRouteDescription() {
		StringBuffer d = new StringBuffer();
		Iterator<Id> ps = passengers.iterator();
		if (ps.hasNext()) d.append( ps.next() );

		while (ps.hasNext()) {
			d.append( "," );
			d.append( ps.next() );
		}

		for (Id l : getLinkIds()) {
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

