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
import java.util.Arrays;
import java.util.Collection;

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
		int nConfigFiles = 5 ;
		Object [][] files2run = new Object [nConfigFiles][1]; 
		files2run[0] = new Object [] {"examples/equil/config.xml"};
		files2run[1] = new Object [] {"examples/equil-extended/config.xml"};
		files2run[2] = new Object [] {"examples/tutorial/config/example1-config.xml"} ;
		files2run[3] = new Object [] {"examples/tutorial/config/example5-config.xml"} ;
		files2run[4] = new Object [] {"examples/tutorial/config/example5trips-config.xml"} ;
//		files2run[5] = new Object [] {"examples/equil-extended/config-with-roadpricing.xml"}; // yyyy throws exception; should be investigated and fixed.  (This can not work from core matsim after roadpricing was moved into a contrib!) kai, sep'16
//		files2run[3] = new Object [] {"examples/equil-mixedTraffic/config.xml"}; // yyyy throws exception; should be investigated and fixed.  kai, sep'16
//		files2run[5] = new Object [] {"examples/tutorial/config/example2-config.xml"} ; // yyyy throws exception; should be investigated and fixed.  kai, sep'16
//		files2run[8] = new Object [] {"examples/tutorial/config/externalReplanning.xml"} ; // yyyy throws exception; should be investigated and fixed.  kai, sep'16 
//		files2run[2] = new Object [] {"examples/equil-extended/config-with-network-change-events.xml"}; // yyyy runs forever; should be investigated and fixed.  kai, sep'16
//		files2run[1] = new Object [] {"examples/tutorial/config/externalMobsim.xml"} ; // yyyy throws exception; should be investigated and fixed.  kai, sep'16
		return Arrays.asList(files2run);
		
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
