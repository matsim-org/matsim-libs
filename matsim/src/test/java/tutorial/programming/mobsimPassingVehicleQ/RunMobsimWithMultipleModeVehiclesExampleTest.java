package tutorial.programming.mobsimPassingVehicleQ;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;

public class RunMobsimWithMultipleModeVehiclesExampleTest {

	@Test
	public void test() {
		// using an appropriate config file, that is included in the repository.
		// unfortunately, the test now depends on this file. if someone removes/changes it, problems might occur.
		String[] args = {"examples/tutorial/config/example5-config.xml"};
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		String pathname = config.getParam("controler", "outputDirectory");

		try {
			IOUtils.deleteDirectory(new File(pathname), false);
		} catch (IllegalArgumentException ee) {
			// (maybe the directory is left over from somewhere else; do nothing)
		}

		RunMobsimWithMultipleModeVehiclesExample.main(args);

		IOUtils.deleteDirectory(new File(pathname), false);
	}

}
