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

import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.testcases.MatsimTestCase;

public class MeisterkConfigGroupTest extends MatsimTestCase {

	private static MeisterkConfigGroup meisterk;
	
	protected void setUp() throws Exception {
		super.setUp();
		meisterk = new MeisterkConfigGroup();
	}

	public void testMeisterkConfigGroup() {

		EnumSet<BasicLeg.Mode> expected = EnumSet.of(BasicLeg.Mode.car, BasicLeg.Mode.bike);
		this.runTest(expected);
	}
	
	public void testAddParam() {

		meisterk.addParam(MeisterkConfigGroup.MeisterkConfigParameter.CHAIN_BASED_MODES.getParameterName(), "miv");
		EnumSet<BasicLeg.Mode> expected = EnumSet.of(BasicLeg.Mode.miv);
		this.runTest(expected);

	}

	public void testAddEmptyParam() {

		meisterk.addParam(MeisterkConfigGroup.MeisterkConfigParameter.CHAIN_BASED_MODES.getParameterName(), "");
		EnumSet<BasicLeg.Mode> expected = EnumSet.noneOf(BasicLeg.Mode.class);
		this.runTest(expected);

	}
	
	private void runTest(EnumSet<BasicLeg.Mode> expected) {

		EnumSet<BasicLeg.Mode> actual = meisterk.getChainBasedModes();
		assertEquals(actual.size(), expected.size());
		Iterator<BasicLeg.Mode> modeIterator = actual.iterator();
		while (modeIterator.hasNext()) {
			BasicLeg.Mode mode = modeIterator.next();
			assertTrue(expected.contains(mode));
		}
		
	}
	
}
