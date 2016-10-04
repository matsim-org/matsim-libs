/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package tutorial.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.core.utils.io.IOUtils;

import tutorial.config.RunFromConfigfileExample;

/**
 * @author nagel
 *
 */
@RunWith(Parameterized.class)
public class ExamplesByConfigfileTest {
	
	private String configFile;

	public ExamplesByConfigfileTest( String configFile ) {
		this.configFile = configFile ;
	}
	
	@Parameters(name = "{index}: configFilename == {0};")
	public static Collection<Object[]> createTests() {
		Collection<Object[]> filesToRun = new ArrayList<Object[]>();
//		----------
//		already working configs
		filesToRun.add(new Object [] {"examples/equil/config.xml"});
		filesToRun.add(new Object [] {"examples/equil-extended/config.xml"});
		filesToRun.add(new Object [] {"examples/tutorial/config/example1-config.xml"});
		filesToRun.add(new Object [] {"examples/tutorial/config/example5-config.xml"});
		filesToRun.add(new Object [] {"examples/tutorial/config/example5trips-config.xml"});
		filesToRun.add(new Object [] {"examples/equil-mixedTraffic/config.xml"});
//		----------
		
//		----------
//		fixed configs
//		-----
		filesToRun.add(new Object [] {"examples/tutorial/config/example2-config.xml"}); // yyyy throws exception; should be investigated and fixed.  kai, sep'16
		// Used config group "simulation" which no longer exists. Replaced with config group "qsim", which has the same parameter "snapshotperiod". Now running without exception. If only the name of the config group was changed from "simulation" to "qsim", this should be sufficient. vsp-gleich, sep'16
//		----------
		
//		----------
//		not working configs
//		-----
//		filesToRun.add(new Object [] {"examples/equil-extended/config-with-roadpricing.xml"}); // yyyy throws exception; should be investigated and fixed.  (This can not work from core matsim after roadpricing was moved into a contrib!) kai, sep'16
//		-----
//		filesToRun.add(new Object [] {"examples/tutorial/config/externalReplanning.xml"}); // yyyy throws exception; should be investigated and fixed.  kai, sep'16 		
		// adjusted some file paths in externalReplanning.xml. However, there is no longer any replanning.jar as referenced in externalReplanning.xml (neither at the path indicated nor elsewhere in the matsim repository). Delete? vsp-gleich, oct'16
//		-----
//		filesToRun.add(new Object [] {"examples/equil-extended/config-with-network-change-events.xml"}); // yyyy runs forever; should be investigated and fixed.  kai, sep'16
		// All agents drive by car from link 1 to link 20 and back to link 1. As the networkChangeEvents.xml sets the free speed of link 1 where all agents depart (and other links) to 0 m/s before the first agent departs, no agent will ever arrive during the first iteration. Keep link 1 open? vsp-gleich, oct'16
//		-----
//		filesToRun.add(new Object [] {"examples/tutorial/config/externalMobsim.xml"}); // yyyy throws exception; should be investigated and fixed.  kai, sep'16
		// adjusted file paths in externalMobsim.xml. However, there is no longer any Mobsim.jar as referenced in externalMobsim.xml (neither at the path indicated nor elsewhere in the matsim repository). Uses config group "simulation" which no longer exists. Delete? vsp-gleich, oct'16
//		-----
		
		return filesToRun;
		
		// the convention, I think, is that the output of the method marked by "@Parameters" is taken as input to the constructor
		// before running each test. kai, jul'16
	}


	/**
	 * Test method for {@link tutorial.config.RunFromConfigfileExample#main(java.lang.String[])}.
	 */
	@SuppressWarnings("static-method")
	@Test
	public final void testMain() {
		try {
			IOUtils.deleteDirectory(new File("./output/example"),false);
		} catch ( IllegalArgumentException ee ) {
			// (normally, the directory should NOT be there initially.  It might, however, be there if someone ran the main class in some other way,
			// and did not remove the directory afterwards.)
		}
		RunFromConfigfileExample.main(new String[]{configFile});
		IOUtils.deleteDirectory(new File("./output/example"),false);
		// (here, the directory should be there)
	}

}
