/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCleaner.java
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

package playground.toronto;

import java.util.Iterator;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.misc.ArgumentParser;

/**
 * Reads a network from file and "cleans" it to ensure the network is suited for simulation. Currently,
 * it is ensured that every link can be reached by every other link.
 *
 * @author mrieser
 */
public class NetworkCleaner {
	
	private void printUsage() {
		System.out.println();
		System.out.println("NetworkCleaner");
		System.out.println("Reads a network-file and \"cleans\" it. Currently, it performs the following");
		System.out.println("steps to ensure a network is suited for simulation:");
		System.out.println(" - ensure every link can be reached by every other link. It looks for the");
		System.out.println("   biggest cluster of connected nodes and links and removes all other elements.");
		System.out.println();
		System.out.println("usage: NetworkCleaner [OPTIONS] input-network-file output-network-file");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-h, --help:     Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}
	
	/** Runs the network cleaning algorithms over the network read in from <code>inputNetworkFile</code>
	 * and writes the resulting ("cleaned") network to the specified file.
	 * 
	 * @param inputNetworkFile filename of the network to be handled
	 * @param outputNetworkFile filename where to write the cleaned network to
	 */
	public void run(final String inputNetworkFile, final String outputNetworkFile) {
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(inputNetworkFile);

		new org.matsim.core.network.algorithms.NetworkCleaner().run(network);

		new NetworkWriter(network).write(outputNetworkFile);
	}
	
	/** Runs the network cleaning algorithms over the network read in from the argument list, and
	 * writing the resulting network out to a file again
	 * 
	 * @param args <code>args[0]</code> filename of the network to be handled, 
	 * <code>args[1]</code> filename where to write the cleaned network to
	 */
	public void run(final String[] args) {
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
			String inputFile = arg;
			if (!argIter.hasNext()) {
				System.out.println("Too few arguments.");
				printUsage();
				System.exit(1);
			}
			String outputFile = argIter.next();
			if (argIter.hasNext()) {
				System.out.println("Too many arguments.");
				printUsage();
				System.exit(1);
			}
			run(inputFile, outputFile);
		}
	}

	public static void main(String[] args) {
		new NetworkCleaner().run(new String[] {"C:/workspace/matsim/input/NetworkCleaner/network.xml", "C:/workspace/matsim/output/NetworkCleaner/network.xml"});
	}

}
