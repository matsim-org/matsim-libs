/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.example;
/*
 * created by jbischoff, 19.03.2019
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.discharging.LTHDriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.LTHConsumptionModelReader;
import org.matsim.contrib.ev.routing.EvNetworkRoutingProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Runs a sample EV run using a vehicle consumption model designed at LTH in Lund which takes the speed and the slope of a link into account.
 * Link slopes may be added using a double array on the network.
 * The consumption maps are based on Domingues, Gabriel. / Modeling, Optimization and Analysis of Electromobility Systems. Lund : Department of Biomedical Engineering, Lund university, 2018. 169 p., PhD thesis
 */
public class RunEvExampleWithOwnConsumptionModel{
	static final String DEFAULT_CONFIG_FILE = "test/input/org/matsim/contrib/ev/example/RunEvExample/config.xml";
	private static final Logger log = LogManager.getLogger( RunEvExampleWithOwnConsumptionModel.class );

	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			log.info("Starting simulation run with the following arguments:");
			log.info("args=" + Arrays.toString( args ) );
		} else {
			File localConfigFile = new File(DEFAULT_CONFIG_FILE);
			if (localConfigFile.exists()) {
				log.info("Starting simulation run with the local example config file");
				args = new String[]{ DEFAULT_CONFIG_FILE };
			} else {
				log.info("Starting simulation run with the example config file from GitHub repository");
				args = new String[]{"https://raw.githubusercontent.com/matsim-org/matsim/master/contribs/ev/"
						+ DEFAULT_CONFIG_FILE };
			}
		}
		new RunEvExampleWithOwnConsumptionModel().run(args );
	}

	public void run( String[] args ) {
		Config config = ConfigUtils.loadConfig(args, new EvConfigGroup());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		// ===

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// ===

		Controler controler = new Controler(scenario);
		{
			DriveEnergyConsumption.Factory driveEnergyConsumptionFactory = new DriveEnergyConsumption.Factory(){
				@Override public DriveEnergyConsumption create( ElectricVehicle electricVehicle ){
					DriveEnergyConsumption.Factory factory = new LTHConsumptionModelReader().readURL( ConfigGroup.getInputFileURL( config.getContext(), "MidCarMap.csv" ) );
					DriveEnergyConsumption delegate = factory.create( electricVehicle );

					DriveEnergyConsumption consumption = new DriveEnergyConsumption(){
						@Override public double calcEnergyConsumption( Link link, double travelTime, double linkEnterTime ){

							// discharge because the link must be driven:
							double delta = delegate.calcEnergyConsumption( link, travelTime, linkEnterTime );

							double desiredSocAtEndOfLink = (double) electricVehicle.getVehicleSpecification().getMatsimVehicle().getAttributes().getAttribute( "whatever" );

							return electricVehicle.getBattery().getSoc() - desiredSocAtEndOfLink;
							// * above will often be negative; this is the purpose: discharging is negative i.e. we are
							// charging on the link.  ((This is why I am in general against hiding the sign in the method
							// name.  kai))

							// * above is in SOC space, needs to be translated into kWh space

							// * need to make sure that the above charging is physically possible

							// * need to make sure that we are not discharging beyond what is needed to drive the link

						}
					};
					return consumption;
				}
			};

			controler.addOverridingModule( new EvModule() );
			controler.addOverridingModule( new AbstractModule(){
				@Override
				public void install(){
					bind( DriveEnergyConsumption.Factory.class ).toInstance( driveEnergyConsumptionFactory );
					bind( AuxEnergyConsumption.Factory.class ).toInstance(
							electricVehicle -> ( beginTime, duration, linkId ) -> 0 ); //a dummy factory, as aux consumption is part of the drive consumption in the model

					addRoutingModuleBinding( TransportMode.car ).toProvider( new EvNetworkRoutingProvider( TransportMode.car ) );
					// a router that inserts charging activities when the battery is run empty.  there may be some other way to insert
					// charging activities, based on the situation.  kai, dec'22
				}
			} );
		}


		controler.run();
	}
}
