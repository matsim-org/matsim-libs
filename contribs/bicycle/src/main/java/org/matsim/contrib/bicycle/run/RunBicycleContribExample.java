package org.matsim.contrib.bicycle.run;

// this runner ist based on:
//https://github.com/matsim-org/matsim-code-examples/blob/dev.x/src/main/java/org/matsim/codeexamples/extensions/bicycle/RunBicycleContribExample.java

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.contrib.bicycle.BicycleUtils;
//import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import org.matsim.core.network.NetworkUtils;

import java.util.Set;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import com.google.inject.Inject;
import org.matsim.contrib.bicycle.AdditionalBicycleLinkScore;
import org.matsim.contrib.bicycle.AdditionalBicycleLinkScoreDefaultImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.core.controler.AbstractModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.matsim.core.config.groups.ReplanningConfigGroup.*;
import static org.matsim.core.config.groups.ScoringConfigGroup.*;

public final class RunBicycleContribExample {
	private static final Logger LOG = LogManager.getLogger(RunBicycleContribExample.class);

	private static final String BICYCLE = "bike";
	public static final double BICYCLE_SPEED = 6.944;
	private static final boolean USE_OWN_SCORING = true;

	public static void main(String[] args) {
		Config config;
		if (args.length >= 1) {
			LOG.info("A user-specified config.xml file was provided. Using it...");
			config = ConfigUtils.loadConfig(args[0], new BicycleConfigGroup());
		} else {
//			config = ConfigUtils.createConfig("scenarios/bicycle_example/");
//
//			config.network().setInputFile("network_lane.xml");
//			config.plans().setInputFile("population_1200.xml");

			config = ConfigUtils.createConfig("contribs/bicycle/src/main/java/org/matsim/contrib/bicycle/run/scenarios/");

			//Neukoelln bicycle network
			config.network().setInputFile("C:/Users/metz_so/Workspace/data/matsim-network_nk_bike_rules_NEW3.xml.gz"); // Modify this
			//config.network().setInputFile("C:/Users/metz_so/Workspace/data/matsim-network_nk_bike_rules_slim.xml.gz"); // Modify this


			//Berlin bicycle network
			//config.network().setInputFile("C:/Users/metz_so/Workspace/data/matsim-network_berlin_bike_rules.xml.gz"); // Modify this

			//Random plans nord-Neukoelln
			config.plans().setInputFile("C:/Users/metz_so/myProjects/matsim_helper/data/plans_nk_500_bike.xml");
			//config.plans().setInputFile("C:/Users/metz_so/myProjects/matsim_helper/data/plans_nk_5000_bike.xml");

			config.replanning().addStrategySettings(new StrategySettings().setStrategyName("ChangeExpBeta").setWeight(0.8));
			config.replanning().addStrategySettings(new StrategySettings().setStrategyName("ReRoute").setWeight(0.2));

			config.scoring().addActivityParams(new ActivityParams("home").setTypicalDuration(12 * 60 * 60));
			config.scoring().addActivityParams(new ActivityParams("work").setTypicalDuration(8 * 60 * 60));

			config.scoring().addModeParams(new ModeParams(BICYCLE).setConstant(0.).setMarginalUtilityOfDistance(-0.0004).setMarginalUtilityOfTraveling(0.).setMonetaryDistanceRate(0.));

			config.global().setNumberOfThreads(1);
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

			config.controller().setLastIteration(1);

//			config.controller().setOutputDirectory(
//				"C:/Users/metz_so/Workspace/data/matsim-output/26-02-18_nk_motorized_newRunner"
//			);

			String baseOut = "C:/Users/metz_so/Workspace/data/matsim-output/";
			String scenarioName = "nk_motorized_500";


			String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
			String outDir = baseOut + ts + "_" + scenarioName;

			config.controller().setOutputDirectory(outDir);
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
		}

		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
		bicycleConfigGroup.setBicycleMode(BICYCLE);
		bicycleConfigGroup.setMarginalUtilityOfInfrastructure_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfComfort_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfGradient_pct_m(-0.0002);

		//bicycleConfigGroup.setMaxBicycleSpeedForRouting(BICYCLE_SPEED);
		// (technically, this has been superseded by using the correct mode vehicle type, see below.  But there is still a faulty consistency check :-( .)


		bicycleConfigGroup.setMotorizedInteraction(true); // oder abhängig von einem boolean

		config.controller().setWriteEventsInterval(1);
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
		config.routing().removeTeleportedModeParams(BICYCLE);
		config.routing().setRoutingRandomness(4.0);


		List<String> mainModeList = Arrays.asList(BICYCLE, TransportMode.car);

		config.qsim().setMainModes(mainModeList);

		config.routing().setNetworkModes(mainModeList);

		// ===

		Scenario scenario = ScenarioUtils.loadScenario(config);

		NetworkUtils.simplifyNetwork(scenario.getNetwork());
		NetworkUtils.cleanNetwork(scenario.getNetwork(), Set.of(TransportMode.car, BICYCLE));


		{
			// set config such that the mode vehicles come from vehicles data:
			scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

			// now put hte mode vehicles into the vehicles data:
			final VehiclesFactory vf = VehicleUtils.getFactory();
			scenario.getVehicles().addVehicleType(vf.createVehicleType(Id.createVehicleTypeId(TransportMode.car)).setNetworkMode(TransportMode.car));
			scenario.getVehicles().addVehicleType(vf.createVehicleType(Id.createVehicleTypeId(BICYCLE)).setNetworkMode(BICYCLE).setMaximumVelocity(BICYCLE_SPEED).setPcuEquivalents(0.25).setLength(2.0));
		}
//		{
//			Link link = scenario.getNetwork().getLinks().get( Id.createLinkId( 2 ) );
//			BicycleUtils.setBicycleInfrastructureFactor( link, 3. );
//		}
//		{
//			Link link = scenario.getNetwork().getLinks().get( Id.createLinkId( 11 ) );
//			BicycleUtils.setBicycleInfrastructureFactor( link, 3. );
//		}
		// ===

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BicycleModule());

		if (USE_OWN_SCORING) {
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(AdditionalBicycleLinkScoreDefaultImpl.class); // als Delegate verfügbar machen
					bind(AdditionalBicycleLinkScore.class).to(MyAdditionalBicycleLinkScore.class);
				}
			});
		}

		controler.run();
	}

	private static class MyAdditionalBicycleLinkScore implements AdditionalBicycleLinkScore {

		@com.google.inject.Inject
		private AdditionalBicycleLinkScoreDefaultImpl delegate;

		@Override
		public double computeLinkBasedScore(Link link, Id<Vehicle> vehicleId, String bicycleMode) {

			Object v = link.getAttributes().getAttribute("carFreeStatus");
			double carFree = (v instanceof Number) ? ((Number) v).doubleValue() : 0.0;

			double base = delegate.computeLinkBasedScore(link, vehicleId, bicycleMode);

			// Achtung: unskaliert kann das stark wirken. Besser: Gewichtung.
			double weight = 1.0; // TODO kalibrieren
			return base + weight * carFree;
		}
	}


}
