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

import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.testcases.MatsimTestUtils;

public class CountTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private Counts<Link> counts;

	@BeforeEach
	public void setUp() throws Exception {
		this.counts = new Counts<>();
	}

	@Test
	void testCreateVolume() {
		Count count = counts.createAndAddCount(Id.create(0, Link.class), "1");
		Volume volume = count.createVolume(1, 100.0);
		assertTrue(volume.getHourOfDayStartingWithOne()==1, "Creation and initialization of volume failed");
		assertTrue(volume.getValue()==100.0, "Creation and initialization of volume failed");
	}

	@Test
	void testGetVolume() {
		Count count = counts.createAndAddCount(Id.create(0, Link.class), "1");
		count.createVolume(1, 100.0);
		assertTrue(count.getVolume(1).getValue() == 100.0, "Getting volume failed");
	}

	@Test
	void testGetVolumes() {
		Count count = counts.createAndAddCount(Id.create(0, Link.class), "1");
		count.createVolume(1, 100.0);

		Iterator<Volume> vol_it = count.getVolumes().values().iterator();
		while (vol_it.hasNext()) {
			Volume v = vol_it.next();
			assertTrue(v.getValue() == 100.0, "Getting volumes failed");
		}

	}
}
