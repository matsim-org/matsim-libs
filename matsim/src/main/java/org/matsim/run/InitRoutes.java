/* *********************************************************************** *
 * project: org.matsim.*
 * XY2Links.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.run;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.DefaultPrepareForSimModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ArgumentParser;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Assigns for each leg of each plan of each person an initial (freespeed) route.
 * All given activities must have a link assigned already (use XY2Links).
 *
 * @author balmermi
 * @author mrieser
 *
 */
public class InitRoutes {
	// yyyy this functionality should move into the application contrib.  Maybe it already exists there.  Possibly without the streaming
	// functionality, which may not be needed any more anyways. kai, jul'25

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private Config config;
	private String configfile = null;
	private String plansfile = null;

	//////////////////////////////////////////////////////////////////////
	// parse methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * Parses all arguments and sets the corresponding members.
	 *
	 * @param args
	 */
	private void parseArguments(final String[] args) {
		if (args.length == 0) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}
		Iterator<String> argIter = new ArgumentParser(args).iterator();
		String arg = argIter.next();
		if (arg.equals("-h") || arg.equals("--help")) {
			printUsage();
			System.exit(0);
		} else {
			this.configfile = arg;
			this.plansfile = argIter.next();
			if (argIter.hasNext()) {
				System.out.println("Too many arguments.");
				printUsage();
				System.exit(1);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	private void printUsage() {
		System.out.println();
		System.out.println("InitRoutes");
		System.out.println("Reads a plans-file and assignes each leg in each plan of each person");
		System.out.println("a an initial route (freespeed) based on the given netowrk. The modified plans/");
		System.out.println("persons are then written out to file again.");
		System.out.println();
		System.out.println("usage: InitRoutes [OPTIONS] configfile");
		System.out.println("       The following parameters must be given in the config-file:");
		System.out.println("       - network.inputNetworkFile");
		System.out.println("       - plans.inputPlansFile");
		System.out.println("       - plans.outputPlansFile");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-h, --help:     Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final String[] args) {
		parseArguments(args);
		this.config = ConfigUtils.loadConfig(this.configfile);
		MatsimRandom.reset(config.global().getRandomSeed());
		final Scenario scenario = ScenarioUtils.createScenario(config );
//		final Population plans = PopulationUtils.createStreamingPopulation( config.plans(), null );
		StreamingPopulationReader reader = new StreamingPopulationReader( scenario ) ;

		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());

		final StreamingPopulationWriter plansWriter = new StreamingPopulationWriter();
		Gbl.assertNotNull(this.plansfile);
		plansWriter.startStreaming(this.plansfile);
		final FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(config.scoring());
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				install(AbstractModule.override( List.of( new TripRouterModule() ), new AbstractModule() {
					@Override
					public void install() {
						install(new ScenarioByInstanceModule(scenario));
						install(new TimeInterpretationModule());
						addTravelTimeBinding("car").toInstance(timeCostCalc);
						addTravelDisutilityFactoryBinding("car").toInstance(new TravelDisutilityFactory() {
							@Override
							public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
								return timeCostCalc;
							}
						});
					}
				}));
//				install( new DefaultPrepareForSimModule() );
				// needed for PrepareForSim below, which does not work (see there).
			}
		});

//		injector.getInstance( PrepareForSim.class ).run();
		// this cannot work since the scenario at this point is still empty
		// would need this as a PersonAlgo, which we could add below.  kai, jul'25

		// alternatively, we could remove this functionality, since in general such functionality is now made available by the christian rakow application contrib.  kai, 'jul25


		// yy The following creates and adds a default vehicle type for every person.  This is necessary, since we are removing the execution
		// path where routing without vehicles is possible ... note that these vehicles provide the maximum speed.  (I guess that one could
		// allow a "null" argument for the vehicle ... but it would be more error prone since we would never know if the vehicle was passed
		// through all the way to the router.)  It would be cleaner to first check if there are vehicles or vehicle types in the scenario.
		// However, this would be more work to implement ... and as stated above, the functionality would better be implemented in the
		// application contrib (and maybe already is).  So here we are just checking the config:
		Gbl.assertIf( config.qsim().getVehiclesSource()== QSimConfigGroup.VehiclesSource.defaultVehicle );

		VehicleType defaultVehicleType = VehicleUtils.createDefaultVehicleType();
		scenario.getVehicles().addVehicleType( defaultVehicleType );

		reader.addAlgorithm( new PersonAlgorithm(){
			@Override public void run( Person person ){
				Map<String, Id<Vehicle>> vehiclesByMode = new LinkedHashMap<>();
				{
					Id<Vehicle> vehicleId = Id.createVehicleId( "car_" + person.getId() );

					// add the vehicle to scenario:
					scenario.getVehicles().addVehicle( scenario.getVehicles().getFactory().createVehicle( vehicleId, defaultVehicleType ) );

					// add the vehicle ID into the person:
					vehiclesByMode.put( "car", vehicleId );
				}
				VehicleUtils.insertVehicleIdsIntoPersonAttributes( person, vehiclesByMode );
			}
		} );
		reader.addAlgorithm(new PlanRouter(injector.getInstance(TripRouter.class), null, injector.getInstance(TimeInterpretation.class)));
		reader.addAlgorithm(plansWriter);
		reader.readFile(this.config.plans().getInputFile());
		PopulationUtils.printPlansCount(reader) ;
		plansWriter.closeStreaming();

		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// main method
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		new InitRoutes().run(args);
	}

}
