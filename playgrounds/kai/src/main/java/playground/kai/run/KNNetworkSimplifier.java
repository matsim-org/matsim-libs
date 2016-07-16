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

package playground.kai.run;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matsim4urbansim.utils.network.NetworkSimplifier;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ArgumentParser;

/**
 * Reads a network from file and simplifies it, using various methods
 * 
 * @author nagel based on mrieser
 */
public class KNNetworkSimplifier {
	
	private static void printUsage() {
		System.out.println();
		System.out.println("KNNetworkSimplifier");
		System.out.println("Reads a network-file and \"simplifies\" it. Need to read the source code to figure out what it is doing ...");
		System.out.println();
		System.out.println("usage: NetworkCleaner [OPTIONS] input-network-file output-network-file");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-h, --help:     Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2013, matsim.org");
		System.out.println();
	}
	
	/** Runs the network cleaning algorithms over the network read in from <code>inputNetworkFile</code>
	 * and writes the resulting ("cleaned") network to the specified file.
	 * 
	 * @param inputNetworkFile filename of the network to be handled
	 * @param outputNetworkFile filename where to write the cleaned network to
	 */
	public static void run(final String inputNetworkFile, final String outputNetworkFile) {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputNetworkFile);

		NetworkCleaner cleaner = new org.matsim.core.network.algorithms.NetworkCleaner() ;
		cleaner.run( network) ;
		
		NetworkSimplifier simplifier = new org.matsim.contrib.matsim4urbansim.utils.network.NetworkSimplifier() ;
		simplifier.setMergeLinkStats(true);
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add( NetworkCalcTopoType.PASS1WAY );
		// (if you look at the code, this means a node where a 1-way street passes through)
		nodeTypesToMerge.add( NetworkCalcTopoType.PASS2WAY );
		// (if you look at the code, this means a node where a 2-way street passes through)
		simplifier.setNodesToMerge(nodeTypesToMerge);
		simplifier.run(network);

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
		new KNNetworkSimplifier().run(args);
	}

}
