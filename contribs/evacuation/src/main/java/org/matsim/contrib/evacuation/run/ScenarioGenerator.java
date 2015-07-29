/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.run;


/**
 * This class is part of the GRIPS project (GIS based risk
assessment and incident preparation system)
 * This class provides an entry point to the ``real'' ScenarioGenerator
 * @author laemmel
 *
 */
public class ScenarioGenerator {
	public static void main(String [] args) {
		if (args.length != 1) {
			printUsage();
			System.exit(-1);
		}

		new org.matsim.contrib.evacuation.scenariogenerator.ScenarioGenerator(args[0]).run();

	}

	protected static void printUsage() {
		System.out.println();
		System.out.println("ScenarioGenerator");
		System.out.println("Generates a MATSim scenario from meta format input files.");
		System.out.println();
		System.out.println("usage : ScenarioGenerator config-file");
		System.out.println();
		System.out.println("config-file:   A MATSim config file that gives the location to the input files needed for creating an evacuation scenario.");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2011, 2012, matsim.org");
		System.out.println();
	}
}
