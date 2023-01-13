/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimResourceTest.java
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

package org.matsim.core.gbl;

import static org.junit.Assert.assertEquals;

import java.awt.Image;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class MatsimResourceTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();


	@Test public final void testGetAsImage() {
		final Image logo = MatsimResource.getAsImage("matsim_logo_transparent.png");

		// verify that the correct image was correctly loaded by testing its dimension
		assertEquals(256, logo.getWidth(null));
		assertEquals(48, logo.getHeight(null));
	}

}
