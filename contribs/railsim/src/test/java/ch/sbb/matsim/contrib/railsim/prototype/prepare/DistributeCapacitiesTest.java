package ch.sbb.matsim.contrib.railsim.prototype.prepare;

import ch.sbb.matsim.contrib.railsim.prototype.prepare.DistributeCapacities;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import ch.sbb.matsim.contrib.railsim.prototype.RunRailsim;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Ihab Kaddoura
 */
public class DistributeCapacitiesTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void test0() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"};

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.network().setInputFile("transitNetwork.xml.gz");
			config.transit().setTransitScheduleFile("transitSchedule.xml.gz");
			config.transit().setVehiclesFile("transitVehicles.xml.gz");

			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.qsim().setNumberOfThreads(1);
			config.global().setNumberOfThreads(1);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);

			DistributeCapacities adjust = new DistributeCapacities(scenario);
			adjust.run();

		} catch (Exception ee) {
			ee.printStackTrace();
			LogManager.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

}
