package org.matsim.contrib.bicycle.run;

// this runner ist based on:
//https://github.com/matsim-org/matsim-code-examples/blob/dev.x/src/main/java/org/matsim/codeexamples/extensions/bicycle/RunBicycleContribExample.java

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import static org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import static org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;

// this is based on https://github.com/matsim-org/matsim-code-examples/blob/2a40dd20336c55a0c8ca4c582d5d9489a4ff8d0c/src/main/java/org/matsim/codeexamples/extensions/bicycle/RunBicycleContribExample.java
// modified to test some functionality

/**
 * @deprecated This class does not belong in the contrib module and has been
 * moved to the project-specific test infrastructure. Use
 * {@link RunSimpleBicycleExample} instead.
 */
@Deprecated(forRemoval = true)
public final class RunBicycleContrib_MITester {
	private static final Logger LOG = LogManager.getLogger(RunBicycleContrib_MITester.class);

	private static final String BICYCLE = "bicycle";
	public static final double BICYCLE_SPEED = 4.16666; //4.16666; //6.944;
	//private static final boolean USE_OWN_SCORING = true;

	public static void main(String[] args) {
		Config config;
		if (args.length >= 1) {
			LOG.info("A user-specified config.xml file was provided. Using it...");
			config = ConfigUtils.loadConfig(args[0], new BicycleConfigGroup());
		} else {

			config = ConfigUtils.createConfig();  // kein Context-Argument

			String base = "C:/Users/metz_so/Workspace/matsim-libs/contribs/bicycle/"
				+ "src/main/java/org/matsim/contrib/bicycle/run/scenarios/bicycle_example/";

			config.network().setInputFile(base + "network_MI_test2_stau.xml");
			config.plans().setInputFile(base + "population_MI_test2.xml");


			config.replanning().addStrategySettings(new StrategySettings().setStrategyName("ChangeExpBeta").setWeight(0.7));
			config.replanning().addStrategySettings(new StrategySettings().setStrategyName("ReRoute").setWeight(0.3));

			config.scoring().addActivityParams(new ActivityParams("home").setTypicalDuration(12 * 60 * 60));
			config.scoring().addActivityParams(new ActivityParams("work").setTypicalDuration(8 * 60 * 60));

			config.scoring().addModeParams(new ModeParams(BICYCLE).setConstant(0.)
				.setMarginalUtilityOfDistance(-0.0004)  //-0.0004
				.setMarginalUtilityOfTraveling(-6.)  // added value of time
				.setMonetaryDistanceRate(0.));

			config.global().setNumberOfThreads(12);
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

			config.controller().setCompressionType(ControllerConfigGroup.CompressionType.gzip);
			config.controller().setLastIteration(5);


			String baseOut = "C:/Users/metz_so/Workspace/data/matsim-output/";

			//String scenarioName = "MI-test_carCount";
			String scenarioName = "MI-test_carsPassed_stau_seepage";
			//String scenarioName = "MI-test_avgCar";


			String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
			String outDir = baseOut + ts + "_" + scenarioName;

			config.controller().setOutputDirectory(outDir);
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
		}

		new RunBicycleContrib_MITester().run(config);
	}

	static void fillConfigWithBicycleStandardValues(Config config) {
		config.controller().setWriteEventsInterval(1);

		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
		bicycleConfigGroup.setBicycleMode(BICYCLE);
		bicycleConfigGroup.setMarginalUtilityOfInfrastructure_m(-0.003);
		bicycleConfigGroup.setMarginalUtilityOfComfort_m(-0.001);
		bicycleConfigGroup.setMarginalUtilityOfGradient_pct_m(-0.001);
		//bicycleConfigGroup.setMotorizedInteractionType(BicycleConfigGroup.MotorizedInteraction.CAR_COUNT_ON_BICYCLE_LEAVE_LINK);
		bicycleConfigGroup.setMotorizedInteractionType(BicycleConfigGroup.MotorizedInteraction.CARS_PASSED_BICYCLE_ON_LINK);
		//bicycleConfigGroup.setMotorizedInteractionType(BicycleConfigGroup.MotorizedInteraction.AVG_CAR_OCCUPANCY_DURING_BICYCLE_TRAVERSAL);


		//bicycleConfigGroup.setBicycleInfraAttribute("cycleway"); // default
		//bicycleConfigGroup.setBicycleInfraAttribute("bicycle_infra");
		bicycleConfigGroup.setBicycleInfraAttribute(BicycleUtils.BicycleInfraAttribute.bicycle_infra);

		List<String> mainModeList = Arrays.asList(BICYCLE, TransportMode.car);
		config.qsim().setMainModes(mainModeList);
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.SeepageQ);
		config.qsim().setSeepModes(Collections.singleton(BICYCLE));
		config.qsim().setRestrictingSeepage(true);

		config.routing().setNetworkModes(mainModeList);
		config.routing().removeTeleportedModeParams(BICYCLE);
		//config.routing().setRoutingRandomness(2.);
		config.routing().setRoutingRandomness(3.);
	}

	public void run(Config config) {
		fillConfigWithBicycleStandardValues(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		//NetworkUtils.cleanNetwork(scenario.getNetwork(), Set.of(TransportMode.car, BICYCLE));

		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		final VehiclesFactory vf = VehicleUtils.getFactory();
		scenario.getVehicles().addVehicleType(vf.createVehicleType(Id.createVehicleTypeId(TransportMode.car)).setNetworkMode(TransportMode.car));
		scenario.getVehicles().addVehicleType(vf.createVehicleType(Id.createVehicleTypeId(BICYCLE))
			.setNetworkMode(BICYCLE)
			.setMaximumVelocity(BICYCLE_SPEED)
			.setPcuEquivalents(0.25)
			.setLength(2.0));

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BicycleModule());
		controler.run();

	}


}
