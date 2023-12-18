/* *********************************************************************** *
 * project: org.matsim.*
 * SubtourModeChoiceConfigGroupTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.core.config.groups;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author thibautd
 */
public class SubtourModeChoiceConfigGroupTest {

	@Test
	void testModes() throws Exception {
		SubtourModeChoiceConfigGroup group = new SubtourModeChoiceConfigGroup();
		final String msg = "Wrong values for modes";
		final String msgString = "Wrong string representation";

		group.addParam( 
			SubtourModeChoiceConfigGroup.MODES,
			"foot,car,balloon" );
		assertArrayEquals(
				new String[]{"foot", "car", "balloon" },
				group.getModes(),
				msg );
		assertEquals(
				"foot,car,balloon",
				group.getValue(
					SubtourModeChoiceConfigGroup.MODES ),
				msgString);

		group.addParam( 
			SubtourModeChoiceConfigGroup.MODES,
			"    rocket,bike" );
		assertArrayEquals(
				new String[]{"rocket", "bike" },
				group.getModes(),
				msg );
		assertEquals(
				"rocket,bike",
				group.getValue(
					SubtourModeChoiceConfigGroup.MODES ),
				msgString);

		group.addParam( 
			SubtourModeChoiceConfigGroup.MODES,
			"skateboard       ,      		  unicycle      " );
		assertArrayEquals(
				new String[]{"skateboard", "unicycle" },
				group.getModes(),
				msg );
		assertEquals(
				"skateboard,unicycle",
				group.getValue(
					SubtourModeChoiceConfigGroup.MODES ),
				msgString);
	}

	@Test
	void testChainBasedModes() throws Exception {
		SubtourModeChoiceConfigGroup group = new SubtourModeChoiceConfigGroup();
		final String msg = "Wrong values for chain based modes";
		final String msgString = "Wrong string representation";

		group.addParam( 
			SubtourModeChoiceConfigGroup.CHAINBASEDMODES,
			"foot,car,balloon" );
		assertArrayEquals(
				new String[]{"foot", "car", "balloon" },
				group.getChainBasedModes(),
				msg );
		assertEquals(
				"foot,car,balloon",
				group.getValue(
				SubtourModeChoiceConfigGroup.CHAINBASEDMODES),
				msgString);

		group.addParam( 
			SubtourModeChoiceConfigGroup.CHAINBASEDMODES,
			"    rocket,bike" );
		assertArrayEquals(
				new String[]{"rocket", "bike" },
				group.getChainBasedModes(),
				msg );
		assertEquals(
				"rocket,bike",
				group.getValue(
					SubtourModeChoiceConfigGroup.CHAINBASEDMODES),
				msgString);

		group.addParam( 
			SubtourModeChoiceConfigGroup.CHAINBASEDMODES,
			"skateboard       ,      		  unicycle      " );
		assertArrayEquals(
				new String[]{"skateboard", "unicycle" },
				group.getChainBasedModes(),
				msg );
		assertEquals(
				"skateboard,unicycle",
				group.getValue(
					SubtourModeChoiceConfigGroup.CHAINBASEDMODES),
				msgString);
	}

	@Test
	void testCarAvail() throws Exception {
		SubtourModeChoiceConfigGroup group = new SubtourModeChoiceConfigGroup();

		assertFalse(
				group.considerCarAvailability(),
				"default value is not backward compatible" );

		group.addParam( 
			SubtourModeChoiceConfigGroup.CARAVAIL,
			"true" );	
		assertTrue( group.considerCarAvailability(), "the value was not set to true" );

		group.addParam( 
			SubtourModeChoiceConfigGroup.CARAVAIL,
			"false" );	
		assertFalse( group.considerCarAvailability(), "the value was not set to false" );
	}
}

