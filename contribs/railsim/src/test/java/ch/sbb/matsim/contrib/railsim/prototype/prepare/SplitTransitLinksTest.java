package ch.sbb.matsim.contrib.railsim.prototype.prepare;

import ch.sbb.matsim.contrib.railsim.prototype.prepare.SplitTransitLinks;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import ch.sbb.matsim.contrib.railsim.prototype.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.prototype.RunRailsim;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author Ihab Kaddoura
 */
public class SplitTransitLinksTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void test0() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // one direction

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.network().setInputFile("trainNetwork.xml");
			config.transit().setTransitScheduleFile("transitSchedule.xml");
			config.transit().setVehiclesFile("transitVehicles.xml");

			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.qsim().setNumberOfThreads(1);
			config.global().setNumberOfThreads(1);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = ScenarioUtils.loadScenario(config);

			assertEquals(6, scenario.getNetwork().getNodes().size());
			assertEquals(5, scenario.getNetwork().getLinks().size());

			double distanceBefore = 0.;
			for (Link link : scenario.getNetwork().getLinks().values()) {
				distanceBefore += link.getLength();

				if (link.getId().toString().startsWith("t2_OUT-t3_IN")) {
					link.getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_MINIMUM_TIME, 120.);
				}
			}

			SplitTransitLinks splitTransitLinks = new SplitTransitLinks(scenario);
			splitTransitLinks.run(1000.);

			double distanceAfter = 0.;
			for (Link link : scenario.getNetwork().getLinks().values()) {
				distanceAfter += link.getLength();

				if (link.getId().toString().startsWith("t2_OUT-t3_IN")) {
					double time = RailsimUtils.getMinimumTrainHeadwayTime(link);
					assertEquals("minimum time attribute has not been passed to split links", time, 120., MatsimTestUtils.EPSILON);
				}
			}
			assertEquals("distance has changed after splitting the links", distanceBefore, distanceAfter, MatsimTestUtils.EPSILON);

			assertEquals(104, scenario.getNetwork().getNodes().size());
			assertEquals(103, scenario.getNetwork().getLinks().size());

		} catch (Exception ee) {
			ee.printStackTrace();
			LogManager.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

}
