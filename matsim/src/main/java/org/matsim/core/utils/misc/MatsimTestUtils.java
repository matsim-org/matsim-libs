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

package org.matsim.core.utils.misc;

import java.io.File;
import java.lang.reflect.Method;
import java.security.Permission;

import org.junit.Assert;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

/**
 * Some helper methods for writing JUnit 4 tests in MATSim.
 * Inspired by JUnit's rule TestName
 *
 * @author mrieser
 */
public class MatsimTestUtils extends TestWatchman {

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
	private String classInputDirectory = null;
	/**
	 * The input directory two levels above the default input directory. If files are used
	 * by several test classes of a package they have to be stored in this directory.
	 */
	private String packageInputDirectory;

	private boolean outputDirCreated = false;

	private Class<?> testClass = null;
	private String testMethodName = null;

	public MatsimTestUtils() {
		MatsimRandom.reset();
	}

	/**
	 * Loads a configuration from file (or the default config if <code>configfile</code> is <code>null</code>).
	 *
	 * @param configfile The path/filename of a configuration file, or null to load the default configuration.
	 * @return The loaded configuration.
	 */
	public Config loadConfig(final String configfile, final ConfigGroup... customGroups) {
		Config config;
		if (configfile != null) {
			config = ConfigUtils.loadConfig(configfile, customGroups);
		} else {
			config = ConfigUtils.createConfig( customGroups );
		}
		this.outputDirectory = getOutputDirectory();
		config.controler().setOutputDirectory(this.outputDirectory);
		return config;
	}

	private void createOutputDirectory() {
		if ((!this.outputDirCreated) && (this.outputDirectory != null)) {
			File directory = new File(this.outputDirectory);
			if (directory.exists()) {
				IOUtils.deleteDirectory(directory);
			}
			this.outputDirCreated = directory.mkdirs();
			Assert.assertTrue("Could not create the output directory " + this.outputDirectory, this.outputDirCreated);
		}
	}

	/**
	 * Returns the path to the output directory for this test including a trailing slash as directory delimiter.
	 *
	 * @return path to the output directory for this test
	 */
	public String getOutputDirectory() {
		if (this.outputDirectory == null) {
			this.outputDirectory = "test/output/" + this.testClass.getCanonicalName().replace('.', '/') + "/" + getMethodName() + "/";
		}
		createOutputDirectory();
		return this.outputDirectory;
	}

	/**
	 * Returns the path to the input directory for this test including a trailing slash as directory delimiter.
	 *
	 * @return path to the input directory for this test
	 */
	public String getInputDirectory() {
		if (this.inputDirectory == null) {
			this.inputDirectory = getClassInputDirectory() + getMethodName() + "/";
		}
		return this.inputDirectory;
	}
	/**
	 * Returns the path to the input directory one level above the default input directory for this test including a trailing slash as directory delimiter.
	 *
	 * @return path to the input directory for this test
	 */
	public String getClassInputDirectory() {
		if (this.classInputDirectory == null) {
			this.classInputDirectory = "test/input/" + this.testClass.getCanonicalName().replace('.', '/') + "/";
		}
		return this.classInputDirectory;
	}
	/**
	 * Returns the path to the input directory two levels above the default input directory for this test including a trailing slash as directory delimiter.
	 *
	 * @return path to the input directory for this test
	 */
	public String getPackageInputDirectory() {
		if (this.packageInputDirectory == null) {
			String classDirectory = getClassInputDirectory();
			this.packageInputDirectory = classDirectory.substring(0, classDirectory.lastIndexOf('/'));
			this.packageInputDirectory = this.packageInputDirectory.substring(0, this.packageInputDirectory.lastIndexOf('/') + 1);
		}
		return this.packageInputDirectory;
	}

	/**
	 * @return the name of the currently-running test method
	 */
	public String getMethodName() {
		if (this.testMethodName == null) {
			throw new RuntimeException("MatsimTestUtils.getMethodName() can only be used in actual test, not in constructor or elsewhere!");
		}
		return this.testMethodName;
	}

	/**
	 * Initializes MatsimTestUtils without requiring the method of a class to be a JUnit test.
	 * This should be used for "fixtures" only that provide a scenario common to several
	 * test cases.
	 */
	public void initWithoutJUnitForFixture(Class fixture, Method method){
		this.testClass = fixture;
		this.testMethodName = method.getName();
	}

	/* inspired by
	 * @see org.junit.rules.TestName#starting(org.junit.runners.model.FrameworkMethod)
	 */
	@Override
	public void starting(FrameworkMethod method) {
		super.starting(method);
		this.testClass = method.getMethod().getDeclaringClass();
		this.testMethodName = method.getName();
	}

	@Override
	public void finished(FrameworkMethod method) {
		super.finished(method);
		this.testClass = null;
		this.testMethodName = null;
	}


	public static class ExitTrappedException extends SecurityException {
		private static final long serialVersionUID = 1L;
	}

  public static void forbidSystemExitCall() {
    final SecurityManager securityManager = new SecurityManager() {
      @Override
			public void checkPermission(Permission permission) {
      	if (permission.getName().startsWith("exitVM")) {
          throw new ExitTrappedException();
        }
      }
    };
    System.setSecurityManager(securityManager);
  }

  public static void enableSystemExitCall() {
    System.setSecurityManager(null);
  }

}
