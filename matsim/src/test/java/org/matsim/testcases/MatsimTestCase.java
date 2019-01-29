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

import org.junit.Ignore;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

@Ignore
public class MatsimTestCase extends TestCase {

	/** A constant for the exactness when comparing doubles. */
	public static final double EPSILON = 1e-10;

	/** The default output directory, where files of this test should be written to.
	 * Includes the trailing '/' to denote a directory. */
	private String outputDirectory = null;

	/** The default input directory, where files of this test should be read from.
	 * Includes the trailing '/' to denote a directory. */
	private String inputDirectory = null;

	/**
	 * The input directory one level above the default input directory. If files are
	 * used by several test methods of a testcase they have to be stored in this directory.
	 */
	private String classInputDirectory;
	/**
	 * The input directory two levels above the default input directory. If files are used
	 * by several test classes of a package they have to be stored in this directory.
	 */
	private String packageInputDirectory;

	private boolean outputDirCreated = false;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.outputDirectory = "test/output/" + this.getClass().getCanonicalName().replace('.', '/') + "/" + getName() + "/";
		this.classInputDirectory = "test/input/" + this.getClass().getCanonicalName().replace('.', '/');
		this.packageInputDirectory = this.classInputDirectory.substring(0, this.classInputDirectory.lastIndexOf('/') + 1);
		this.classInputDirectory = this.classInputDirectory + "/";
		this.inputDirectory = this.classInputDirectory + getName() + "/";
		MatsimRandom.reset();
	}

	/**
	 * Loads a configuration from file (or the default config if <code>configfile</code> is <code>null</code>).
	 *
	 * @param configfile The path/filename of a configuration file, or null to load the default configuration.
	 * @return The loaded configuration.
	 */
	public Config loadConfig(final String configfile) {
		Config config;
		if (configfile != null) {
			config = ConfigUtils.loadConfig(configfile);
			MatsimRandom.reset(config.global().getRandomSeed());
		} else {
			config = new Config();
			config.addCoreModules();
		}
		createOutputDirectory();
		config.controler().setOutputDirectory(this.outputDirectory);
		return config;
	}

	private void createOutputDirectory() {
		if ((!this.outputDirCreated) && (this.outputDirectory != null)) {
			File directory = new File(this.outputDirectory);
			if (directory.exists()) {
				IOUtils.deleteDirectoryRecursively(directory.toPath());
			}
			this.outputDirCreated = directory.mkdirs();
			assertTrue("Could not create the output directory " + this.outputDirectory, this.outputDirCreated);
		}
	}

	/**
	 * Returns the path to the output directory for this test including a trailing slash as directory delimiter.
	 *
	 * @return path to the output directory for this test
	 */
	public String getOutputDirectory() {
		createOutputDirectory();
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
	/**
	 * Returns the path to the input directory one level above the default input directory for this test including a trailing slash as directory delimiter.
	 *
	 * @return path to the input directory for this test
	 */
	public String getClassInputDirectory() {
		return this.classInputDirectory;
	}
	/**
	 * Returns the path to the input directory two levels above the default input directory for this test including a trailing slash as directory delimiter.
	 *
	 * @return path to the input directory for this test
	 */
	public String getPackageInputDirectory() {
		return this.packageInputDirectory;
	}

}
