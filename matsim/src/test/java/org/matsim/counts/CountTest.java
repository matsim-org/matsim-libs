/* *********************************************************************** *
 * project: org.matsim.*
 * CountTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.counts;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.testcases.MatsimTestUtils;

public class CountTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private Counts<Link> counts;

	@Before
	public void setUp() throws Exception {
		this.counts = new Counts<>();
	}

	@Test public void testCreateVolume() {
		Count count = counts.createAndAddCount(Id.create(0, Link.class), "1");
		Volume volume = count.createVolume(1, 100.0);
		assertTrue("Creation and initialization of volume failed", volume.getHourOfDayStartingWithOne()==1);
		assertTrue("Creation and initialization of volume failed", volume.getValue()==100.0);
	}

	@Test public void testGetVolume() {
		Count count = counts.createAndAddCount(Id.create(0, Link.class), "1");
		count.createVolume(1, 100.0);
		assertTrue("Getting volume failed", count.getVolume(1).getValue() == 100.0);
	}

	@Test public void testGetVolumes() {
		Count count = counts.createAndAddCount(Id.create(0, Link.class), "1");
		count.createVolume(1, 100.0);

		Iterator<Volume> vol_it = count.getVolumes().values().iterator();
		while (vol_it.hasNext()) {
			Volume v = vol_it.next();
			assertTrue("Getting volumes failed", v.getValue() == 100.0);
		}

	}
}
