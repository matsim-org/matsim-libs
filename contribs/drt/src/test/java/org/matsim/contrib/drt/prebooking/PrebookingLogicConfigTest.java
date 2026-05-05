package org.matsim.contrib.drt.prebooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.prebooking.PrebookingTestEnvironment.RequestInfo;
import org.matsim.contrib.drt.prebooking.logic.AttributeBasedPrebookingLogic;
import org.matsim.contrib.drt.prebooking.logic.AttributeBasedPrebookingLogicParams;
import org.matsim.contrib.drt.prebooking.logic.ProbabilityBasedPrebookingLogicParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests for config-based prebooking logic installation (issue #4545).
 * Verifies that prebooking logic can be configured per DRT mode via config
 * parameter sets instead of manual programmatic installation.
 *
 * @author Samuel Hoenle (samuelhoenle)
 */
public class PrebookingLogicConfigTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void probabilityBasedLogicParams_defaultValues() {
		ProbabilityBasedPrebookingLogicParams params = new ProbabilityBasedPrebookingLogicParams();
		assertEquals(1.0, params.getProbability(), MatsimTestUtils.EPSILON);
		assertEquals(900.0, params.getSubmissionSlack(), MatsimTestUtils.EPSILON);
	}

	@Test
	void probabilityBasedLogicParams_setAndGet() {
		ProbabilityBasedPrebookingLogicParams params = new ProbabilityBasedPrebookingLogicParams();
		params.setProbability(0.5);
		params.setSubmissionSlack(1800.0);
		assertEquals(0.5, params.getProbability(), MatsimTestUtils.EPSILON);
		assertEquals(1800.0, params.getSubmissionSlack(), MatsimTestUtils.EPSILON);
	}

	@Test
	void prebookingParams_probabilityLogicRegistration() {
		PrebookingParams prebookingParams = new PrebookingParams();
		assertFalse(prebookingParams.getLogicParams().isPresent());
		assertFalse(prebookingParams.getProbabilityBasedLogicParams().isPresent());
		assertFalse(prebookingParams.getAttributeBasedLogicParams().isPresent());

		ProbabilityBasedPrebookingLogicParams logicParams = new ProbabilityBasedPrebookingLogicParams();
		logicParams.setProbability(0.7);
		logicParams.setSubmissionSlack(600.0);
		prebookingParams.addParameterSet(logicParams);

		assertTrue(prebookingParams.getLogicParams().isPresent());
		assertTrue(prebookingParams.getProbabilityBasedLogicParams().isPresent());
		assertFalse(prebookingParams.getAttributeBasedLogicParams().isPresent());
		assertEquals(0.7, prebookingParams.getProbabilityBasedLogicParams().get().getProbability(), MatsimTestUtils.EPSILON);
		assertEquals(600.0, prebookingParams.getProbabilityBasedLogicParams().get().getSubmissionSlack(), MatsimTestUtils.EPSILON);
	}

	@Test
	void prebookingParams_attributeLogicRegistration() {
		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.addParameterSet(new AttributeBasedPrebookingLogicParams());

		assertTrue(prebookingParams.getLogicParams().isPresent());
		assertFalse(prebookingParams.getProbabilityBasedLogicParams().isPresent());
		assertTrue(prebookingParams.getAttributeBasedLogicParams().isPresent());
	}

	@Test
	void prebookingParams_oneOfManyConstraint() {
		PrebookingParams prebookingParams = new PrebookingParams();

		ProbabilityBasedPrebookingLogicParams probParams = new ProbabilityBasedPrebookingLogicParams();
		prebookingParams.addParameterSet(probParams);
		assertTrue(prebookingParams.getProbabilityBasedLogicParams().isPresent());

		// Adding another logic type without removing the first should throw
		AttributeBasedPrebookingLogicParams attrParams = new AttributeBasedPrebookingLogicParams();
		assertThrows(IllegalStateException.class, () -> prebookingParams.addParameterSet(attrParams));

		// After removing the first, adding the second should work
		prebookingParams.removeParameterSet(probParams);
		assertFalse(prebookingParams.getProbabilityBasedLogicParams().isPresent());

		prebookingParams.addParameterSet(attrParams);
		assertFalse(prebookingParams.getProbabilityBasedLogicParams().isPresent());
		assertTrue(prebookingParams.getAttributeBasedLogicParams().isPresent());
	}

	@Test
	void configRoundTrip_probabilityBased() {
		Config config = ConfigUtils.createConfig();
		MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
		config.addModule(multiModeDrtConfig);

		DrtConfigGroup drtConfig = new DrtConfigGroup();
		drtConfig.setMode("drt");
		multiModeDrtConfig.addParameterSet(drtConfig);

		PrebookingParams prebookingParams = new PrebookingParams();
		ProbabilityBasedPrebookingLogicParams logicParams = new ProbabilityBasedPrebookingLogicParams();
		logicParams.setProbability(0.42);
		logicParams.setSubmissionSlack(1234.0);
		prebookingParams.addParameterSet(logicParams);
		drtConfig.addParameterSet(prebookingParams);

		// Write and read config
		String configFile = utils.getOutputDirectory() + "/test_config.xml";
		ConfigUtils.writeConfig(config, configFile);

		Config readConfig = ConfigUtils.loadConfig(configFile, new MultiModeDrtConfigGroup());
		MultiModeDrtConfigGroup readMultiModeDrt = MultiModeDrtConfigGroup.get(readConfig);
		DrtConfigGroup readDrtConfig = readMultiModeDrt.getModalElements().iterator().next();

		assertTrue(readDrtConfig.getPrebookingParams().isPresent());
		PrebookingParams readPrebooking = readDrtConfig.getPrebookingParams().get();
		assertTrue(readPrebooking.getProbabilityBasedLogicParams().isPresent());
		assertFalse(readPrebooking.getAttributeBasedLogicParams().isPresent());

		ProbabilityBasedPrebookingLogicParams readLogic = readPrebooking.getProbabilityBasedLogicParams().get();
		assertEquals(0.42, readLogic.getProbability(), MatsimTestUtils.EPSILON);
		assertEquals(1234.0, readLogic.getSubmissionSlack(), MatsimTestUtils.EPSILON);
	}

	@Test
	void configRoundTrip_attributeBased() {
		Config config = ConfigUtils.createConfig();
		MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
		config.addModule(multiModeDrtConfig);

		DrtConfigGroup drtConfig = new DrtConfigGroup();
		drtConfig.setMode("drt");
		multiModeDrtConfig.addParameterSet(drtConfig);

		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.addParameterSet(new AttributeBasedPrebookingLogicParams());
		drtConfig.addParameterSet(prebookingParams);

		String configFile = utils.getOutputDirectory() + "/test_config.xml";
		ConfigUtils.writeConfig(config, configFile);

		Config readConfig = ConfigUtils.loadConfig(configFile, new MultiModeDrtConfigGroup());
		MultiModeDrtConfigGroup readMultiModeDrt = MultiModeDrtConfigGroup.get(readConfig);
		DrtConfigGroup readDrtConfig = readMultiModeDrt.getModalElements().iterator().next();

		assertTrue(readDrtConfig.getPrebookingParams().isPresent());
		PrebookingParams readPrebooking = readDrtConfig.getPrebookingParams().get();
		assertFalse(readPrebooking.getProbabilityBasedLogicParams().isPresent());
		assertTrue(readPrebooking.getAttributeBasedLogicParams().isPresent());
	}

	@Test
	void configRoundTrip_multiModeDrt() {
		/*-
		 * Two DRT modes in one config: "drtA" with probability-based logic,
		 * "drtB" with attribute-based logic. Write to XML and read back,
		 * verifying each mode retains its own logic configuration.
		 */

		Config config = ConfigUtils.createConfig();
		MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
		config.addModule(multiModeDrtConfig);

		// Mode A: probability-based
		DrtConfigGroup drtA = new DrtConfigGroup();
		drtA.setMode("drtA");
		PrebookingParams prebookingA = new PrebookingParams();
		ProbabilityBasedPrebookingLogicParams probParams = new ProbabilityBasedPrebookingLogicParams();
		probParams.setProbability(0.75);
		probParams.setSubmissionSlack(600.0);
		prebookingA.addParameterSet(probParams);
		drtA.addParameterSet(prebookingA);
		multiModeDrtConfig.addParameterSet(drtA);

		// Mode B: attribute-based with custom prefixes
		DrtConfigGroup drtB = new DrtConfigGroup();
		drtB.setMode("drtB");
		PrebookingParams prebookingB = new PrebookingParams();
		AttributeBasedPrebookingLogicParams attrParams = new AttributeBasedPrebookingLogicParams();
		attrParams.setSubmissionTimeAttributePrefix("custom:submit");
		attrParams.setPlannedDepartureTimeAttributePrefix("custom:departure");
		prebookingB.addParameterSet(attrParams);
		drtB.addParameterSet(prebookingB);
		multiModeDrtConfig.addParameterSet(drtB);

		// Write and read config
		String configFile = utils.getOutputDirectory() + "/multi_mode_config.xml";
		ConfigUtils.writeConfig(config, configFile);

		Config readConfig = ConfigUtils.loadConfig(configFile, new MultiModeDrtConfigGroup());
		MultiModeDrtConfigGroup readMultiModeDrt = MultiModeDrtConfigGroup.get(readConfig);
		assertEquals(2, readMultiModeDrt.getModalElements().size());

		DrtConfigGroup readA = null;
		DrtConfigGroup readB = null;
		for (DrtConfigGroup drtCfg : readMultiModeDrt.getModalElements()) {
			if ("drtA".equals(drtCfg.getMode())) readA = drtCfg;
			if ("drtB".equals(drtCfg.getMode())) readB = drtCfg;
		}

		// Verify mode A: probability-based
		assertTrue(readA.getPrebookingParams().isPresent());
		PrebookingParams readPrebookingA = readA.getPrebookingParams().get();
		assertTrue(readPrebookingA.getProbabilityBasedLogicParams().isPresent());
		assertFalse(readPrebookingA.getAttributeBasedLogicParams().isPresent());
		assertEquals(0.75, readPrebookingA.getProbabilityBasedLogicParams().get().getProbability(), MatsimTestUtils.EPSILON);
		assertEquals(600.0, readPrebookingA.getProbabilityBasedLogicParams().get().getSubmissionSlack(), MatsimTestUtils.EPSILON);

		// Verify mode B: attribute-based with custom prefixes
		assertTrue(readB.getPrebookingParams().isPresent());
		PrebookingParams readPrebookingB = readB.getPrebookingParams().get();
		assertFalse(readPrebookingB.getProbabilityBasedLogicParams().isPresent());
		assertTrue(readPrebookingB.getAttributeBasedLogicParams().isPresent());
		AttributeBasedPrebookingLogicParams readAttrParams = readPrebookingB.getAttributeBasedLogicParams().get();
		assertEquals("custom:submit", readAttrParams.getSubmissionTimeAttributePrefix());
		assertEquals("custom:departure", readAttrParams.getPlannedDepartureTimeAttributePrefix());
	}

	@Test
	void endToEnd_probabilityBasedFromConfig() {
		/*-
		 * Test that probability-based logic configured via config produces the same
		 * result as the manual installation. All trips are prebooked (probability=1.0)
		 * with a submission slack of 900 seconds.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("personA", 0, 0, 5, 5, 2000.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();

		// Install prebooking with probability-based logic via config
		PrebookingParams prebookingParams = new PrebookingParams();
		ProbabilityBasedPrebookingLogicParams logicParams = new ProbabilityBasedPrebookingLogicParams();
		logicParams.setProbability(1.0);
		logicParams.setSubmissionSlack(900.0);
		prebookingParams.addParameterSet(logicParams);

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());
		drtConfig.addParameterSet(prebookingParams);

		controller.run();

		// The request should have been prebooked
		RequestInfo requestInfo = environment.getRequestInfo().get("personA");
		// Submission time = departure - slack = 2000 - 900 = 1100
		assertEquals(1100.0, requestInfo.submissionTime, MatsimTestUtils.EPSILON);
		assertFalse(Double.isNaN(requestInfo.pickupTime));
		assertFalse(Double.isNaN(requestInfo.dropoffTime));
	}

	@Test
	void endToEnd_attributeBasedFromConfig() {
		/*-
		 * Test that attribute-based logic configured via config works correctly.
		 * The request has submission time and planned departure time set as attributes.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("personA", 0, 0, 5, 5, 2000.0, 0.0, 2000.0 - 200.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();

		// Install prebooking with attribute-based logic via config
		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.addParameterSet(new AttributeBasedPrebookingLogicParams());

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());
		drtConfig.addParameterSet(prebookingParams);

		controller.run();

		// The request should have been prebooked with submission time from attribute
		RequestInfo requestInfo = environment.getRequestInfo().get("personA");
		assertEquals(0.0, requestInfo.submissionTime, MatsimTestUtils.EPSILON);
		assertFalse(Double.isNaN(requestInfo.pickupTime));
		assertFalse(Double.isNaN(requestInfo.dropoffTime));
	}

	@Test
	void endToEnd_noLogicConfigured_backwardCompatible() {
		/*-
		 * Test that when no logic parameter set is configured, the prebooking
		 * infrastructure is still set up (for manual installation) but no logic
		 * is auto-installed.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("personA", 0, 0, 5, 5, 2000.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();

		// Install prebooking without any logic (backward-compatible scenario)
		PrebookingTest.installPrebooking(controller, false);

		controller.run();

		// Without any prebooking logic, the request is submitted at departure time
		RequestInfo requestInfo = environment.getRequestInfo().get("personA");
		assertEquals(2000.0, requestInfo.submissionTime, MatsimTestUtils.EPSILON);
	}

	@Test
	void endToEnd_multiModeDrt_differentLogics() {
		/*-
		 * Two independent DRT modes in one simulation:
		 *   "drtA" — probability-based prebooking (probability=1.0, slack=900)
		 *   "drtB" — attribute-based prebooking
		 * Each mode has its own vehicle and person. Verify that each mode's
		 * prebooking logic operates independently and produces the expected
		 * submission times.
		 */

		double endTime = 10.0 * 3600.0;
		double edgeLength = 200.0;
		double speed = 10.0;

		// --- Config ---
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.qsim().setStartTime(0.0);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setEndTime(endTime);
		config.qsim().setSimEndtimeInterpretation(EndtimeInterpretation.onlyUseEndtime);

		for (String mode : new String[] { "drtA", "drtB" }) {
			config.scoring().addModeParams(new ModeParams(mode));
		}
		ActivityParams genericParams = new ActivityParams("generic");
		genericParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(genericParams);

		DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfig.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));
		config.addModule(dvrpConfig);

		MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
		config.addModule(multiModeDrtConfig);

		// Mode A: probability-based (all requests prebooked, slack=900)
		DrtConfigGroup modeACfg = new DrtConfigGroup();
		modeACfg.setMode("drtA");
		configureDrtMode(modeACfg);
		PrebookingParams prebookingA = new PrebookingParams();
		ProbabilityBasedPrebookingLogicParams probParams = new ProbabilityBasedPrebookingLogicParams();
		probParams.setProbability(1.0);
		probParams.setSubmissionSlack(900.0);
		prebookingA.addParameterSet(probParams);
		modeACfg.addParameterSet(prebookingA);
		multiModeDrtConfig.addParameterSet(modeACfg);

		// Mode B: attribute-based
		DrtConfigGroup modeBCfg = new DrtConfigGroup();
		modeBCfg.setMode("drtB");
		configureDrtMode(modeBCfg);
		PrebookingParams prebookingB = new PrebookingParams();
		prebookingB.addParameterSet(new AttributeBasedPrebookingLogicParams());
		modeBCfg.addParameterSet(prebookingB);
		multiModeDrtConfig.addParameterSet(modeBCfg);

		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing());

		// --- Scenario ---
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories()
				.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		buildGridNetwork(scenario.getNetwork(), 10, 10, edgeLength, speed);

		Id<Link> originLink = Id.createLinkId("0:0-1:0");
		Id<Link> destLink = Id.createLinkId("5:5-6:5");

		Population population = scenario.getPopulation();
		PopulationFactory factory = population.getFactory();

		// personA uses drtA, departs at 2000
		{
			Person person = factory.createPerson(Id.createPersonId("personA"));
			population.addPerson(person);
			Plan plan = factory.createPlan();
			person.addPlan(plan);
			Activity origin = factory.createActivityFromLinkId("generic", originLink);
			origin.setEndTime(2000.0);
			plan.addActivity(origin);
			plan.addLeg(factory.createLeg("drtA"));
			plan.addActivity(factory.createActivityFromLinkId("generic", destLink));
		}

		// personB uses drtB, departs at 3000, prebooked via attributes at t=500
		{
			Person person = factory.createPerson(Id.createPersonId("personB"));
			population.addPerson(person);
			Plan plan = factory.createPlan();
			person.addPlan(plan);
			Activity origin = factory.createActivityFromLinkId("generic", originLink);
			origin.setEndTime(3000.0);
			origin.getAttributes().putAttribute(
					AttributeBasedPrebookingLogic.getSubmissionTimeAttribute("drtB"), 500.0);
			origin.getAttributes().putAttribute(
					AttributeBasedPrebookingLogic.getPlannedDepartureTimeAttribute("drtB"), 2800.0);
			plan.addActivity(origin);
			plan.addLeg(factory.createLeg("drtB"));
			plan.addActivity(factory.createActivityFromLinkId("generic", destLink));
		}

		// --- Controller ---
		Controler controller = new Controler(scenario);
		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new MultiModeDrtModule());
		controller.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

		IntegerLoadType loadType = new IntegerLoadType("passengers");
		for (String mode : new String[] { "drtA", "drtB" }) {
			FleetSpecification fleet = new FleetSpecificationImpl();
			fleet.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create("vehicle_" + mode, DvrpVehicle.class))
					.capacity(loadType.fromInt(4))
					.serviceBeginTime(0.0)
					.serviceEndTime(endTime)
					.startLinkId(originLink)
					.build());
			controller.addOverridingModule(new AbstractDvrpModeModule(mode) {
				@Override
				public void install() {
					bindModal(FleetSpecification.class).toInstance(fleet);
				}
			});
		}

		// Track submission times per person
		Map<String, Double> submissionTimes = new HashMap<>();
		Map<String, Double> pickupTimes = new HashMap<>();
		Map<String, Double> dropoffTimes = new HashMap<>();
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(
						(DrtRequestSubmittedEventHandler) event -> event.getPersonIds().forEach(
								personId -> submissionTimes.put(personId.toString(), event.getTime())));
				addEventHandlerBinding().toInstance(
						(PassengerPickedUpEventHandler) event ->
								pickupTimes.put(event.getPersonId().toString(), event.getTime()));
				addEventHandlerBinding().toInstance(
						(PassengerDroppedOffEventHandler) event ->
								dropoffTimes.put(event.getPersonId().toString(), event.getTime()));
			}
		});

		controller.run();

		// Verify: personA (probability-based) submitted at 2000 - 900 = 1100
		assertEquals(1100.0, submissionTimes.get("personA"), MatsimTestUtils.EPSILON);
		assertFalse(Double.isNaN(pickupTimes.get("personA")));
		assertFalse(Double.isNaN(dropoffTimes.get("personA")));

		// Verify: personB (attribute-based) submitted at 500.0
		assertEquals(500.0, submissionTimes.get("personB"), MatsimTestUtils.EPSILON);
		assertFalse(Double.isNaN(pickupTimes.get("personB")));
		assertFalse(Double.isNaN(dropoffTimes.get("personB")));
	}

	// --- Helpers for multi-mode test ---

	private static void configureDrtMode(DrtConfigGroup modeConfig) {
		DrtOptimizationConstraintsSetImpl constraints = modeConfig
				.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet();
		constraints.setMaxWaitTime(600.0);
		constraints.setMaxTravelTimeAlpha(1.3);
		constraints.setMaxTravelTimeBeta(600.0);
		modeConfig.setStopDuration(60.0);
		modeConfig.setIdleVehiclesReturnToDepots(false);
		modeConfig.setVehiclesFile(null);
		modeConfig.addDrtInsertionSearchParams(new SelectiveInsertionSearchParams());
	}

	private static void buildGridNetwork(Network network, int width, int height,
			double edgeLength, double speed) {
		NetworkFactory factory = network.getFactory();
		Node[][] nodes = new Node[width][height];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				nodes[i][j] = factory.createNode(Id.createNodeId(i + ":" + j),
						new Coord(i * edgeLength, j * edgeLength));
				network.addNode(nodes[i][j]);
			}
		}
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width - 1; i++) {
				addBidirectionalLinks(network, nodes[i][j], nodes[i + 1][j], edgeLength, speed);
			}
		}
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height - 1; j++) {
				addBidirectionalLinks(network, nodes[i][j], nodes[i][j + 1], edgeLength, speed);
			}
		}
	}

	private static void addBidirectionalLinks(Network network, Node a, Node b,
			double length, double speed) {
		NetworkFactory factory = network.getFactory();
		for (var pair : new Node[][] { { a, b }, { b, a } }) {
			Id<Link> id = Id.createLinkId(pair[0].getId() + "-" + pair[1].getId());
			Link link = factory.createLink(id, pair[0], pair[1]);
			link.setLength(length);
			link.setFreespeed(speed);
			link.setCapacity(1e9);
			link.setNumberOfLanes(1.0);
			network.addLink(link);
		}
	}
}
