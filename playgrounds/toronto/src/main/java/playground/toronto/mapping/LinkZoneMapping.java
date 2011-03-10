/* *********************************************************************** *
 * project: org.matsim.*
 * LinkZoneMapping.java
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

package playground.toronto.mapping;

import java.util.Iterator;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ArgumentParser;
import org.matsim.core.utils.misc.ConfigUtils;

public class LinkZoneMapping {
	
	private void printUsage() {
		System.out.println();
		System.out.println("LinkZoneMapping");
		System.out.println("Reads the toronto network-file and creates a link-zone mapping file.");
		System.out.println("Network node id < 10'000 are defined as zone centroids. Link are assigned");
		System.out.println("to the nearest zone centroid.");
		System.out.println();
		System.out.println("usage: LinkZoneMapping [OPTIONS] input-network-file output-mapping file");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-h, --help:     Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}
	
	public void run(final String inputNetworkFile, final String outputMappingFile) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(inputNetworkFile);
		new NetworkCreateL2ZMapping(outputMappingFile).run(network);
	}
	
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
			run(inputFile,outputFile);
		}
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			// default
			new LinkZoneMapping().run(new String[]{"../../input/network.xml.gz","../../output/l2z-mapping.txt"});
		}
		else {
			new LinkZoneMapping().run(args);
		}
	}
}
