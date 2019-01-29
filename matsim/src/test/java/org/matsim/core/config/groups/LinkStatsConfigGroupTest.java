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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author mrieser
 */
public class LinkStatsConfigGroupTest {

	@Test
	public void testWriteLinkStatsInterval() {
		LinkStatsConfigGroup cg = new LinkStatsConfigGroup();
		// test initial value
		Assert.assertEquals(10, cg.getWriteLinkStatsInterval());
		Assert.assertEquals("10", cg.getValue("writeLinkStatsInterval"));
		// test setting with setMobsim
		cg.setWriteLinkStatsInterval(4);
		Assert.assertEquals(4, cg.getWriteLinkStatsInterval());
		Assert.assertEquals("4", cg.getValue("writeLinkStatsInterval"));
		// test setting with addParam
		cg.addParam("writeLinkStatsInterval", "2");
		Assert.assertEquals(2, cg.getWriteLinkStatsInterval());
		Assert.assertEquals("2", cg.getValue("writeLinkStatsInterval"));
	}

	@Test
	public void testGetParams_writeLinkStatsInterval() {
		LinkStatsConfigGroup cg = new LinkStatsConfigGroup();
		Assert.assertNotNull(cg.getParams().get("writeLinkStatsInterval"));
	}
	
	@Test
	public void testWriteAverageOverIterations() {
		LinkStatsConfigGroup cg = new LinkStatsConfigGroup();
		// test initial value
		Assert.assertEquals(5, cg.getAverageLinkStatsOverIterations());
		Assert.assertEquals("5", cg.getValue("averageLinkStatsOverIterations"));
		// test setting with setMobsim
		cg.setAverageLinkStatsOverIterations(4);
		Assert.assertEquals(4, cg.getAverageLinkStatsOverIterations());
		Assert.assertEquals("4", cg.getValue("averageLinkStatsOverIterations"));
		// test setting with addParam
		cg.addParam("averageLinkStatsOverIterations", "2");
		Assert.assertEquals(2, cg.getAverageLinkStatsOverIterations());
		Assert.assertEquals("2", cg.getValue("averageLinkStatsOverIterations"));
	}
	
	@Test
	public void testGetParams_averageLinkStatsOverIterations() {
		LinkStatsConfigGroup cg = new LinkStatsConfigGroup();
		Assert.assertNotNull(cg.getParams().get("averageLinkStatsOverIterations"));
	}
	
}
