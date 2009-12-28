/* *********************************************************************** *
 * project: org.matsim.*
 * MeisterkConfigGroupTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.org.matsim.config.groups;

import java.util.EnumSet;
import java.util.Iterator;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.testcases.MatsimTestCase;

public class MeisterkConfigGroupTest extends MatsimTestCase {

	private MeisterkConfigGroup meisterk;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.meisterk = new MeisterkConfigGroup();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.meisterk = null;
	}

	public void testMeisterkConfigGroup() {

		EnumSet<TransportMode> expected = EnumSet.of(TransportMode.car, TransportMode.bike);
		this.runTest(expected);
	}
	
	public void testAddParam() {

		this.meisterk.addParam(MeisterkConfigGroup.MeisterkConfigParameter.CHAIN_BASED_MODES.getParameterName(), "miv");
		EnumSet<TransportMode> expected = EnumSet.of(TransportMode.miv);
		this.runTest(expected);

	}

	public void testAddEmptyParam() {

		this.meisterk.addParam(MeisterkConfigGroup.MeisterkConfigParameter.CHAIN_BASED_MODES.getParameterName(), "");
		EnumSet<TransportMode> expected = EnumSet.noneOf(TransportMode.class);
		this.runTest(expected);

	}
	
	private void runTest(EnumSet<TransportMode> expected) {

		EnumSet<TransportMode> actual = this.meisterk.getChainBasedModes();
		assertEquals(actual.size(), expected.size());
		Iterator<TransportMode> modeIterator = actual.iterator();
		while (modeIterator.hasNext()) {
			TransportMode mode = modeIterator.next();
			assertTrue(expected.contains(mode));
		}
		
	}
	
}
