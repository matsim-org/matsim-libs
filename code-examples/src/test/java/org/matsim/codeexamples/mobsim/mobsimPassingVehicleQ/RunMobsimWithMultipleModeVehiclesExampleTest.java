package org.matsim.codeexamples.mobsim.mobsimPassingVehicleQ;

import java.io.File;

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
		// using an appropriate config file, that is included in the repository.
		// unfortunately, the test now depends on this file. if someone removes/changes it, problems might occur.
		String[] args = {"scenarios/equil/example5-config.xml"};

		RunMobsimWithMultipleModeVehiclesExample matsim = new RunMobsimWithMultipleModeVehiclesExample( args );

		Config config = matsim.prepareConfig() ;
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		matsim.run() ;
	}

}
