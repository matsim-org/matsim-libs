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
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.balmermi.census2000v2.modules.PersonAssignToNetwork;
import playground.balmermi.world.World;

public class IIDMAssign2Network {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(IIDMAssign2Network.class);

	//////////////////////////////////////////////////////////////////////
	// createPopulation()
	//////////////////////////////////////////////////////////////////////

	public static void assignNetwork(Config config) {

		log.info("MATSim-DB: assignNetwork...");

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		World world = new World();

		//////////////////////////////////////////////////////////////////////

		log.info("  extracting input directory... ");
		String indir = config.facilities().getInputFile();
		indir = indir.substring(0,indir.lastIndexOf("/"));
		log.info("    "+indir);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  reading facilities xml file...");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		log.info("  done.");

		System.out.println("  reading the network xml file...");
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  running world modules... ");
		// the link types where no facility can be placed on
		// Here: for the ivtch network (nationales Netzmodell)
		Set<String> excludingLinkTypes = new HashSet<String>();
		excludingLinkTypes.add("0"); excludingLinkTypes.add("1"); excludingLinkTypes.add("2"); excludingLinkTypes.add("3");
		excludingLinkTypes.add("4"); excludingLinkTypes.add("5"); excludingLinkTypes.add("6"); excludingLinkTypes.add("7");
		excludingLinkTypes.add("8"); excludingLinkTypes.add("9");
		excludingLinkTypes.add("10"); excludingLinkTypes.add("11"); excludingLinkTypes.add("12"); excludingLinkTypes.add("13");
		excludingLinkTypes.add("14"); excludingLinkTypes.add("15"); excludingLinkTypes.add("16"); excludingLinkTypes.add("17");
		excludingLinkTypes.add("18"); excludingLinkTypes.add("19");
		excludingLinkTypes.add("20"); excludingLinkTypes.add("21"); excludingLinkTypes.add("22"); excludingLinkTypes.add("23");
		excludingLinkTypes.add("24"); excludingLinkTypes.add("25"); excludingLinkTypes.add("26"); excludingLinkTypes.add("27");
		excludingLinkTypes.add("28"); excludingLinkTypes.add("29");
		excludingLinkTypes.add("90"); excludingLinkTypes.add("91"); excludingLinkTypes.add("92"); excludingLinkTypes.add("93");
		excludingLinkTypes.add("94"); excludingLinkTypes.add("95"); excludingLinkTypes.add("96"); excludingLinkTypes.add("97");
		excludingLinkTypes.add("98"); excludingLinkTypes.add("99");
//		new WorldConnectLocations(excludingLinkTypes, config).run(world);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up population objects...");
		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		pop.setIsStreaming(true);
		PopulationWriter pop_writer = new PopulationWriter(pop, network);
		pop_writer.startStreaming(null);//config.plans().getOutputFile());
		PopulationReader pop_reader = new MatsimPopulationReader(scenario);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		pop.addAlgorithm(new PersonAssignToNetwork(network, facilities, config));
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		pop.addAlgorithm(pop_writer);
		pop_reader.readFile(config.plans().getInputFile());
		pop.printPlansCount();
		pop_writer.closeStreaming();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write(null);//config.facilities().getOutputFile());
		log.info("  done.");

		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws IOException {

		Gbl.startMeasurement();

		Config config = ConfigUtils.loadConfig(args[0]);

		assignNetwork(config);

		Gbl.printElapsedTime();
	}
}
