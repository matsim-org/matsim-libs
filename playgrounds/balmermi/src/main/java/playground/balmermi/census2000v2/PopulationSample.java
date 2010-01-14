/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationCreation.java
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

package playground.balmermi.census2000v2;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.world.World;

public class PopulationSample {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PopulationSample.class);

	//////////////////////////////////////////////////////////////////////
	// createPopulation()
	//////////////////////////////////////////////////////////////////////

	public static void samplePopulation(Config config) {

		log.info("samplePopulation...");

		ScenarioImpl scenario = new ScenarioImpl(config);
		World world = scenario.getWorld();

		//////////////////////////////////////////////////////////////////////

		log.info("  extracting input directory... ");
		String indir = config.facilities().getInputFile();
		indir = indir.substring(0,indir.lastIndexOf("/"));
		log.info("    "+indir);
		Gbl.printMemoryUsage();
		log.info("  done.");

		log.info("  extracting output directory... ");
		String outdir = config.facilities().getOutputFile();
		outdir = outdir.substring(0,outdir.lastIndexOf("/"));
		log.info("    "+outdir);
		Gbl.printMemoryUsage();
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  reading facilities xml file...");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		world.complete();
		Gbl.printMemoryUsage();
		log.info("  done.");

		System.out.println("  reading the network xml file...");
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		world.complete();
		Gbl.printMemoryUsage();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up population objects...");
		PopulationImpl pop = scenario.getPopulation();
		pop.setIsStreaming(true);
		PopulationWriter pop_writer = new PopulationWriter(pop, scenario.getNetwork());
		pop_writer.startStreaming(config.plans().getOutputFile());
		pop.addAlgorithm(pop_writer);
		PopulationReader pop_reader = new MatsimPopulationReader(scenario);
		Gbl.printMemoryUsage();
		System.out.println("  done.");


		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		pop_reader.readFile(config.plans().getInputFile());
		pop_writer.closeStreaming();
		pop.printPlansCount();
		Gbl.printMemoryUsage();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  writing network xml file... ");
		new NetworkWriter(network).writeFile(config.network().getOutputFile());
		Gbl.printMemoryUsage();
		log.info("  done.");

		log.info("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).writeFile(config.facilities().getOutputFile());
		Gbl.printMemoryUsage();
		log.info("  done.");

		log.info("  writing config xml file... ");
		new ConfigWriter(config).writeFile(config.config().getOutputFile());
		Gbl.printMemoryUsage();
		log.info("  done.");

		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Config config = Gbl.createConfig(args);

		samplePopulation(config);

		Gbl.printElapsedTime();
	}
}
