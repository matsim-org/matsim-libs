/* *********************************************************************** *
 * project: org.matsim.*
 * IOUtilsTest
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
package org.matsim.utils.io;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author dgrether
 *
 */
public class IOUtilsTest extends MatsimTestCase {
	
	private static final Logger log = Logger.getLogger(IOUtilsTest.class);
	/**
	 * Simple test that checks the creation of logfiles and the filter for the errorLogFile
	 * @throws IOException
	 * @author dgrether
	 */
	public void testInitOutputDirLogging() throws IOException {
		String outDir = this.getOutputDirectory();
		IOUtils.initOutputDirLogging(outDir, null);
		
		File l = new File(outDir + IOUtils.LOGFILE);
		File errorLog = new File(outDir + IOUtils.WARNLOGFILE);
		assertNotNull(l);
		assertNotNull(errorLog);
		assertTrue(l.exists());
		assertTrue(errorLog.exists());
		assertEquals(0, l.length());
		assertEquals(0, errorLog.length());
		log.info("testing");
		assertNotSame(0, l.length());
		assertEquals(0, errorLog.length());
		log.warn("still testing");
		assertNotSame(0, l.length());
		assertNotSame(0, errorLog.length());
	}
	

}
