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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author thibautd
 */
public class SubtourModeChoiceConfigGroupTest {

	@Test
	public void testModes() throws Exception {
		SubtourModeChoiceConfigGroup group = new SubtourModeChoiceConfigGroup();
		final String msg = "Wrong values for modes";
		final String msgString = "Wrong string representation";

		group.addParam( 
			SubtourModeChoiceConfigGroup.MODES,
			"foot,car,balloon" );
		assertArrayEquals(
				msg,
				new String[]{"foot", "car", "balloon" },
				group.getModes() );
		assertEquals(
				msgString,
				"foot,car,balloon",
				group.getValue(
					SubtourModeChoiceConfigGroup.MODES ));

		group.addParam( 
			SubtourModeChoiceConfigGroup.MODES,
			"    rocket,bike" );
		assertArrayEquals(
				msg,
				new String[]{"rocket", "bike" },
				group.getModes() );
		assertEquals(
				msgString,
				"rocket,bike",
				group.getValue(
					SubtourModeChoiceConfigGroup.MODES ));

		group.addParam( 
			SubtourModeChoiceConfigGroup.MODES,
			"skateboard       ,      		  unicycle      " );
		assertArrayEquals(
				msg,
				new String[]{"skateboard", "unicycle" },
				group.getModes() );
		assertEquals(
				msgString,
				"skateboard,unicycle",
				group.getValue(
					SubtourModeChoiceConfigGroup.MODES ));
	}

	@Test
	public void testChainBasedModes() throws Exception {
		SubtourModeChoiceConfigGroup group = new SubtourModeChoiceConfigGroup();
		final String msg = "Wrong values for chain based modes";
		final String msgString = "Wrong string representation";

		group.addParam( 
			SubtourModeChoiceConfigGroup.CHAINBASEDMODES,
			"foot,car,balloon" );
		assertArrayEquals(
				msg,
				new String[]{"foot", "car", "balloon" },
				group.getChainBasedModes() );
		assertEquals(
				msgString,
				"foot,car,balloon",
				group.getValue(
				SubtourModeChoiceConfigGroup.CHAINBASEDMODES));

		group.addParam( 
			SubtourModeChoiceConfigGroup.CHAINBASEDMODES,
			"    rocket,bike" );
		assertArrayEquals(
				msg,
				new String[]{"rocket", "bike" },
				group.getChainBasedModes() );
		assertEquals(
				msgString,
				"rocket,bike",
				group.getValue(
					SubtourModeChoiceConfigGroup.CHAINBASEDMODES));

		group.addParam( 
			SubtourModeChoiceConfigGroup.CHAINBASEDMODES,
			"skateboard       ,      		  unicycle      " );
		assertArrayEquals(
				msg,
				new String[]{"skateboard", "unicycle" },
				group.getChainBasedModes() );
		assertEquals(
				msgString,
				"skateboard,unicycle",
				group.getValue(
					SubtourModeChoiceConfigGroup.CHAINBASEDMODES));
	}

	@Test
	public void testCarAvail() throws Exception {
		SubtourModeChoiceConfigGroup group = new SubtourModeChoiceConfigGroup();

		assertFalse(
				"default value is not backward compatible",
				group.considerCarAvailability() );

		group.addParam( 
			SubtourModeChoiceConfigGroup.CARAVAIL,
			"true" );	
		assertTrue( "the value was not set to true" , group.considerCarAvailability() );

		group.addParam( 
			SubtourModeChoiceConfigGroup.CARAVAIL,
			"false" );	
		assertFalse( "the value was not set to false" , group.considerCarAvailability() );
	}
}

