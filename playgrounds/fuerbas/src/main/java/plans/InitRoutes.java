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

package plans;

import java.util.Iterator;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ArgumentParser;

/**
 * Assigns for each leg of each plan of each person an initial (freespeed) route.
 * All given activities must have a link assigned already (use XY2Links).
 *
 * @author balmermi
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
		System.out.println("usage: InitRoutes [OPTIONS] configfile outputPlansFile");
		System.out.println("       The following parameters must be given in the config-file:");
		System.out.println("       - network.inputNetworkFile");
		System.out.println("       - plans.inputPlansFile");
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
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(this.configfile);
		sl.loadNetwork();
		Network network = sl.getScenario().getNetwork();
		this.config = sl.getScenario().getConfig();

		final PopulationImpl plans = (PopulationImpl) sl.getScenario().getPopulation();
		plans.setIsStreaming(true);
		final PopulationReader plansReader = new MatsimPopulationReader(sl.getScenario());
		final PopulationWriter plansWriter = new PopulationWriter(plans, network);
		plansWriter.startStreaming(this.plansfile);
		final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(config.planCalcScore());
		plans.addAlgorithm(new PlansCalcRoute(this.config.plansCalcRoute(), network, timeCostCalc, timeCostCalc, new AStarLandmarksFactory(network, timeCostCalc)));
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(this.config.plans().getInputFile());
		plans.printPlansCount();
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
