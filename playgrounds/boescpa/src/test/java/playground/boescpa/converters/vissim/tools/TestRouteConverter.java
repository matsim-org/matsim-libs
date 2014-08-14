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

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import playground.boescpa.converters.vissim.ConvEvents2Anm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Provides tests for RouteConverter.
 *
 * @author boescpa
 */
public class TestRouteConverter {

	ConvEvents2Anm.RouteConverter routeConverter;
	HashMap<Id, Id[]> networkKey = new HashMap<Id, Id[]>();

	@Before
	public void prepare() {
		// Provide dummy trips
		this.routeConverter = new AbstractRouteConverter() {
			@Override
			protected List<Trip> routes2Trips(String path2RouteFile, String path2OrigNetwork, String path2VissimZoneShp) {
				List<Trip> trips = new ArrayList<Trip>();
				Trip trip1 = new Trip(new IdImpl("t100"), 100.0);
				trip1.links.add(new IdImpl(1l));
				trip1.links.add(new IdImpl(2l));
				trips.add(trip1);
				Trip trip2 = new Trip(new IdImpl("t200"), 200.0);
				trip2.links.add(new IdImpl(2l));
				trip2.links.add(new IdImpl(3l));
				trips.add(trip2);
				return trips;
			}
		};
		// Provide dummy key set
		networkKey.put(new IdImpl(1l), new Id[]{new IdImpl(10l), new IdImpl(11l), new IdImpl(12l)});
		networkKey.put(new IdImpl(2l), new Id[]{new IdImpl(20l), new IdImpl(21l), new IdImpl(22l)});
		networkKey.put(new IdImpl(3l), new Id[]{new IdImpl(22l), new IdImpl(30l)});
	}

	@Test
	public void testTrips2SimpleRoutes() {
		HashMap<Id, Long[]> simpleRoutes = this.routeConverter.convert(this.networkKey, "", "", "");
		Long[] trip1 = simpleRoutes.get(new IdImpl("t100"));
		Long[] trip2 = simpleRoutes.get(new IdImpl("t200"));
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
