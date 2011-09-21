/* *********************************************************************** *
 * project: org.matsim.*
 * Controller2DTest.java
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
package playground.gregor.sim2d_v2.controller;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import playground.gregor.sim2d_v2.controller.Controller2D;

/**
 * @author laemmel
 * 
 */
public class Controller2DTest {
	private static final Logger log = Logger.getLogger(Controller2DTest.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testController2D() {
		String testEventsFile = this.utils.getOutputDirectory() + "ITERS/it.10/10.events.xml.gz";
		String configFile = this.utils.getInputDirectory() + "config2d.xml";
		Controller2D c = new Controller2D(new String[] { configFile });
		c.run();
		String refEventsFile = this.utils.getInputDirectory() + "10.events.xml.gz";
		log.info("comparing events files: ");
		log.info(refEventsFile);
		log.info(testEventsFile);
		int i = EventsFileComparator.compare(refEventsFile, testEventsFile);
		Assert.assertEquals("different events-files in iteration 10", 0, i);
	}
}
