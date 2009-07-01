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

package playground.dgrether.analysis;

import java.util.Date;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.network.Network;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;

/**
 * This Class is able to compare two plan files of different iterations. In a
 * config file the world and network must be given. The arguments needed to run
 * this tool will be described when called without arguments.
 *
 * @author dgrether
 *
 */
public class PlanComparator {

	/**
   * the object holding the population
   */
	private Population population;

	/**
   * the data needed to compare the plans is stored here
   */
	private final PlanComparison _result;

	/**
   * Creates the object and computes the resulting comparison
   *
   * @param configPath
   * @param firstPlanPath
   * @param secondPlanPath
   * @param outpath
   *          if null the output is written to the console
   */
	public PlanComparator(final String configPath, final String firstPlanPath,
			final String secondPlanPath, final String outpath) {

		ScenarioLoader sl = new ScenarioLoader(configPath);
		sl.loadNetwork();

		// load first plans file
		this.population = loadPlansFile(firstPlanPath, sl.getScenario().getNetwork());
		this._result = new PlanComparison(this.population.getPersons().keySet().size());
		PlanImpl plan;
		ActivityImpl act;
		for (Id id : this.population.getPersons().keySet()) {
			plan = this.population.getPersons().get(id).getSelectedPlan();
			act = plan.getFirstActivity();
			this._result.addFirstPlansData(id, plan.getScore().doubleValue(), act);
		}
		// many people can be in one pop -> care about memory
		this.population = null;
		System.gc();
		// load second population
		this.population = loadPlansFile(secondPlanPath, sl.getScenario().getNetwork());
		for (Id id : this.population.getPersons().keySet()) {
			plan = this.population.getPersons().get(id).getSelectedPlan();
			this._result.addSecondPlansData(id, plan.getScore().doubleValue());
		}

		if (outpath == null) {
			PlanComparisonStringWriter writer = new PlanComparisonStringWriter();
			writer.write(this._result);
			System.out.println(writer.getResult());
		}
		else {
			new PlanComparisonFileWriter(outpath).write(this._result);
			System.out.println("Results written to: " + outpath);
		}
	}

	/**
   * Load the plan file with the given path.
   *
   * @param filename
   *          the path to the filename
   * @return the Plans object containing the population
   */
	protected Population loadPlansFile(final String filename, Network network) {
		Population plans = new PopulationImpl();

		printNote("", "  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(plans, network);
		plansReader.readFile(filename);
		printNote("", "  done");

		return plans;
	}

	/**
   * an internal routine to generated some (nicely?) formatted output. This
   * helps that status output looks about the same every time output is written.
   *
   * @param header
   *          the header to print, e.g. a module-name or similar. If empty
   *          <code>""</code>, no header will be printed at all
   * @param action
   *          the status message, will be printed together with a timestamp
   */
	private final void printNote(final String header, final String action) {
		if (header != "") {
			System.out.println();
			System.out
					.println("===============================================================");
			System.out.println("== " + header);
			System.out
					.println("===============================================================");
		}
		if (action != "") {
			System.out.println("== " + action + " at " + (new Date()));
		}
		if (header != "") {
			System.out.println();
		}
	}

	private static void printHelp() {
		// String ls = System.getProperty("line.separator");
		System.out.println("This tool needs three or optional four arguments: ");
		System.out.println("1. the path to the config file (mandatory)");
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
		if (args.length == 3) {
			new PlanComparator(args[0], args[1], args[2], null);
		}
		else if (args.length == 4) {
			new PlanComparator(args[0], args[1], args[2], args[3]);
		}
		else
			printHelp();

	}

}
