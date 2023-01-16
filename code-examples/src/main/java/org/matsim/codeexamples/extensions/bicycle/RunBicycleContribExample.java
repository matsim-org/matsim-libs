package org.matsim.codeexamples.extensions.bicycle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RunBicycleContribExample{
	private static final Logger LOG = LogManager.getLogger( RunBicycleContribExample.class );

	public static void main(String[] args) {
		Config config;
		if (args.length >= 1) {
			LOG.info("A user-specified config.xml file was provided. Using it...");
			config = ConfigUtils.loadConfig(args, new BicycleConfigGroup() );
		} else {
			LOG.info("No config.xml file was provided. Using 'standard' example files given in this contrib's resources folder.");
			// Setting the context like this works when the data is stored under "/matsim/contribs/bicycle/src/main/resources/bicycle_example"
			config = ConfigUtils.createConfig("bicycle_example/");
			config.addModule(new BicycleConfigGroup());
			config.network().setInputFile("network_lane.xml"); // Modify this
			config.plans().setInputFile("population_1200.xml");

			config.strategy().addStrategySettings( new StrategyConfigGroup.StrategySettings().setStrategyName("ChangeExpBeta" ).setWeight(0.8 ) );
			config.strategy().addStrategySettings( new StrategyConfigGroup.StrategySettings().setStrategyName("ReRoute" ).setWeight(0.2 ) );

			config.planCalcScore().addActivityParams( new PlanCalcScoreConfigGroup.ActivityParams("home").setTypicalDuration(12*60*60 ) );
			config.planCalcScore().addActivityParams( new PlanCalcScoreConfigGroup.ActivityParams("work").setTypicalDuration(8*60*60 ) );

			config.planCalcScore().addModeParams( new PlanCalcScoreConfigGroup.ModeParams("bicycle").setConstant(0. ).setMarginalUtilityOfDistance(-0.0004 ).setMarginalUtilityOfTraveling(-6.0 ).setMonetaryDistanceRate(0. ) );

			config.global().setNumberOfThreads(1 );
			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

			config.controler().setLastIteration(100); // Modify if motorized interaction is used
		}

		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );
		bicycleConfigGroup.setBicycleMode( "bicycle" );
		bicycleConfigGroup.setMarginalUtilityOfInfrastructure_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfComfort_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfGradient_m_100m(-0.02);
		bicycleConfigGroup.setMaxBicycleSpeedForRouting(4.16666666);

		List<String> mainModeList = Arrays.asList( "bicycle", TransportMode.car);

		config.qsim().setMainModes(mainModeList );

		config.plansCalcRoute().setNetworkModes(mainModeList );

		// ===

		Scenario scenario = ScenarioUtils.loadScenario( config );

		// set config such that the mode vehicles come from vehicles data:
		scenario.getConfig().qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

		// now put hte mode vehicles into the vehicles data:
		final VehiclesFactory vf = VehicleUtils.getFactory();
		scenario.getVehicles().addVehicleType( vf.createVehicleType( Id.create(TransportMode.car, VehicleType.class ) ).setNetworkMode( TransportMode.car ) );
		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create("bicycle", VehicleType.class ) ).setNetworkMode( "bicycle" ).setMaximumVelocity(4.16666666 ).setPcuEquivalents(0.25 ) );

		// ===

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BicycleModule() );

		controler.run();
	}

}
