package org.matsim.codeexamples.mobsim.mobsimPassingVehicleQ;

import java.io.File;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.testcases.MatsimTestUtils;

public class RunMobsimWithMultipleModeVehiclesExampleTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void test() {
		String[] args = {
			  "scenarios/equil/example5-config.xml",
			  "--config:controler.outputDirectory", utils.getOutputDirectory(),
			  "--config:controler.lastIteration=2",
			  "--config:controler.writeEventsInterval=1"
		} ;

		try{
			RunMobsimWithMultipleModeVehiclesExample.main( args );
		} catch ( Exception ee ) {
			ee.printStackTrace();
			Assert.fail();
		}

	}

}
