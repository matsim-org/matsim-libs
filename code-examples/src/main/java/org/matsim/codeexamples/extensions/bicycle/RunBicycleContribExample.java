package org.matsim.codeexamples.extensions.bicycle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.bicycle.*;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.matsim.core.config.groups.ReplanningConfigGroup.*;
import static org.matsim.core.config.groups.ScoringConfigGroup.*;

public final class RunBicycleContribExample{
	private static final Logger LOG = LogManager.getLogger( RunBicycleContribExample.class );

	private static final String BICYCLE = "bicycle";
	public static final double BICYCLE_SPEED = 6.944;

	public static void main(String[] args) {
		Config config;
		if (args.length >= 1) {
			LOG.info("A user-specified config.xml file was provided. Using it...");
			config = ConfigUtils.loadConfig(args, new BicycleConfigGroup() );
		} else {
			config = ConfigUtils.createConfig("scenarios/bicycle_example/");

			config.network().setInputFile("network_lane.xml");
			config.plans().setInputFile("population_1200.xml");

			config.replanning().addStrategySettings( new StrategySettings().setStrategyName("ChangeExpBeta" ).setWeight(0.8 ) );
			config.replanning().addStrategySettings( new StrategySettings().setStrategyName("ReRoute" ).setWeight(0.2 ) );

			config.scoring().addActivityParams( new ActivityParams("home").setTypicalDuration(12*60*60 ) );
			config.scoring().addActivityParams( new ActivityParams("work").setTypicalDuration(8*60*60 ) );

			config.scoring().addModeParams( new ModeParams( BICYCLE ).setConstant(0. ).setMarginalUtilityOfDistance(-0.0004 ).setMarginalUtilityOfTraveling(0. ).setMonetaryDistanceRate(0. ) );

			config.global().setNumberOfThreads(1 );
			config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

			config.controller().setLastIteration(0);
		}

		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );
		bicycleConfigGroup.setBicycleMode( BICYCLE );
		bicycleConfigGroup.setMarginalUtilityOfInfrastructure_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfComfort_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfGradient_pct_m(-0.02);

		config.routing().setRoutingRandomness( 0. );

		List<String> mainModeList = Arrays.asList( BICYCLE, TransportMode.car );

		config.qsim().setMainModes(mainModeList );

		config.routing().setNetworkModes(mainModeList );

		// ===

		Scenario scenario = ScenarioUtils.loadScenario( config );
		{
			// set config such that the mode vehicles come from vehicles data:
			scenario.getConfig().qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

			// now put hte mode vehicles into the vehicles data:
			final VehiclesFactory vf = VehicleUtils.getFactory();
			scenario.getVehicles().addVehicleType( vf.createVehicleType( Id.createVehicleTypeId( TransportMode.car ) ).setNetworkMode( TransportMode.car ) );
			scenario.getVehicles().addVehicleType( vf.createVehicleType( Id.createVehicleTypeId( BICYCLE ) ).setNetworkMode( BICYCLE )
													 .setMaximumVelocity( BICYCLE_SPEED ).setPcuEquivalents( 0.25 ) );
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
		controler.addOverridingModule(new BicycleModule() );

		controler.addOverridingModule( new OTFVisLiveModule() );
		controler.run();
	}

}
