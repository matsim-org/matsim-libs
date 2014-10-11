/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.vissim.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.boescpa.converters.vissim.ConvEvents;
import playground.boescpa.converters.vissim.tools.AbstractRouteConverter.Trip;

/**
 * Provides tests for RouteConverter.
 *
 * @author boescpa
 */
public class TestRouteConverter {

	ConvEvents.RouteConverter routeConverter;
	HashMap<Id<Link>, Id<Node>[]> networkKey = new HashMap<>();

	@Before
	public void prepare() {
		// Provide dummy trips
		this.routeConverter = new AbstractRouteConverter() {
			@Override
			protected List<Trip> routes2Trips(String path2RouteFile, String path2OrigNetwork, String path2VissimZoneShp) {
				List<Trip> trips = new ArrayList<Trip>();
				Trip trip1 = new Trip(Id.create("t100", Trip.class), 0.0);
				trip1.links.add(Id.create(1l, Link.class));
				trip1.links.add(Id.create(2l, Link.class));
				trips.add(trip1);
				Trip trip2 = new Trip(Id.create("t200", Trip.class), 0.0);
				trip2.links.add(Id.create(2l, Link.class));
				trip2.links.add(Id.create(3l, Link.class));
				trips.add(trip2);
				return trips;
			}
		};
		// Provide dummy key set
		networkKey.put(Id.create(1l, Link.class), new Id[]{Id.create(10l, Node.class), Id.create(11l, Node.class), Id.create(12l, Node.class)});
		networkKey.put(Id.create(2l, Link.class), new Id[]{Id.create(20l, Node.class), Id.create(21l, Node.class), Id.create(22l, Node.class)});
		networkKey.put(Id.create(3l, Link.class), new Id[]{Id.create(22l, Node.class), Id.create(30l, Node.class)});
	}

	@Test
	public void testTrips2SimpleRoutes() {
		List<HashMap<Id<Trip>, Long[]>> simpleRoutesColl = this.routeConverter.convert(this.networkKey, "", "", "");
		HashMap<Id<Trip>, Long[]> simpleRoutes = simpleRoutesColl.get(0);
		Long[] trip1 = simpleRoutes.get(Id.create("t100", Trip.class));
		Long[] trip2 = simpleRoutes.get(Id.create("t200", Trip.class));
		Assert.assertEquals(trip1.length, 6);
		Assert.assertEquals(trip1[0], (Long)10l);
		Assert.assertEquals(trip1[2], (Long)12l);
		Assert.assertEquals(trip1[3], (Long)20l);
		Assert.assertEquals(trip1[5], (Long)22l);
		Assert.assertEquals(trip2.length, 4);
		Assert.assertEquals(trip2[0], (Long)20l);
		Assert.assertEquals(trip2[1], (Long)21l);
		Assert.assertEquals(trip2[2], (Long)22l);
		Assert.assertEquals(trip2[3], (Long)30l);
	}
}
