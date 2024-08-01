package org.matsim.integration.drtAndPt;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
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
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class PtAlongALineTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Disabled
	@Test
	void testPtAlongALine() {

		Config config = createConfig(utils.getOutputDirectory());

		Scenario scenario = new PtAlongALineFixture().createScenario(config, 1000);

		Controler controler = new Controler(scenario);

		controler.run();
	}

	/**
	 * Test of Intermodal Access & Egress to pt using bike.There are three transit stops, and
	 * only the middle stop is accessible by bike.
	 */
	@Disabled
	@Test
	void testPtAlongALineWithRaptorAndBike() {

		Config config = createConfig(utils.getOutputDirectory());

		SwissRailRaptorConfigGroup configRaptor = createRaptorConfigGroup(1000000,
				1000000);// (radius walk, radius bike)
		config.addModule(configRaptor);

		Scenario scenario = new PtAlongALineFixture().createScenario(config, 1000);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SwissRailRaptorModule());

		controler.run();
	}

	/**
	 * Test of Drt. 200 drt Vehicles are generated on Link 499-500, and all Agents rely on these
	 * drts to get to their destination
	 */
	@Disabled
	@Test
	void testDrtAlongALine() {

		Config config = ConfigUtils.createConfig();

		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);

		config.transit().setUseTransit(true);

		DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);

		MultiModeDrtConfigGroup multiModeDrtCfg = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.mode = "drt_A";
			drtConfig.stopDuration = 60.;
			DefaultDrtOptimizationConstraintsSet defaultConstraintsSet =
                    (DefaultDrtOptimizationConstraintsSet) drtConfig.addOrGetDrtOptimizationConstraintsParams()
							.addOrGetDefaultDrtOptimizationConstraintsSet();
			defaultConstraintsSet.maxWaitTime = 900.;
			defaultConstraintsSet.maxTravelTimeAlpha = 1.3;
			defaultConstraintsSet.maxTravelTimeBeta = 10. * 60.;
			defaultConstraintsSet.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
			drtConfig.changeStartLinkToLastLinkInSchedule = true;
			multiModeDrtCfg.addParameterSet(drtConfig);
		}

		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			DrtConfigs.adjustDrtConfig(drtCfg, config.scoring(), config.routing());
		}

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		{
			ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
			stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice);
			stratSets.setWeight(0.1);
			config.replanning().addStrategySettings(stratSets);
			//
			config.subtourModeChoice().setModes(new String[] { TransportMode.car, "drt_A" });
		}
		{
			ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
			stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
			stratSets.setWeight(1.);
			config.replanning().addStrategySettings(stratSets);
		}

		{
			ModeParams modeParams = new ModeParams("drt_A");
			config.scoring().addModeParams(modeParams);
		}

		{
			ModeParams modeParams = new ModeParams("drt_A_walk");
			config.scoring().addModeParams(modeParams);
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// ---

		final int lastNodeIdx = 1000;
		final double deltaX = 100.;

		PtAlongALineFixture.createAndAddCarNetwork(scenario, lastNodeIdx, deltaX);

		PtAlongALineFixture.createAndAddPopulation(scenario, "drt_A", 1000);

		final double deltaY = 1000.;

		var fixture = new PtAlongALineFixture();

		fixture.createAndAddTransitNetwork(scenario, lastNodeIdx, deltaX, deltaY);

		fixture.createAndAddTransitStopFacilities(scenario, lastNodeIdx, deltaX, deltaY);

		fixture.createAndAddTransitVehicleType(scenario);

		fixture.createAndAddTransitLine(scenario);

		TransitScheduleValidator.printResult(
				TransitScheduleValidator.validateAll(scenario.getTransitSchedule(), scenario.getNetwork()));
		// ---

		scenario.getPopulation()
				.getFactory()
				.getRouteFactories()
				.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());

		controler.configureQSimComponents(DvrpQSimComponents.activateModes("drt_A"));

		controler.addOverridingModule(PtAlongALineTest.createGeneratedFleetSpecificationModule("drt_A", "drtA-", 200,
				Id.createLinkId("499-500"), 4));

		controler.run();
	}

	/**
	 * Test of Intermodal Access & Egress to pt using drt. Only the middle pt station is accessible by
	 * drt, which is set by a StopFilterAttribute
	 */

	@Disabled
	@Test
	void testPtAlongALineWithRaptorAndDrtStopFilterAttribute() {
		Config config = PtAlongALineTest.createConfig(utils.getOutputDirectory());

		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		// yy why?  kai, jun'19

		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		ModeParams accessWalk = new ModeParams(TransportMode.non_network_walk);
		accessWalk.setMarginalUtilityOfTraveling(0);
		config.scoring().addModeParams(accessWalk);

		// (scoring parameters for drt modes)
		{
			ModeParams modeParams = new ModeParams(TransportMode.drt);
			config.scoring().addModeParams(modeParams);
		}

		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		// (as of today, will also influence router. kai, jun'19)

		config.controller().setLastIteration(0);

		{
			// (raptor config)

			SwissRailRaptorConfigGroup configRaptor = ConfigUtils.addOrGetModule(config,
					SwissRailRaptorConfigGroup.class);
			configRaptor.setUseIntermodalAccessEgress(true);

			// drt
			IntermodalAccessEgressParameterSet paramSetDrt = new IntermodalAccessEgressParameterSet();
			paramSetDrt.setMode(TransportMode.drt);
			paramSetDrt.setMaxRadius(1000000000);
			//			paramSetDrt.setStopFilterAttribute( "drtAccessible" );
			//			paramSetDrt.setStopFilterValue( "true" );
			configRaptor.addIntermodalAccessEgress(paramSetDrt);

		}

		DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
		MultiModeDrtConfigGroup mm = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.stopDuration = 60.;
			DefaultDrtOptimizationConstraintsSet defaultConstraintsSet =
					(DefaultDrtOptimizationConstraintsSet) drtConfig.addOrGetDrtOptimizationConstraintsParams()
							.addOrGetDefaultDrtOptimizationConstraintsSet();
			defaultConstraintsSet.maxTravelTimeAlpha = 1.3;
			defaultConstraintsSet.maxTravelTimeBeta = 5. * 60.;
			defaultConstraintsSet.maxWaitTime = Double.MAX_VALUE;
			defaultConstraintsSet.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
			drtConfig.mode = TransportMode.drt;
			mm.addParameterSet(drtConfig);
		}

		for (DrtConfigGroup drtConfigGroup : mm.getModalElements()) {
			DrtConfigs.adjustDrtConfig(drtConfigGroup, config.scoring(), config.routing());
		}

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

		Scenario scenario = new PtAlongALineFixture().createScenario(config, 100);

		scenario.getPopulation()
				.getFactory()
				.getRouteFactories()
				.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 0, 1000, TransportMode.drt);

		// The following is for the _router_, not the qsim!  kai, jun'19
		VehiclesFactory vf = scenario.getVehicles().getFactory();
		{
			VehicleType vehType = vf.createVehicleType(Id.create(TransportMode.drt, VehicleType.class));
			vehType.setMaximumVelocity(50. / 3.6);
			scenario.getVehicles().addVehicleType(vehType);
		}
		{
			VehicleType vehType = vf.createVehicleType(Id.create(TransportMode.car, VehicleType.class));
			vehType.setMaximumVelocity(50. / 3.6);
			scenario.getVehicles().addVehicleType(vehType);
		}

		// ===

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SwissRailRaptorModule());

		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(TransportMode.drt));

		controler.addOverridingModule(
				PtAlongALineTest.createGeneratedFleetSpecificationModule(TransportMode.drt, "DRT-", 10,
						Id.createLinkId("0-1"), 4));

		// This will start otfvis.  Comment out if not needed.
		//		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run();
	}

	static Config createConfig(String outputDir) {
		Config config = ConfigUtils.createConfig();

		config.global().setNumberOfThreads(1);

		config.controller().setOutputDirectory(outputDir);
		config.controller().setLastIteration(0);

		config.routing().getModeRoutingParams().get(TransportMode.walk).setTeleportedModeSpeed(3.);
		config.routing().getModeRoutingParams().get(TransportMode.bike).setTeleportedModeSpeed(10.);

		config.qsim().setEndTime(24. * 3600.);

		config.transit().setUseTransit(true);

		// This configures otfvis:
		OTFVisConfigGroup visConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
		visConfig.setDrawTransitFacilities(false);
		visConfig.setColoringScheme(OTFVisConfigGroup.ColoringScheme.bvg);
		visConfig.setDrawTime(true);
		visConfig.setDrawNonMovingItems(true);
		visConfig.setAgentSize(125);
		visConfig.setLinkWidth(30);
		visConfig.setShowTeleportedAgents(true);
		visConfig.setDrawTransitFacilities(true);
		//		{
		//			BufferedImage image = null ;
		//			Rectangle2D zoomstore = new Rectangle2D.Double( 0., 0., +100.*1000., +10.*1000. ) ;
		//			ZoomEntry zoomEntry = new ZoomEntry( image, zoomstore, "*Initial*" ) ;
		//			visConfig.addZoom( zoomEntry );
		//		}

		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.kinematicWaves);
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);

		configureScoring(config);
		return config;
	}

	private static void configureScoring(Config config) {
		ModeParams accessWalk = new ModeParams(TransportMode.non_network_walk);
		accessWalk.setMarginalUtilityOfTraveling(0);
		config.scoring().addModeParams(accessWalk);

		ModeParams transitWalk = new ModeParams("transit_walk");
		transitWalk.setMarginalUtilityOfTraveling(0);
		config.scoring().addModeParams(transitWalk);

		ModeParams bike = new ModeParams("bike");
		bike.setMarginalUtilityOfTraveling(0);
		config.scoring().addModeParams(bike);

		ModeParams drt = new ModeParams("drt");
		drt.setMarginalUtilityOfTraveling(0);
		config.scoring().addModeParams(drt);
	}

	static SwissRailRaptorConfigGroup createRaptorConfigGroup(int radiusWalk, int radiusBike) {
		SwissRailRaptorConfigGroup configRaptor = new SwissRailRaptorConfigGroup();
		configRaptor.setUseIntermodalAccessEgress(true);

		// Walk
		IntermodalAccessEgressParameterSet paramSetWalk = new IntermodalAccessEgressParameterSet();
		paramSetWalk.setMode(TransportMode.walk);
		paramSetWalk.setMaxRadius(radiusWalk);
		paramSetWalk.setPersonFilterAttribute(null);
		paramSetWalk.setStopFilterAttribute(null);
		configRaptor.addIntermodalAccessEgress(paramSetWalk);

		// Bike
		IntermodalAccessEgressParameterSet paramSetBike = new IntermodalAccessEgressParameterSet();
		paramSetBike.setMode(TransportMode.bike);
		paramSetBike.setMaxRadius(radiusBike);
		paramSetBike.setPersonFilterAttribute(null);
		//		paramSetBike.setStopFilterAttribute("bikeAccessible");
		//		paramSetBike.setStopFilterValue("true");
		configRaptor.addIntermodalAccessEgress(paramSetBike);

		return configRaptor;
	}

	static AbstractDvrpModeModule createGeneratedFleetSpecificationModule(String mode, String vehPrefix,
			int numberofVehicles, Id<Link> startLinkId, int capacity) {
		return new AbstractDvrpModeModule(mode) {
			@Override
			public void install() {
				bindModal(FleetSpecification.class).toProvider(
						() -> PtAlongALineTest.createDrtFleetSpecifications(vehPrefix, numberofVehicles, startLinkId,
								capacity)).asEagerSingleton();
			}
		};
	}

	static FleetSpecification createDrtFleetSpecifications(String vehPrefix, int numberofVehicles, Id<Link> startLinkId,
			int capacity) {
		FleetSpecification fleetSpecification = new FleetSpecificationImpl();
		for (int i = 0; i < numberofVehicles; i++) {
			//for multi-modal networks: Only links where drts can ride should be used.
			fleetSpecification.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create(vehPrefix + i, DvrpVehicle.class))
					.startLinkId(startLinkId)
					.capacity(capacity)
					.serviceBeginTime(0)
					.serviceEndTime(36 * 3600)
					.build());
		}
		return fleetSpecification;
	}

	static void addModeToAllLinksBtwnGivenNodes(Network network, int fromNodeNumber, int toNodeNumber, String drtMode) {
		for (int i = fromNodeNumber; i < toNodeNumber; i++) {
			Set<String> newAllowedModes = new HashSet<>(
					network.getLinks().get(Id.createLinkId(i + "-" + (i + 1))).getAllowedModes());
			newAllowedModes.add(drtMode);
			network.getLinks().get(Id.createLinkId(i + "-" + (i + 1))).setAllowedModes(newAllowedModes);

			newAllowedModes = new HashSet<>(
					network.getLinks().get(Id.createLinkId((i + 1) + "-" + i)).getAllowedModes());
			newAllowedModes.add(drtMode);
			network.getLinks().get(Id.createLinkId((i + 1) + "-" + i)).setAllowedModes(newAllowedModes);
		}
	}

}
