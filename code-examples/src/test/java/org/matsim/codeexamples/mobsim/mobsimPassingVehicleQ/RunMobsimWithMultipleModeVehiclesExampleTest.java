package org.matsim.codeexamples.mobsim.mobsimPassingVehicleQ;

import java.io.File;

import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

public class RunMobsimWithMultipleModeVehiclesExampleTest {

	@Test
	public void test() {
		// using an appropriate config file, that is included in the repository.
		// unfortunately, the test now depends on this file. if someone removes/changes it, problems might occur.
		String[] args = {"scenarios/equil/example5-config.xml"};
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		String pathname = config.getParam("controler", "outputDirectory");

		try {
			IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());
		} catch (UncheckedIOException ee) {
			// (maybe the directory is left over from somewhere else; do nothing)
		}

		RunMobsimWithMultipleModeVehiclesExample.main(args);

		IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());
	}

}
