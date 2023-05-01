package ch.sbb.matsim.contrib.railsim.prototype.prepare;

import ch.sbb.matsim.contrib.railsim.prototype.prepare.AdjustNetworkToSchedule;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import ch.sbb.matsim.contrib.railsim.prototype.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.prototype.RailsimConfigGroup.TrainSpeedApproach;
import ch.sbb.matsim.contrib.railsim.prototype.RunRailsim;
import ch.sbb.matsim.contrib.railsim.prototype.analysis.ConvertTrainEventsToDefaultEvents;
import ch.sbb.matsim.contrib.railsim.prototype.analysis.TransitEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Ihab Kaddoura
 */
public class AdjustNetworkToScheduleTest {

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

			RailsimConfigGroup rscfg = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class);
			rscfg.setTrainSpeedApproach(TrainSpeedApproach.fromLinkAttributesForEachLineAndRoute);

			Scenario scenario = RunRailsim.prepareScenario(config);

			AdjustNetworkToSchedule adjust = new AdjustNetworkToSchedule(scenario);
			adjust.run();

			Controler controler = RunRailsim.prepareControler(scenario);

			controler.run();

			// read events
			String eventsFile = config.controler().getOutputDirectory() + "/" + config.controler().getRunId() + ".output_events.xml.gz";

			EventsManager events = EventsUtils.createEventsManager();
			TransitEventHandler transitEventHandler = new TransitEventHandler();
			events.addHandler(transitEventHandler);
			events.initProcessing();
			new MatsimEventsReader(events).readFile(eventsFile);
			events.finishProcessing();

			transitEventHandler.printResults(config.controler().getOutputDirectory(), config.controler().getRunId());

			// visualize trains in addition to train path
			ConvertTrainEventsToDefaultEvents analysis = new ConvertTrainEventsToDefaultEvents();
			analysis.run(config.controler().getRunId(), utils.getOutputDirectory());

		} catch (Exception ee) {
			ee.printStackTrace();
			LogManager.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}


}
