/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mzilske.postgres;

import java.io.PrintWriter;
import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ArgumentParser;
import org.matsim.core.utils.misc.ConfigUtils;

public class ExportNetworkToSQL {

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

	public void run(final String inputNetworkFile, final String outputSQLFile) {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(inputNetworkFile);

		int id = 0;
		PrintWriter out = new PrintWriter(IOUtils.getBufferedWriter(outputSQLFile, false));
		for (Link link : network.getLinks().values()) {
			out.println(
					id++ + ","
					+ link.getId().toString() + ","
					+ link.getFromNode().getId() + ","
					+ link.getToNode().getId() + ","
					+ link.getFreespeed() * link.getLength() + ","
					+ link.getFromNode().getCoord().getX() + ","
					+ link.getFromNode().getCoord().getY() + ","
					+ link.getToNode().getCoord().getX() + ","
					+ link.getToNode().getCoord().getY());
		}
		out.close();

	}

	private void printUsage() {
		System.out.println("wurst");
	}

	public static void main(String[] args) {
		new ExportNetworkToSQL().run(args);
	}

}
