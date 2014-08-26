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
import playground.boescpa.converters.vissim.ConvEvents;

import java.util.HashMap;

/**
 * Provides tests for TripMatcher.
 *
 * @author boescpa
 */
public class TestTripMatcher {

	ConvEvents.TripMatcher tripMatcher = new TripMatcher();
	HashMap<Id, Long[]> msTrips = null;
	HashMap<Id, Long[]> amTrips = null;

	@Before

	public void prepare() {
		msTrips = new HashMap<Id, Long[]>();
		amTrips = new HashMap<Id, Long[]>();

		msTrips.put(new IdImpl(11l), new Long[]{1l,2l,3l,4l});
		msTrips.put(new IdImpl(12l), new Long[]{2l,4l});

		amTrips.put(new IdImpl(21l), new Long[]{2l,3l,4l,5l});
		amTrips.put(new IdImpl(22l), new Long[]{2l,3l,4l});
		amTrips.put(new IdImpl(23l), new Long[]{1l,2l,3l,4l});
	}

	@Test
	public void testMatchTrips() {
		HashMap<Id, Integer> results = tripMatcher.matchTrips(msTrips, amTrips);
		Assert.assertEquals(0, results.get(new IdImpl(21l)).intValue());
		Assert.assertEquals(1, results.get(new IdImpl(22l)).intValue());
		Assert.assertEquals(1, results.get(new IdImpl(23l)).intValue());
	}

}
