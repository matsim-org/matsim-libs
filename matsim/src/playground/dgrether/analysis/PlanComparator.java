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

import java.io.PrintWriter;
import java.util.Date;

import org.matsim.config.ConfigWriter;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.world.MatsimWorldReader;

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
	private PlanComparison _result;

	/**
   * Creates the object and computes the resulting comparison
   *
   * @param configPath
   * @param firstPlanPath
   * @param secondPlanPath
   * @param outpath
   *          if null the output is written to the console
   */
	public PlanComparator(String configPath, String firstPlanPath,
			String secondPlanPath, String outpath) {
		// create config
		initConfig(configPath);
		loadWorld();
		loadNetwork();

		// load first plans file
		this.population = loadPlansFile(firstPlanPath);
		this._result = new PlanComparison(this.population.getPersons().keySet().size());
		Plan plan;
		Act act;
		for (Id id : this.population.getPersons().keySet()) {
			plan = this.population.getPerson(id).getSelectedPlan();
			act = (Act) plan.getIteratorAct().next();
			this._result.addFirstPlansData(id, plan.getScore(), act);
		}
		// many people can be in one pop -> care about memory
		this.population = null;
		System.gc();
		// load second population
		this.population = loadPlansFile(secondPlanPath);
		for (Id id : this.population.getPersons().keySet()) {
			plan = this.population.getPerson(id).getSelectedPlan();
			this._result.addSecondPlansData(id, plan.getScore());
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
   * load the world
   *
   */
	protected void loadWorld() {
		if (Gbl.getConfig().world().getInputFile() != null) {
			printNote("", "  reading world xml file... ");
			final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
			worldReader.readFile(Gbl.getConfig().world().getInputFile());
			printNote("", "  done");
		}
		else {
			printNote("", "  No World input file given in config.xml!");
		}
	}

	/**
   * load the network
   *
   * @return the network layer
   */
	protected NetworkLayer loadNetwork() {
		// - read network: which buildertype??
		printNote("", "  creating network layer... ");
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		printNote("", "  done");

		printNote("", "  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		printNote("", "  done");

		return network;
	}

	/**
   * Load the plan file with the given path.
   *
   * @param filename
   *          the path to the filename
   * @return the Plans object containing the population
   */
	protected Population loadPlansFile(String filename) {
		Population plans = new Population(Population.NO_STREAMING);

		printNote("", "  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(plans);
		plansReader.readFile(filename);
		plans.printPlansCount();
		printNote("", "  done");

		return plans;
	}

	/**
   * Reads the configuration file
   *
   * @param configPath
   */
	private void initConfig(String configPath) {
		if (Gbl.getConfig() == null) {
			Gbl.createConfig(new String[] { configPath, "config_v1.dtd" });
		}
		else {
			Gbl.errorMsg("config exists already! Cannot create a 2nd global config from args: "
							+ configPath);
		}


		printNote("", "Complete config dump:...");
		ConfigWriter configwriter = new ConfigWriter(Gbl.getConfig(),
				new PrintWriter(System.out));
		configwriter.write();
		printNote("", "Complete config dump: done...");
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
	private final void printNote(String header, String action) {
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
	public static void main(String[] args) {
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
