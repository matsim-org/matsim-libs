/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.bicycle.run;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.bicycle.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author dziemke
 */
public class RunBicycleExample {
	private static final Logger LOG = LogManager.getLogger(RunBicycleExample.class);


	public static void main(String[] args) {
		Config config;
		if (args.length == 1) {
			LOG.info("A user-specified config.xml file was provided. Using it...");
			config = ConfigUtils.loadConfig(args[0], new BicycleConfigGroup());
			fillConfigWithBicycleStandardValues(config);
		} else if (args.length == 0) {
			LOG.info("No config.xml file was provided. Using 'standard' example files given in this contrib's resources folder.");
			// Setting the context like this works when the data is stored under "/matsim/contribs/bicycle/src/main/resources/bicycle_example"
			// TODO https://github.com/matsim-org/matsim-code-examples/tree/dev.x/scenarios/bicycle_example
			config = ConfigUtils.createConfig("contribs/bicycle/src/main/java/org/matsim/contrib/bicycle/run/scenarios/");
			config.addModule(new BicycleConfigGroup());
			fillConfigWithBicycleStandardValues(config);

			config.network().setInputFile("C:/Users/metz_so/Workspace/data/bicycle_example/network_lane_bike_car.xml"); // Modify this
			config.plans().setInputFile("C:/Users/metz_so/Workspace/data/bicycle_example/population_10_bike_car.xml");

			//Neukoelln bicycle network
			//config.network().setInputFile("C:/Users/metz_so/Workspace/data/matsim-network_nk_bike_rules_NEW3.xml.gz"); // Modify this
		//	config.network().setInputFile("C:/Users/metz_so/Workspace/data/matsim-network_nk_bike_rules_slim.xml.gz"); // Modify this

			//Berlin bicycle network
			//config.network().setInputFile("C:/Users/metz_so/Workspace/data/matsim-network_berlin_bike_rules.xml.gz"); // Modify this

			//Random plans nord-Neukoelln
		//	config.plans().setInputFile("C:/Users/metz_so/myProjects/matsim_helper/data/plans_nk_500.xml");
		} else {
			throw new RuntimeException("More than one argument was provided. There is no procedure for this situation. Thus aborting!"
				+ " Provide either (1) only a suitable config file or (2) no argument at all to run example with given example of resources folder.");
		}
		config.controller().setLastIteration(1); // Modify if motorized interaction is used

		//custom output folder
		config.controller().setOutputDirectory(
			"C:/Users/metz_so/Workspace/data/matsim-output/equil_motorized"
		);


		//Simon:  switch between default and considerMotorizedInteraction
		//boolean considerMotorizedInteraction = false;
		//new RunBicycleExample().run(config );
		boolean considerMotorizedInteraction = true;
		new RunBicycleExample().runWithOwnScoring(config, considerMotorizedInteraction);
	}

	static void fillConfigWithBicycleStandardValues(Config config) {
		config.controller().setWriteEventsInterval(1);

		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );
		config.routing().setAccessEgressType( RoutingConfigGroup.AccessEgressType.accessEgressModeToLink );

		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );
		bicycleConfigGroup.setBicycleMode(TransportMode.bike); // "bike"

		// diese config values gehen sowohl in Routing als auch in Scoring ein...
		bicycleConfigGroup.setMarginalUtilityOfInfrastructure_m(-0.002);
//		bicycleConfigGroup.setMarginalUtilityOfInfrastructure_m(-0.2); // experimented with higher impact

		bicycleConfigGroup.setMarginalUtilityOfComfort_m(-0.002);
		//bicycleConfigGroup.setMarginalUtilityOfGradient_pct_m(-0.0002 );


		//bicycleConfigGroup.setMaxBicycleSpeedForRouting(4.16666666);


		List<String> mainModeList = new ArrayList<>();
		mainModeList.add( bicycleConfigGroup.getBicycleMode() );
		mainModeList.add(TransportMode.car);


		//Simon: QSIM
		config.qsim().setMainModes(mainModeList);

		// Simon: hier könnte ich setUsePassingQ etwas so aktivieren
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ); // default ist FIFO, alternative SeepageQ

		//Simon: Replanning (-Strategien)
		config.replanning().setMaxAgentPlanMemorySize(5); // default anyways
		config.replanning().addStrategySettings( new StrategySettings().setStrategyName("ChangeExpBeta" ).setWeight(0.8 ) );
		config.replanning().addStrategySettings( new StrategySettings().setStrategyName("ReRoute" ).setWeight(0.2 ) );

		//Simon: Standard-Scoring?
		config.scoring().addActivityParams( new ActivityParams("home").setTypicalDuration(12*60*60 ) );
		config.scoring().addActivityParams( new ActivityParams("work").setTypicalDuration(8*60*60 ) );

//		config.scoring().addModeParams( new ModeParams("bicycle").setConstant(0. ).setMarginalUtilityOfDistance(-0.0004 ).setMarginalUtilityOfTraveling(-6.0 ).setMonetaryDistanceRate(0. ) );
		config.scoring().addModeParams( new ModeParams(TransportMode.bike)
			.setConstant(0. )
			.setMarginalUtilityOfDistance(-0.0004 )  //per m, ggf. dann personenabhängig
			.setMarginalUtilityOfTraveling(-6.0 )   //
			.setMonetaryDistanceRate(0. )   // keine monetaere Kosten beim eigenen Fahrrad (?)
		);


		config.routing().setNetworkModes(mainModeList);
		config.routing().removeTeleportedModeParams(TransportMode.bike); // <-- MATSim 2025.x ??
	}

	public void run(Config config ) {

		//Simon: Allgemeine Controller/Global Settings
		config.global().setNumberOfThreads(1);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		//Simon: routingRandomness wirkt direkt auf Routing (randomisierte Disutility im bicycle router).
		config.routing().setRoutingRandomness(4.);

		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );

		final String bicycle = bicycleConfigGroup.getBicycleMode();

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//Simon: networkcleaner eingefügt, schmeisst bspw. Kanten ohne mode raus
		// Cleaner: macht kaputte/inkonsistente Netze wieder routingfähig (pro Modus, mit Turn Restrictions).
		// Simplifier: macht große Netze kleiner/übersichtlicher (und muss dabei Restriktionen beachten).

		NetworkUtils.cleanNetwork(scenario.getNetwork(), Set.of(TransportMode.car,TransportMode.bike));
		//NetworkUtils.simplifyNetwork(scenario.getNetwork());
		//NetworkUtils.cleanNetwork(scenario.getNetwork(), Set.of(TransportMode.car,TransportMode.bike));

		// set config such that the mode vehicles come from vehicles data:
		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		// now put hte mode vehicles into the vehicles data:
		final VehiclesFactory vf = VehicleUtils.getFactory();
		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create(TransportMode.car, VehicleType.class ) ) );
		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create( bicycle, VehicleType.class ) )  // TODO: need to change bicycle to TransportMode.bike??
			.setNetworkMode( bicycle ).setMaximumVelocity(4.16666666 ).setPcuEquivalents(0.25 ).setLength(2.0) );

		//Simon: Controler erstellen + BicycleModule installieren (Wiring Routing + Scoring)
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BicycleModule() );

		controler.run();
	}

	/// Simon: Alternatives/Zusätzliches Scoring für considerMotorizedInteraction
	public void runWithOwnScoring(Config config, boolean considerMotorizedInteraction) {
		config.global().setNumberOfThreads(1);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.routing().setRoutingRandomness(4.);

		if (considerMotorizedInteraction) {
			BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );
			bicycleConfigGroup.setMotorizedInteraction(considerMotorizedInteraction);
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// set config such that the mode vehicles come from vehicles data:
		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		// now put hte mode vehicles into the vehicles data:
		final VehiclesFactory vf = VehicleUtils.getFactory();
		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create(TransportMode.car, VehicleType.class ) ) );
		//scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create("bicycle", VehicleType.class ) ).setMaximumVelocity(4.16666666 ).setPcuEquivalents(0.25 ) );
		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create(TransportMode.bike, VehicleType.class ) ).setMaximumVelocity(4.16666666 ).setPcuEquivalents(0.25 ) );

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BicycleModule() );
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.bind( AdditionalBicycleLinkScoreDefaultImpl.class ); // so it can be used as delegate
				this.bind( AdditionalBicycleLinkScore.class ).to( MyAdditionalBicycleLinkScore.class );
			}
		} );

		controler.run();
	}



	/// Simon: Zusätzliches Scoring für carFreeStatus je Link wenn considerMotorizedInteraction
	private static class MyAdditionalBicycleLinkScore implements AdditionalBicycleLinkScore {

		@Inject private AdditionalBicycleLinkScoreDefaultImpl delegate;

		@Override public double computeLinkBasedScore(Link link, Id<Vehicle> vehicleId, String bicycleMode ){
			double result = (double) link.getAttributes().getAttribute( "carFreeStatus" );  // from zero to one

			double amount = delegate.computeLinkBasedScore( link, vehicleId, bicycleMode );

			return amount + result ;  // or some other way to augment the score

		}
	}

}
