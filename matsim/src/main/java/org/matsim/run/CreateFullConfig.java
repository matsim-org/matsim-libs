/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.utils.misc.ArgumentParser;

/**
 * A very simple class which just dumps a clean config
 * with all settings and their default values to a file.
 * Useful to get a list of available settings for many users.
 * <p></p>
 * Run as {@code java -cp .../matsim.jar org.matsim.run.CreateFullConfig config.xml} .
 * <p></p>
 * An example from the development head can be found <a href="http://ci.matsim.org:8080/job/MATSim_M2/ws/matsim/test/output/org/matsim/run/CreateFullConfigTest/testMain/newConfig.xml">here</a>.
 * <p></p>
 * This class is mentioned in the User Guide.
 * 
 * @author mrieser / Senozon AG
 *
 */
public class CreateFullConfig {

	private static String configFilename = null;

	public static void main(String[] args) {
		parseArguments(args);

		Config config = ConfigUtils.createConfig();
		new ConfigWriter(config).write(configFilename);
	}

	private static void parseArguments(final String[] args) {
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
			configFilename = arg;
			if (argIter.hasNext()) {
				System.out.println("Too many arguments.");
				printUsage();
				System.exit(1);
			}
		}
	}

	private static void printUsage() {
		System.out.println();
		System.out.println("CreateFullConfig");
		System.out.println();
		System.out.println("Writes a new configuration file with all known parameters and their default");
		System.out.println("values.");
		System.out.println();
		System.out.println("usage: CreateFullConfig path/to/config.xml");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-h, --help:     Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2014, matsim.org");
		System.out.println();
	}
}
