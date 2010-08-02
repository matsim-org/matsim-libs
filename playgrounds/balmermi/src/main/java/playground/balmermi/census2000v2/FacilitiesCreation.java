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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.facilities.algorithms.FacilitiesCombine;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.WorldWriter;

import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000v2.modules.FacilitiesCreateBuildingsFromCensus2000;
import playground.balmermi.census2000v2.modules.FacilitiesDistributeCenter;
import playground.balmermi.census2000v2.modules.FacilitiesRenameAndRemoveNOGAActTypes;

public class FacilitiesCreation {

	//////////////////////////////////////////////////////////////////////
	// createPopulation()
	//////////////////////////////////////////////////////////////////////

	public static void createHomeFacilities(Config config) {

		System.out.println("MATSim-DB: create Facilites based on census2000 data.");

		ScenarioImpl scenario = new ScenarioImpl(config);

		//////////////////////////////////////////////////////////////////////

		System.out.println("  extracting input directory... ");
		String indir = config.facilities().getInputFile();
		indir = indir.substring(0,indir.lastIndexOf("/"));
		System.out.println(indir);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading world xml file...");
		final MatsimWorldReader worldReader = new MatsimWorldReader(scenario);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file...");
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		scenario.getWorld().complete(config);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  running facilities modules...");
		new FacilitiesRenameAndRemoveNOGAActTypes().run(facilities);
		new FacilitiesCreateBuildingsFromCensus2000(indir+"/ETHZ_Pers.tab",scenario.getWorld().getLayer(Municipalities.MUNICIPALITY)).run(facilities);
		new FacilitiesDistributeCenter().run(facilities);
		new FacilitiesCombine().run(facilities); // to check for coord uniqueness
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  writing facilities xml file... ");
		FacilitiesWriter fac_writer = new FacilitiesWriter(facilities);
		fac_writer.write(config.facilities().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(scenario.getWorld());
		world_writer.write(config.world().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		new ConfigWriter(config).write(config.config().getOutputFile());
		System.out.println("  done.");

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Config config = Gbl.createConfig(args);

		createHomeFacilities(config);

		Gbl.printElapsedTime();
	}
}
