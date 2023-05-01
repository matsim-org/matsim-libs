package ch.sbb.matsim.contrib.railsim.prototype.supply;

import ch.sbb.matsim.contrib.railsim.prototype.RunRailsim;
import ch.sbb.matsim.contrib.railsim.prototype.analysis.ConvertTrainEventsToDefaultEvents;
import ch.sbb.matsim.contrib.railsim.prototype.prepare.SplitTransitLinks;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.MatsimVehicleWriter;

import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Test for supply generation using railsim supply builder.
 *
 * @author Merlin Unterfinger
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RailsimSupplyBuilderTest {

	private static final double TOLERANCE_DELTA = 0.001;
	private static final String CONFIG_FILE = "config.xml";
	private static final String NETWORK_FILE = "transitNetwork.xml";
	private static final String SCHEDULE_FILE = "transitSchedule.xml";
	private static final String VEHICLE_FILE = "transitVehicles.xml";
	private static final String RUN_ID = "test";
	private Scenario scenario;
	private RailsimSupplyBuilder supply;
	private double waitingTime;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void setUp() {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("CH1903plus_LV95");
		scenario = ScenarioUtils.loadScenario(config);
		supply = new RailsimSupplyBuilder(scenario);
		waitingTime = 3. * 60;
	}

	@Test
	public void testBuild() {
		supply.addStop("genf", 2499965., 1119074.);
		supply.addStop("versoix", 2501905., 1126194.);
		supply.addStop("coppet", 2503642., 1130305.);
		supply.addStop("nyon", 2507480., 1137714.);
		// add transit line: IR
		final var ir = supply.addTransitLine("IR", "IR", "genf", waitingTime);
		ir.addStop("versoix", 10 * 60., waitingTime);
		ir.addStop("coppet", 30 * 60., waitingTime);
		ir.addStop("nyon", 20 * 60., waitingTime);
		Stream.of(10 * 3600., 11 * 3600., 12 * 3600., 13 * 3600.).forEach(departureTime -> ir.addDeparture(RouteDirection.FORWARD, departureTime));
		Stream.of(10.25 * 3600., 11.25 * 3600., 12.25 * 3600., 13.25 * 3600.).forEach(departureTime -> ir.addDeparture(RouteDirection.REVERSE, departureTime));
		// add transit line: IC
		final var ic = supply.addTransitLine("IC", "IC", "genf", waitingTime);
		ic.addPass("versoix");
		ic.addPass("coppet");
		ic.addStop("nyon", 45 * 60., waitingTime);
		Stream.of(10.5 * 3600., 11.5 * 3600., 12.5 * 3600., 13.5 * 3600.).forEach(departureTime -> ic.addDeparture(RouteDirection.FORWARD, departureTime));
		Stream.of(10.5 * 3600., 11.5 * 3600., 12.5 * 3600., 13.5 * 3600.).forEach(departureTime -> ic.addDeparture(RouteDirection.REVERSE, departureTime));
		// build transit schedule and network
		supply.build();
		// check generated transit schedule
		// network
		assertEquals(12, scenario.getNetwork().getNodes().size());
		assertEquals(16, scenario.getNetwork().getLinks().size());
		// vehicles
		assertEquals(2, scenario.getTransitVehicles().getVehicleTypes().size());
		assertEquals(5, scenario.getTransitVehicles().getVehicles().size());
		// schedule
		assertEquals(6, scenario.getTransitSchedule().getFacilities().size());
		assertEquals(2, scenario.getTransitSchedule().getTransitLines().size());
		assertEquals(10, scenario.getTransitSchedule().getTransitLines().values().stream().mapToInt(l -> l.getRoutes().size()).sum());
		// line
		final String id = "IC";
		var transitLine = scenario.getTransitSchedule().getTransitLines().get(Id.create(id, TransitLine.class));
		assertEquals(id, transitLine.getId().toString());
		assertNull(transitLine.getName());
		assertEquals(6, transitLine.getRoutes().size());
		// route
		var transitRoute = transitLine.getRoutes().get(Id.create("IC_F_STATION_TO_STATION", TransitRoute.class));
		assertNull(transitRoute.getDescription());
		assertEquals(2, transitRoute.getDepartures().size());
		assertEquals(2, transitRoute.getStops().size());
		assertEquals(41400., transitRoute.getDepartures().get(Id.create("0", Departure.class)).getDepartureTime(), TOLERANCE_DELTA);
		assertEquals(45000., transitRoute.getDepartures().get(Id.create("1", Departure.class)).getDepartureTime(), TOLERANCE_DELTA);
		var firstStop = transitRoute.getStops().get(0);
		assertTrue(firstStop.getArrivalOffset().isUndefined());
		assertEquals(0., firstStop.getDepartureOffset().seconds(), TOLERANCE_DELTA);
		var lastStop = transitRoute.getStops().get(transitRoute.getStops().size() - 1);
		assertEquals(2700., lastStop.getArrivalOffset().seconds(), TOLERANCE_DELTA);
		assertTrue(lastStop.getDepartureOffset().isUndefined());
		// write files
		String outputDir = utils.getOutputDirectory();
		new NetworkWriter(scenario.getNetwork()).write(outputDir + NETWORK_FILE);
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(outputDir + SCHEDULE_FILE);
		new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(outputDir + VEHICLE_FILE);
	}

	@Test
	public final void testRunRailsim() {
		System.setProperty("matsim.preferLocalDtds", "true");
		String inputDir = Paths.get("").toAbsolutePath() + "/" + utils.getOutputDirectory().replace("testRunRailsim/", "testBuild/");
		String outputDir = utils.getOutputDirectory();
		String[] args0 = {utils.getInputDirectory() + CONFIG_FILE};
		try {
			// setup
			Config config = RunRailsim.prepareConfig(args0);
			config.controler().setLastIteration(0);
			config.network().setInputFile(inputDir + NETWORK_FILE);
			config.transit().setTransitScheduleFile(inputDir + SCHEDULE_FILE);
			config.transit().setVehiclesFile(inputDir + VEHICLE_FILE);
			config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
			config.controler().setOutputDirectory(outputDir);
			config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			// split links
			Scenario scenario = RunRailsim.prepareScenario(config);
			SplitTransitLinks splitTransitLinks = new SplitTransitLinks(scenario);
			splitTransitLinks.run(100.);
			assertEquals(412, scenario.getNetwork().getNodes().size());
			assertEquals(416, scenario.getNetwork().getLinks().size());
			// run one iteration
			Controler controler = RunRailsim.prepareControler(scenario);
			controler.run();
			// convert train events
			ConvertTrainEventsToDefaultEvents analysis = new ConvertTrainEventsToDefaultEvents();
			analysis.run(RUN_ID, outputDir);
		} catch (Exception ee) {
			ee.printStackTrace();
			LogManager.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);
			Assert.fail();
		}
	}
}
