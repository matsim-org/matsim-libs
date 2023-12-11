/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.testcases.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author mrieser / Simunto GmbH
 */
public class LogCounterTest {

	private final static Logger LOG = LogManager.getLogger(LogCounterTest.class);

	@Test
	void testLogCounter_INFO() throws IOException {
		LogCounter counter = new LogCounter(Level.INFO);
		counter.activate();
		LOG.info("hello world - this is just a test");
		LOG.warn("hello world - this is just a test");
		counter.deactivate();
		Assertions.assertEquals(1, counter.getInfoCount());
		Assertions.assertEquals(1, counter.getWarnCount());
		LOG.info("hello world - this is just a test"); // this should not be counted
		LOG.warn("hello world - this is just a test"); // this should not be counted
		Assertions.assertEquals(1, counter.getInfoCount());
		Assertions.assertEquals(1, counter.getWarnCount());
	}

	@Test
	void testLogCounter_WARN() throws IOException {
		LogCounter counter = new LogCounter(Level.WARN);
		counter.activate();
		LOG.info("hello world - this is just a test");
		LOG.warn("hello world - this is just a test");
		counter.deactivate();
		Assertions.assertEquals(0, counter.getInfoCount());
		Assertions.assertEquals(1, counter.getWarnCount());
		LOG.info("hello world - this is just a test"); // this should not be counted
		LOG.warn("hello world - this is just a test"); // this should not be counted
		Assertions.assertEquals(0, counter.getInfoCount());
		Assertions.assertEquals(1, counter.getWarnCount());
	}
}
