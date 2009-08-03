/* *********************************************************************** *
 * project: org.matsim.*
 * PlanComparator.java
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

package playground.dgrether.analysis.population;

import org.matsim.api.core.v01.ScenarioImpl;


/**
 * This Class is able to compare two plan files of different iterations. 
 * The arguments needed to run
 * this tool will be described when called without arguments.
 *
 * @author dgrether
 *
 */
public class PlanComparator {

	public PlanComparator() {}

	private static void printHelp() {
		// String ls = System.getProperty("line.separator");
		System.out.println("This tool needs three or optional four arguments: ");
		System.out.println("1. the path to the network file (mandatory)");
		System.out
				.println("2. the path to the population of the first iteration (mandatory)");
		System.out
				.println("3. the path to the population of the second iteration (mandatory)");
		System.out.println("4. the path for the output file (optional)");
		System.out
				.println("If no output path is given, output is written to stdout");
	}

	/**
   * Should be called with 3 arguments, each of them a path to a file: 1. the
   * config file containing the world and the network 2. the first plan file 3.
   * the second plan file
   *
   * @param args
   */
	public static void main(final String[] args) {
		DgAnalysisPopulation pop;
		ScenarioImpl sc = new ScenarioImpl();
		pop = new DgPopulationAnalysisReader(sc).doPopulationAnalysis(args[0], args[1], args[2]);
		if (args.length == 3) {
			System.out.println(new PlanComparisonStringWriter(pop).getResult());
		}
		else if (args.length == 4) {
			new PlanComparisonFileWriter(pop).write(args[3]);
		}
		else
			printHelp();

	}

}
