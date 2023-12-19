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
public class LinkStatsConfigGroupTest {

	@Test
	void testWriteLinkStatsInterval() {
		LinkStatsConfigGroup cg = new LinkStatsConfigGroup();
		// test initial value
		Assertions.assertEquals(50, cg.getWriteLinkStatsInterval());
		Assertions.assertEquals("50", cg.getValue("writeLinkStatsInterval"));
		// test setting with setMobsim
		cg.setWriteLinkStatsInterval(4);
		Assertions.assertEquals(4, cg.getWriteLinkStatsInterval());
		Assertions.assertEquals("4", cg.getValue("writeLinkStatsInterval"));
		// test setting with addParam
		cg.addParam("writeLinkStatsInterval", "2");
		Assertions.assertEquals(2, cg.getWriteLinkStatsInterval());
		Assertions.assertEquals("2", cg.getValue("writeLinkStatsInterval"));
	}

	@Test
	void testGetParams_writeLinkStatsInterval() {
		LinkStatsConfigGroup cg = new LinkStatsConfigGroup();
		Assertions.assertNotNull(cg.getParams().get("writeLinkStatsInterval"));
	}

	@Test
	void testWriteAverageOverIterations() {
		LinkStatsConfigGroup cg = new LinkStatsConfigGroup();
		// test initial value
		Assertions.assertEquals(5, cg.getAverageLinkStatsOverIterations());
		Assertions.assertEquals("5", cg.getValue("averageLinkStatsOverIterations"));
		// test setting with setMobsim
		cg.setAverageLinkStatsOverIterations(4);
		Assertions.assertEquals(4, cg.getAverageLinkStatsOverIterations());
		Assertions.assertEquals("4", cg.getValue("averageLinkStatsOverIterations"));
		// test setting with addParam
		cg.addParam("averageLinkStatsOverIterations", "2");
		Assertions.assertEquals(2, cg.getAverageLinkStatsOverIterations());
		Assertions.assertEquals("2", cg.getValue("averageLinkStatsOverIterations"));
	}

	@Test
	void testGetParams_averageLinkStatsOverIterations() {
		LinkStatsConfigGroup cg = new LinkStatsConfigGroup();
		Assertions.assertNotNull(cg.getParams().get("averageLinkStatsOverIterations"));
	}
	
}
