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

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

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

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
//		World world = scenario.getWorld();

		//////////////////////////////////////////////////////////////////////

		log.info("  extracting input directory... ");
		String indir = config.facilities().getInputFile();
		indir = indir.substring(0,indir.lastIndexOf("/"));
		log.info("    "+indir);
		Gbl.printMemoryUsage();
		log.info("  done.");

		log.info("  extracting output directory... ");
		String outdir = null;
		outdir = outdir.substring(0,outdir.lastIndexOf("/"));
		log.info("    "+outdir);
		Gbl.printMemoryUsage();
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  reading facilities xml file...");
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
//		world.complete(config);
		Gbl.printMemoryUsage();
		log.info("  done.");

		System.out.println("  reading the network xml file...");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
//		world.complete(config);
		Gbl.printMemoryUsage();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up population objects...");
//		Population reader = (Population) scenario.getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader( scenario ) ;
		StreamingUtils.setIsStreaming(reader, true);
		StreamingPopulationWriter pop_writer = new StreamingPopulationWriter(null, scenario.getNetwork());
		pop_writer.startStreaming(null);
		final PersonAlgorithm algo = pop_writer;//config.plans().getOutputFile());
		reader.addAlgorithm(algo);
//		PopulationReader pop_reader = new MatsimPopulationReader(scenario);
		Gbl.printMemoryUsage();
		System.out.println("  done.");


		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
//		pop_reader.readFile(config.plans().getInputFile());
		reader.readFile(config.plans().getInputFile());
		pop_writer.closeStreaming();
		PopulationUtils.printPlansCount(reader) ;
		Gbl.printMemoryUsage();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws IOException {

		Gbl.startMeasurement();

		Config config = ConfigUtils.loadConfig(args[0]);

		samplePopulation(config);

		Gbl.printElapsedTime();
	}
}
