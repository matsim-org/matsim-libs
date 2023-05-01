package ch.sbb.matsim.contrib.railsim.prototype;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import ch.sbb.matsim.contrib.railsim.prototype.RailsimConfigGroup.TrainAccelerationApproach;
import ch.sbb.matsim.contrib.railsim.prototype.RailsimConfigGroup.TrainSpeedApproach;
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

import java.util.stream.Collectors;

/**
 * @author Ihab Kaddoura
 */
public class RunRailsimTest {

	private static final Logger log = LogManager.getLogger(RunRailsimTest.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void test0() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // one direction

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.qsim().setNumberOfThreads(1);
			config.global().setNumberOfThreads(1);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			//			double totalTT = 0.;
			//			for (Double tt : transitEventHandler.getVehicle2totalTraveltime().values()) {
			//				totalTT += tt;
			//			}

			// Ist immer noch nicht deterministisch... :-(

			//			Assert.assertEquals("Total travel time has changed.", 433780., totalTT, MatsimTestUtils.EPSILON);
			//
			//			Assert.assertEquals("Arrival time has changed.", 43337., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train10---t2").getFirst(), MatsimTestUtils.EPSILON);
			//			Assert.assertEquals("Arrival time has changed.", 50610., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train10---t3").getFirst(), MatsimTestUtils.EPSILON);
			//			Assert.assertEquals("Arrival time has changed.", 43337., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train10---t2").getFirst(), MatsimTestUtils.EPSILON);

		} catch (Exception ee) {
			ee.printStackTrace();
			LogManager.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test1() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // two directions, two trains in same time step

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			RailsimConfigGroup rscfg = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class);
			rscfg.setSplitLinks(true);
			rscfg.setSplitLinksLength(1000.);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 32473., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---t2_A-B").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 32620., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---t2_B-A").getFirst(), MatsimTestUtils.EPSILON);

			// also make sure ethe vehicles arrive at the end of the route
			Assert.assertEquals("Arrival time has changed.", 36146., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---t3_A-B").getFirst(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Arrival time has changed.", 28800., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---t3_B-A").getFirst(), MatsimTestUtils.EPSILON);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test2() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // two directions, several trains in different time steps

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			RailsimConfigGroup rscfg = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class);
			rscfg.setSplitLinks(true);
			rscfg.setSplitLinksLength(1000.);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 36147., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---t3_A-B").getFirst(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Arrival time has changed.", 36269., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---t3_A-B").getFirst(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Arrival time has changed.", 36389., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train3---t3_A-B").getFirst(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Arrival time has changed.", 36509., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train4---t3_A-B").getFirst(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Arrival time has changed.", 36629., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train5---t3_A-B").getFirst(), MatsimTestUtils.EPSILON);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test3() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // T shaped network

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			RailsimConfigGroup rscfg = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class);
			rscfg.setAccelerationGlobalDefault(Double.MAX_VALUE);
			rscfg.setDecelerationGlobalDefault(Double.MAX_VALUE);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 32474., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---t2_A-B").getFirst(), 1.);
			Assert.assertEquals("Arrival time has changed.", 32473., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---t2_A-B").getFirst(), 1.);
			Assert.assertEquals("Arrival time has changed.", 32547., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train3---t2_A-B").getFirst(), 1.);
			Assert.assertEquals("Arrival time has changed.", 32547., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train3---t2_A-B").getFirst(), 1.);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test4() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // Genf-Bern

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
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
			System.out.println(transitEventHandler.getVehicleFacilityArrival2time2delay().keySet().stream().sorted().collect(Collectors.toList()));
			Assert.assertEquals("Arrival time has changed.", 4121., transitEventHandler.getVehicleFacilityArrival2time2delay().get("Expresszug_GE_BE_train_0---lausanne").getFirst(), 8.0);
			Assert.assertEquals("Arrival time has changed.", 48614., transitEventHandler.getVehicleFacilityArrival2time2delay().get("Expresszug_BE_GE_train_8---genf").getFirst(), 8.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test5() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // short links, long train blocking beyond several links

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			RailsimConfigGroup rscfg = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class);
			rscfg.setAccelerationGlobalDefault(Double.MAX_VALUE);
			rscfg.setDecelerationGlobalDefault(Double.MAX_VALUE);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 36257., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---t3_A-B").getFirst(), 1.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test6() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // short links, long train blocking beyond several links

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 30086., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---19-20").getFirst(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Arrival time has changed.", 29977., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---12-13").getFirst(), MatsimTestUtils.EPSILON);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test7() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // simple corridor, small links

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 29547., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---20-21").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 29593., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---20-21").getFirst(), 1.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	// with very slow acceleration
	@Test
	public final void test7a() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getClassInputDirectory() + "test7/config.xml"}; // simple corridor, small links

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			RailsimConfigGroup rscfg = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class);
			rscfg.setAccelerationGlobalDefault(0.1);
			rscfg.setDecelerationGlobalDefault(0.1);
			rscfg.setTrainAccelerationApproach(TrainAccelerationApproach.euclideanDistanceBetweenStops);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 31187., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---20-21").getFirst(), 5.0);
			Assert.assertEquals("Arrival time has changed.", 31290., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---20-21").getFirst(), 5.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	// with vehicle-specific values provided in the link attributes
	@Test
	public final void test7b() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getClassInputDirectory() + "test7/config.xml"}; // simple corridor, small links

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			RailsimConfigGroup rscfg = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class);
			rscfg.setTrainSpeedApproach(TrainSpeedApproach.fromLinkAttributesForEachVehicleType);

			Scenario scenario = RunRailsim.prepareScenario(config);

			scenario.getNetwork().getLinks().get(Id.createLinkId("10-11")).getAttributes().putAttribute("trainType1", 1.2345);
			scenario.getNetwork().getLinks().get(Id.createLinkId("10-11")).getAttributes().putAttribute("trainType2", 9.8765);

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

			Assert.assertEquals("Arrival time has changed.", 29626., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---20-21").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 29671., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---20-21").getFirst(), 1.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	// with vehicle-specific values provided in the link attributes
	@Test
	public final void test7c() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getClassInputDirectory() + "test7/config.xml"}; // simple corridor, small links

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			RailsimConfigGroup rscfg = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class);
			rscfg.setTrainSpeedApproach(TrainSpeedApproach.fromLinkAttributesForEachLine);

			Scenario scenario = RunRailsim.prepareScenario(config);

			scenario.getNetwork().getLinks().get(Id.createLinkId("10-11")).getAttributes().putAttribute("line1", 1.111);

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

			Assert.assertEquals("Arrival time has changed.", 29635., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---20-21").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 29761., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---20-21").getFirst(), 1.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test8() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // complex intersection

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 30600., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1----103248_19").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 29258., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1----103224_9").getFirst(), 1.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	/**
	 * Similar to test8 but with modification of the network: adding a link with a limited zugfolgezeit...
	 */
	@Test
	public final void test8a() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getClassInputDirectory() + "test8/config.xml"}; // complex intersection

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
			scenario.getNetwork().getLinks().get(Id.createLinkId("-103251_14")).getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_MINIMUM_TIME, 240.);
			scenario.getNetwork().getLinks().get(Id.createLinkId("-103224_8")).getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_MINIMUM_TIME, 240.);

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

			Assert.assertEquals("Arrival time has changed.", 30600., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1----103248_19").getFirst(), 2.0);
			Assert.assertEquals("Arrival time has changed.", 29325., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1----103224_9").getFirst(), 2.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test9() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // complex intersection, kreuzende Fahrwege ohne gleiche Kanten

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 29638., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2----103224_19").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 30852., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1----103248_19").getFirst(), 2.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	/**
	 * Similar to test9 but with modified network: adding Zugfolgezeit attribute.
	 */
	@Test
	public final void test9a() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getClassInputDirectory() + "test9/config.xml"}; // complex intersection, kreuzende Fahrwege ohne gleiche Kanten

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
			scenario.getNetwork().getLinks().get(Id.createLinkId("-103251_14")).getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_MINIMUM_TIME, 240.);
			scenario.getNetwork().getLinks().get(Id.createLinkId("-103224_8")).getAttributes().putAttribute(RailsimUtils.LINK_ATTRIBUTE_MINIMUM_TIME, 240.);
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

			Assert.assertEquals("Arrival time has changed.", 29638., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2----103224_19").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 31035., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1----103248_19").getFirst(), 1.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test10() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // simple intersection, kreuzende Fahrwege ohne gleiche Kanten

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 29601., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---B0").getFirst(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Arrival time has changed.", 28801., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---C0").getFirst(), MatsimTestUtils.EPSILON);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test11() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // simplified BN-GE situation

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 29100., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train3---B").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 29348., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train3---X").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 29468., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---A").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 29366., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---A").getFirst(), 1.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test12() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // simplified BN-GE situation

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 29100., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train3---B").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 29402., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train3---X").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 29468., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---A").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 29950., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---A").getFirst(), 1.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test13() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // passingQ

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			Assert.assertEquals("Arrival time has changed.", 39827., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---7-8").getFirst(), 1.0);
			Assert.assertEquals("Arrival time has changed.", 35825., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---7-8").getFirst(), 1.0);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

	@Test
	public final void test14() {

		try {

			System.setProperty("matsim.preferLocalDtds", "true");

			String[] args0 = {utils.getInputDirectory() + "config.xml"}; // Strecke mti Depot

			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.qsim().setSnapshotStyle(SnapshotStyle.queue);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

			Scenario scenario = RunRailsim.prepareScenario(config);
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

			//			Assert.assertEquals("Arrival time has changed.", 29100., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train3---B").getFirst(), MatsimTestUtils.EPSILON);
			//			Assert.assertEquals("Arrival time has changed.", 29584., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train3---X").getFirst(), MatsimTestUtils.EPSILON);
			//			Assert.assertEquals("Arrival time has changed.", 29468., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train1---A").getFirst(), MatsimTestUtils.EPSILON);
			//			Assert.assertEquals("Arrival time has changed.", 29650., transitEventHandler.getVehicleFacilityArrival2time2delay().get("train2---A").getFirst(), MatsimTestUtils.EPSILON);

		} catch (Exception ee) {
			ee.printStackTrace();
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}

}
