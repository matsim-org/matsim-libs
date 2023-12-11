/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author mrieser
 */
public class CountsConfigGroupTest {

	@Test
	void testWriteCountsInterval() {
		CountsConfigGroup cg = new CountsConfigGroup();
		// test initial value
		Assertions.assertEquals(10, cg.getWriteCountsInterval());
		Assertions.assertEquals("10", cg.getValue("writeCountsInterval"));
		// test setting with setMobsim
		cg.setWriteCountsInterval(4);
		Assertions.assertEquals(4, cg.getWriteCountsInterval());
		Assertions.assertEquals("4", cg.getValue("writeCountsInterval"));
		// test setting with addParam
		cg.addParam("writeCountsInterval", "2");
		Assertions.assertEquals(2, cg.getWriteCountsInterval());
		Assertions.assertEquals("2", cg.getValue("writeCountsInterval"));
	}

	@Test
	void testGetParams_writeCountsInterval() {
		CountsConfigGroup cg = new CountsConfigGroup();
		Assertions.assertNotNull(cg.getParams().get("writeCountsInterval"));
	}

	@Test
	void testWriteAverageOverIterations() {
		CountsConfigGroup cg = new CountsConfigGroup();
		// test initial value
		Assertions.assertEquals(5, cg.getAverageCountsOverIterations());
		Assertions.assertEquals("5", cg.getValue("averageCountsOverIterations"));
		// test setting with setMobsim
		cg.setAverageCountsOverIterations(4);
		Assertions.assertEquals(4, cg.getAverageCountsOverIterations());
		Assertions.assertEquals("4", cg.getValue("averageCountsOverIterations"));
		// test setting with addParam
		cg.addParam("averageCountsOverIterations", "2");
		Assertions.assertEquals(2, cg.getAverageCountsOverIterations());
		Assertions.assertEquals("2", cg.getValue("averageCountsOverIterations"));
	}

	@Test
	void testGetParams_averageCountsOverIterations() {
		CountsConfigGroup cg = new CountsConfigGroup();
		Assertions.assertNotNull(cg.getParams().get("averageCountsOverIterations"));
	}
	
}
