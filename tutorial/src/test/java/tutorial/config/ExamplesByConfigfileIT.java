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
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author nagel
 *
 */
@RunWith(Parameterized.class)
public class ExamplesByConfigfileIT {
	
	private String configFile;

	public ExamplesByConfigfileIT(String configFile ) {
		this.configFile = configFile ;
	}
	
	@Parameters(name = "{index}: configFilename == {0};")
	public static Collection<Object[]> createTests() {
		Collection<Object[]> filesToRun = new ArrayList<Object[]>();

//		filesToRun.add(new Object [] {"../examples/scenarios/equil/config.xml"});
		// yy commented out because output ends up in "./output" instead of "./output/examples", and thus the input dir is not cleaned by the
		// command below.  Should be changed, but are we destroying something if we do? kai, mar'17
		
		filesToRun.add(new Object [] {"examples/tutorial/config/example1-config.xml"});
		filesToRun.add(new Object [] {"examples/tutorial/config/example5-config.xml"});
		filesToRun.add(new Object [] {"examples/tutorial/config/example5trips-config.xml"});
		filesToRun.add(new Object [] {"../examples/scenarios//equil-mixedTraffic/config-with-mode-vehicles.xml"});
		filesToRun.add(new Object [] {"../examples/scenarios//equil-mixedTraffic/config-with-all-vehicles-from-file.xml"});
		filesToRun.add(new Object [] {"examples/tutorial/config/example2-config.xml"});
		filesToRun.add(new Object [] {"../examples/scenarios//equil-extended/config-with-network-change-events.xml"});
		filesToRun.add(new Object [] {"../examples/scenarios/equil-extended/config-with-subpopulation.xml"});
		
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
			IOUtils.deleteDirectoryRecursively(new File("./output/example").toPath());
		} catch ( UncheckedIOException ee ) {
			// (normally, the directory should NOT be there initially.  It might, however, be there if someone ran the main class in some other way,
			// and did not remove the directory afterwards.)
		}
		RunFromConfigfileExample.main(new String[]{configFile});
		IOUtils.deleteDirectoryRecursively(new File("./output/example").toPath());
		// (here, the directory should be there)
	}

}
