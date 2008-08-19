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
import org.matsim.config.ConfigWriter;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.WorldWriter;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldValidation;

import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000v2.data.Households;
import playground.balmermi.census2000v2.modules.PersonAssignLicenseModel;
import playground.balmermi.census2000v2.modules.PlansWriteCustomAttributes;
import playground.balmermi.census2000v2.modules.PopulationAddCustomAttributes;
import playground.balmermi.census2000v2.modules.WorldParseFacilityZoneMapping;
import playground.balmermi.census2000v2.modules.WorldWriteFacilityZoneMapping;

public class IIDMGeneration {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(IIDMGeneration.class);

	//////////////////////////////////////////////////////////////////////
	// createPopulation()
	//////////////////////////////////////////////////////////////////////

	public static void createIIDM() {

		log.info("MATSim-DB: create iidm.");

		//////////////////////////////////////////////////////////////////////

		log.info("  extracting input directory... ");
		String indir = Gbl.getConfig().facilities().getInputFile();
		indir = indir.substring(0,indir.lastIndexOf("/"));
		log.info("    "+indir);
		log.info("  done.");

		log.info("  extracting output directory... ");
		String outdir = Gbl.getConfig().facilities().getOutputFile();
		outdir = outdir.substring(0,outdir.lastIndexOf("/"));
		log.info("    "+outdir);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  reading world xml file...");
		final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		log.info("  done.");

		log.info("  reading facilities xml file...");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		Gbl.getWorld().complete();
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////
		
		log.info("  parsing additional municipality information... ");
		Municipalities municipalities = new Municipalities(indir+"/gg25_2001_infos.txt");
		municipalities.parse();
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  running world modules... ");
		new WorldCheck().run(Gbl.getWorld());
		new WorldValidation().run(Gbl.getWorld());
		log.info("  done.");
		
		//////////////////////////////////////////////////////////////////////

		log.info("  parsing f2z_mapping... ");
		new WorldParseFacilityZoneMapping(indir+"/f2z_mapping.txt").run(Gbl.getWorld());
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////

		log.info("  running world modules... ");
		new WorldCheck().run(Gbl.getWorld());
		new WorldValidation().run(Gbl.getWorld());
		log.info("  done.");
		
		//////////////////////////////////////////////////////////////////////

		log.info("  reding plans xml file... ");
		Population pop = new Population(Population.NO_STREAMING);
		new MatsimPopulationReader(pop).readFile(Gbl.getConfig().plans().getInputFile());
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////
		
		// TODO: write some consistency tests

		//////////////////////////////////////////////////////////////////////

		log.info("  parsing households... ");
		Households households = new Households(municipalities);
		households.parse(indir+"/households.txt",pop);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////
		
//		// Test Demand for Timo Smiezek
//		// ch.cut.640000.200000.740000.310000.xml
//		Coord min = new CoordImpl(640000.0,200000.0);
//		Coord max = new CoordImpl(740000.0,310000.0);

//		log.info("  running person modules... ");
//		new PersonCreateFakePlanFromKnowledge().run(plans);
//		new PlansScenarioCut(min,max).run(plans);
//		log.info("  done.");
		
		//////////////////////////////////////////////////////////////////////
		
		log.info("  adding custom attributes for persons... ");
		new PopulationAddCustomAttributes(indir+"/ETHZ_Pers.tab").run(pop);
		log.info("  done.");
		
		//////////////////////////////////////////////////////////////////////

		log.info("  runnning person models... ");
		new PersonAssignLicenseModel().run(pop);
		log.info("  done.");
		
		//////////////////////////////////////////////////////////////////////

		log.info("  writing custom attributes of the persons... ");
		new PlansWriteCustomAttributes(outdir+"/output_persons.txt").run(pop);
		log.info("  done.");

		log.info("  writing households txt file... ");
		households.writeTable(outdir+"/output_households.txt");
		log.info("  done.");

		log.info("  writing plans xml file... ");
		PopulationWriter plans_writer = new PopulationWriter(pop);
		plans_writer.write();
		log.info("  done.");

		log.info("  writing f2z_mapping... ");
		new WorldWriteFacilityZoneMapping(outdir+"/output_f2z_mapping.txt").run(Gbl.getWorld());
		log.info("  done.");

		log.info("  writing facilities xml file... ");
		FacilitiesWriter fac_writer = new FacilitiesWriter(facilities);
		fac_writer.write();
		log.info("  done.");

		log.info("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(Gbl.getWorld());
		world_writer.write();
		log.info("  done.");

		log.info("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
		config_writer.write();
		log.info("  done.");

		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Gbl.createConfig(args);
		Gbl.createWorld();

		createIIDM();

		Gbl.printElapsedTime();
	}
}
