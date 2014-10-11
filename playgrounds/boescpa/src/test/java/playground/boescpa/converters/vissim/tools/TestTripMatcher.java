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

import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;

import playground.boescpa.converters.vissim.ConvEvents;
import playground.boescpa.converters.vissim.tools.AbstractRouteConverter.Trip;

/**
 * Provides tests for TripMatcher.
 *
 * @author boescpa
 */
public class TestTripMatcher {

	ConvEvents.TripMatcher tripMatcher = new TripMatcher();
	HashMap<Id<Trip>, Long[]> msTrips = null;
	HashMap<Id<Trip>, Long[]> amTrips = null;

	@Before

	public void prepare() {
		msTrips = new HashMap<>();
		amTrips = new HashMap<>();

		msTrips.put(Id.create("11", Trip.class), new Long[]{1l,2l,3l,4l});
		msTrips.put(Id.create("12", Trip.class), new Long[]{2l,4l});

		amTrips.put(Id.create("21", Trip.class), new Long[]{2l,3l,4l,5l});
		amTrips.put(Id.create("22", Trip.class), new Long[]{2l,3l,4l});
		amTrips.put(Id.create("23", Trip.class), new Long[]{1l,2l,3l,4l});
	}

	@Test
	public void testMatchTrips() {
		HashMap<Id<Trip>, Integer> results = tripMatcher.matchTrips(msTrips, amTrips);
		Assert.assertEquals(0, results.get(Id.create("21", Trip.class)).intValue());
		Assert.assertEquals(1, results.get(Id.create("22", Trip.class)).intValue());
		Assert.assertEquals(1, results.get(Id.create("23", Trip.class)).intValue());
	}

}
