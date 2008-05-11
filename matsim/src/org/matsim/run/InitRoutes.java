/* *********************************************************************** *
 * project: org.matsim.*
 * XY2Links.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import java.util.Iterator;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.utils.misc.ArgumentParser;
import org.matsim.world.World;

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
	private String dtdfile = null;

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
			if (argIter.hasNext()) {
				this.dtdfile = argIter.next();
				if (argIter.hasNext()) {
					System.out.println("Too many arguments.");
					printUsage();
					System.exit(1);
				}
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
		System.out.println("usage: InitRoutes [OPTIONS] configfile [config-dtdfile]");
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
		this.config = Gbl.createConfig(new String[]{this.configfile, this.dtdfile});

		final World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		world.setNetworkLayer(network);
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());

		final Plans plans = new Plans(Plans.USE_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		final PlansWriter plansWriter = new PlansWriter(plans);
		plansWriter.writeStartPlans();
		final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
		preprocess.run(network);
		plans.addAlgorithm(new PlansCalcRouteLandmarks(network, preprocess, timeCostCalc, timeCostCalc));
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(this.config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.write();
		
		System.out.println("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// main method
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		new InitRoutes().run(args);
	}

}
