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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.knowledges.Knowledges;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;

import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000v2.modules.PersonAssignAndNormalizeTimes;
import playground.balmermi.census2000v2.modules.PersonAssignModeChoiceModel;
import playground.balmermi.census2000v2.modules.PersonAssignPrimaryActivities;
import playground.balmermi.census2000v2.modules.PersonAssignShopLeisureLocations;
import playground.balmermi.census2000v2.modules.PersonSetLocationsFromKnowledge;
import playground.balmermi.census2000v2.modules.WorldParseFacilityZoneMapping;
import playground.balmermi.census2000v2.modules.WorldWriteFacilityZoneMapping;

public class IIDMGenerationPart2 {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(IIDMGenerationPart2.class);

	//////////////////////////////////////////////////////////////////////
	// createPopulation()
	//////////////////////////////////////////////////////////////////////

	public static void createIIDM(Config config) {

		log.info("MATSim-DB: create iidm.");

		ScenarioImpl scenario = new ScenarioImpl(config);
		World world = scenario.getWorld();

		//////////////////////////////////////////////////////////////////////

		log.info("  extracting input directory... ");
		String indir = config.facilities().getInputFile();
		indir = indir.substring(0,indir.lastIndexOf("/"));
		log.info("    "+indir);
		log.info("  done.");

		log.info("  extracting output directory... ");
		String outdir = config.facilities().getOutputFile();
		outdir = outdir.substring(0,outdir.lastIndexOf("/"));
		log.info("    "+outdir);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  reading world xml file...");
		final MatsimWorldReader worldReader = new MatsimWorldReader(scenario);
		worldReader.readFile(config.world().getInputFile());
		log.info("  done.");

		log.info("  reading facilities xml file...");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		world.complete(null);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  parsing additional municipality information... ");
		Municipalities municipalities = new Municipalities(indir+"/gg25_2001_infos.txt");
		municipalities.parse(world.getLayer(new IdImpl("municipality")));
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  parsing f2z_mapping... ");
		new WorldParseFacilityZoneMapping(indir+"/f2z_mapping.txt").run(world);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up population objects...");
		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		Knowledges knowledges =  scenario.getKnowledges();
		pop.setIsStreaming(true);
		PopulationWriter pop_writer = new PopulationWriter(pop, scenario.getNetwork(), knowledges);
		pop_writer.startStreaming(config.plans().getOutputFile());
		PopulationReader pop_reader = new MatsimPopulationReader(scenario);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		pop.addAlgorithm(new PersonSetLocationsFromKnowledge(knowledges, facilities));
		pop.addAlgorithm(new PersonAssignShopLeisureLocations(facilities));
		pop.addAlgorithm(new PersonAssignAndNormalizeTimes());
		PersonAssignModeChoiceModel pamcm = new PersonAssignModeChoiceModel(municipalities,outdir+"/subtours.txt", knowledges, facilities, config.planomat());
		pop.addAlgorithm(pamcm);
		pop.addAlgorithm(new PersonAssignPrimaryActivities(knowledges, facilities));
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		pop.addAlgorithm(pop_writer);
		pop_reader.readFile(config.plans().getInputFile());
		pop.printPlansCount();
		pop_writer.closeStreaming();
		pamcm.close();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  writing f2z_mapping... ");
		new WorldWriteFacilityZoneMapping(outdir+"/output_f2z_mapping.txt").run(world);
		log.info("  done.");

		log.info("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write(config.facilities().getOutputFile());
		log.info("  done.");

		log.info("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(world);
		world_writer.write(config.world().getOutputFile());
		log.info("  done.");

		log.info("  writing config xml file... ");
		new ConfigWriter(config).write(config.config().getOutputFile());
		log.info("  done.");

		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Config config = Gbl.createConfig(args);

		createIIDM(config);

		Gbl.printElapsedTime();
	}
}
