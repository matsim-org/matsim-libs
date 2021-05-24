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

import java.util.Arrays;
import java.util.Iterator;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
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

/**
 * Assigns for each leg of each plan of each person an initial (freespeed) route.
 * All given activities must have a link assigned already (use XY2Links).
 *
 * @author balmermi
 * @author mrieser
 */
public class InitRoutes {

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
		final MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
//		final Population plans = PopulationUtils.createStreamingPopulation( config.plans(), null );
		StreamingPopulationReader reader = new StreamingPopulationReader( scenario ) ;

		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());

		final StreamingPopulationWriter plansWriter = new StreamingPopulationWriter();
		Gbl.assertNotNull(this.plansfile);
		plansWriter.startStreaming(this.plansfile);
		final FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
			install(AbstractModule.override(Arrays.asList(new TripRouterModule()), new AbstractModule() {
				@Override
				public void install() {
				install(new ScenarioByInstanceModule(scenario));
				addTravelTimeBinding("car").toInstance(timeCostCalc);
				addTravelDisutilityFactoryBinding("car").toInstance(new TravelDisutilityFactory() {
					@Override
					public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
						return timeCostCalc;
					}
				});
				}
			}));
			}
		});
		reader.addAlgorithm(new PlanRouter(injector.getInstance(TripRouter.class), null));
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
