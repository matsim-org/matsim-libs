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

package org.matsim.testcases;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

/**
 * Some helper methods for writing JUnit 4 tests in MATSim.
 * Inspired by JUnit's rule TestName
 *
 * @author mrieser
 */
public final class MatsimTestUtils extends TestWatchman {

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

	public Config createConfigWithInputResourcePathAsContext() {
		Config config = ConfigUtils.createConfig();
		config.setContext(inputResourcePath());
		this.outputDirectory = getOutputDirectory();
		config.controler().setOutputDirectory(this.outputDirectory);
		return config;
	}

	public Config createConfigWithClassInputResourcePathAsContext() {
		Config config = ConfigUtils.createConfig();
		config.setContext(classInputResourcePath());
		this.outputDirectory = getOutputDirectory();
		config.controler().setOutputDirectory(this.outputDirectory);
		return config;
	}

	public Config createConfigWithPackageInputResourcePathAsContext() {
		Config config = ConfigUtils.createConfig();
		config.setContext(packageInputResourcePath());
		this.outputDirectory = getOutputDirectory();
		config.controler().setOutputDirectory(this.outputDirectory);
		return config;
	}

	public URL inputResourcePath() {
		return getResourceNotNull("/" + getClassInputDirectory() + getMethodName() + "/.");
	}
	
	/**
	 * @return class input directory as URL
	 */
	public URL classInputResourcePath() {
		return getResourceNotNull("/" + getClassInputDirectory() + "/.");
	}

	public URL packageInputResourcePath() {
		return getResourceNotNull("/" + getPackageInputDirectory() + "/.");
	}

	private URL getResourceNotNull(String pathString) {
		URL resource = this.testClass.getResource(pathString);
		if (resource == null) {
			throw new UncheckedIOException("Not found: "+pathString);
		}
		return resource;
	}

	public Config createConfigWithTestInputFilePathAsContext() {
		try {
			Config config = ConfigUtils.createConfig();
			config.setContext(new File(this.getInputDirectory()).toURI().toURL());
			this.outputDirectory = getOutputDirectory();
			config.controler().setOutputDirectory(this.outputDirectory);
			return config;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public Config createConfig(URL context) {
		Config config = ConfigUtils.createConfig();
		config.setContext(context);
		this.outputDirectory = getOutputDirectory();
		config.controler().setOutputDirectory(this.outputDirectory);
		return config;
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

	public Config loadConfig(final URL configfile, final ConfigGroup... customGroups) {
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

	public Config createConfig(final ConfigGroup... customGroups) {
		Config config = ConfigUtils.createConfig( customGroups );
		this.outputDirectory = getOutputDirectory();
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
			
			Logger.getLogger(this.getClass()).warn( "user.dir = " + System.getProperty("user.dir") ) ;
			
			this.classInputDirectory = "test/input/" +
											   this.testClass.getCanonicalName().replace('.', '/') + "/";
//			this.classInputDirectory = System.getProperty("user.dir") + "/test/input/" +
//											   this.testClass.getCanonicalName().replace('.', '/') + "/";
			// (this used to be relative, i.e. ... = "test/input/" + ... .  Started failing when
			// this was used in tests to read a second config file when I made the path of that
			// relative to the root of the initial config file.  Arghh.  kai, feb'18)
			// yyyyyy needs to be discussed, see MATSIM-776 and MATSIM-777. kai, feb'18
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
  
  public static int compareEventsFiles( String filename1, String filename2 ) {
	  return EventsFileComparator.compareAndReturnInt(filename1, filename2) ;
  }
  public static void compareFilesBasedOnCRC( String filename1, String filename2 ) {
	  long checksum1 = CRCChecksum.getCRCFromFile(filename1) ;
	  long checksum2 = CRCChecksum.getCRCFromFile(filename2) ;
	  Assert.assertEquals( "different file checksums", checksum1, checksum2 ); 
  }
  public static boolean comparePopulations( Population pop1, Population pop2 ) {
	  return PopulationUtils.equalPopulation(pop1, pop2) ;
  }

}
