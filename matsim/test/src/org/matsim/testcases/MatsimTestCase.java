/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimTestCase.java
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

package org.matsim.testcases;

import java.io.File;

import junit.framework.TestCase;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;

public class MatsimTestCase extends TestCase {

	/** The default output directory, where files of this test should be written to.
	 * Includes the trailing '/' to denote a directory. */
	private String outputDirectory = null;

	/** The default input directory, where files of this test should be read from.
	 * Includes the trailing '/' to denote a directory. */
	private String inputDirectory = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		this.outputDirectory = "test/output/" + this.getClass().getCanonicalName().replace('.', '/') + "/" + getName() + "/";
		this.inputDirectory = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/" + getName() + "/";
		
		createOutputDirectory();
		Gbl.reset(); // make sure we start with a clean environment
	}

	public Config loadConfig(final String configfile) {
		String [] args = {configfile};
		Config config;
		if (configfile != null) {
			config = Gbl.createConfig(args);
		} else {
			config = Gbl.createConfig(new String[0]);
		}
		config.controler().setOutputDirectory(this.outputDirectory);
		return config;
	}

	private void createOutputDirectory() {
		File directory = new File(this.outputDirectory);
		if (directory.exists()) {
			IOUtils.deleteDirectory(directory);
		}
		boolean success = directory.mkdirs();
		assertTrue("Could not create the output directory " + this.outputDirectory, success);
	}

	/**
	 * Returns the path to the output directory for this test including a trailing slash as directory delimiter.
	 *
	 * @return path to the output directory for this test
	 */
	public String getOutputDirectory() {
		return this.outputDirectory;
	}
	
	/**
	 * Returns the path to the input directory for this test including a trailing slash as directory delimiter.
	 *
	 * @return path to the input directory for this test
	 */
	public String getInputDirectory() {
		return this.inputDirectory;
	}

}
