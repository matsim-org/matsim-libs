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

import java.util.Iterator;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Plans;
import org.matsim.population.PlansReaderI;
import org.matsim.population.PlansWriter;
import org.matsim.utils.misc.ArgumentParser;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;

/**
 * Assigns each activity in each plan of each person in the population a link
 * where the activity takes place based on the coordinates given for the activity.
 * This tool is used for mapping a new demand/population to a network for the first time.
 *
 * @author mrieser
 */
public class XY2Links {

	private Config config;
	private String configfile = null;
	private String dtdfile = null;

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

	private void printUsage() {
		System.out.println();
		System.out.println("XY2Links");
		System.out.println("Reads a plans-file and assignes each activity in each plan of each person");
		System.out.println("a link based on the coordinates given in the activity. The modified plans/");
		System.out.println("persons are then written out to file again.");
		System.out.println();
		System.out.println("usage: XY2Links [OPTIONS] configfile [config-dtdfile]");
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

	/** Starts the assignment of links to activities.
	 *
	 * @param args command-line arguments
	 */
	public void run(final String[] args) {
		parseArguments(args);
		this.config = Gbl.createConfig(new String[]{this.configfile, this.dtdfile});

		final World world = Gbl.getWorld();

		if (this.config.world().getInputFile() != null) {
			final MatsimWorldReader worldReader = new MatsimWorldReader(world);
			worldReader.readFile(this.config.world().getInputFile());
		}

		NetworkLayer network = new NetworkLayer();
		world.setNetworkLayer(network);
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());

		final Plans plans = new Plans(Plans.USE_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		final PlansWriter plansWriter = new PlansWriter(plans);
		plans.addAlgorithm(new org.matsim.population.algorithms.XY2Links(network));
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(this.config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.write();

		System.out.println("done.");
	}

	/**
	 * Main method to start the assignment of links to activities.
	 *
	 * @param args Array of arguments, usually passed on the command line.
	 */
	public static void main(final String[] args) {
		new XY2Links().run(args);
	}

}
