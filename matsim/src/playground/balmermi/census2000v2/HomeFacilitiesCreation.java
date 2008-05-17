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

import org.matsim.config.ConfigWriter;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.WorldWriter;

import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000v2.data.Households;
import playground.balmermi.census2000v2.modules.FacilitiesCreateBuildingsFromCensus2000;
import playground.balmermi.census2000v2.modules.HouseholdsCreateFromCensus2000;
import playground.balmermi.census2000v2.modules.PlansCreateFromCensus2000;

public class HomeFacilitiesCreation {

	//////////////////////////////////////////////////////////////////////
	// createPopulation()
	//////////////////////////////////////////////////////////////////////

	public static void createHomeFacilities() {

		System.out.println("MATSim-DB: create Facilites based on census2000 data.");

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating Facilities object... ");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		Gbl.getWorld().complete();
		System.out.println("  done.");

		System.out.println("  extracting input directory... ");
		String indir = Gbl.getConfig().facilities().getInputFile();
		indir = indir.substring(0,indir.lastIndexOf("/"));
		System.out.println(indir);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  parsing additional municipality information... ");
		Municipalities municipalities = new Municipalities(indir+"/gg25_2001_infos.txt");
		municipalities.parse();
		System.out.println("  done.");

		System.out.println("  creating Households object... ");
		Households households = new Households(municipalities);
		System.out.println("  done.");

//		System.out.println("  creating Humans object... ");
//		Humans humans = new Humans();
//		System.out.println("  done.");
//		new HumansCreateFromCensus2000(indir+"/KANT_16.tab",households).run(humans);
//		new HumansCreateFromCensus2000(indir+"/ETHZ_Pers.tab",households).run(humans);

		System.out.println("  creating plans object...");
		Plans plans = new Plans(Plans.NO_STREAMING);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  running facilities module... ");
		new FacilitiesCreateBuildingsFromCensus2000(indir+"/KANT_16.tab",Gbl.getWorld().getLayer(Municipalities.MUNICIPALITY)).run(facilities);
		new HouseholdsCreateFromCensus2000(indir+"/KANT_16.tab",facilities,municipalities).run(households);
		new PlansCreateFromCensus2000(indir+"/KANT_16.tab",households).run(plans);

//		new FacilitiesCreateBuildingsFromCensus2000(indir+"/ETHZ_Pers.tab",Gbl.getWorld().getLayer(Municipalities.MUNICIPALITY)).run(facilities);
//		new HouseholdsCreateFromCensus2000(indir+"/ETHZ_Pers.tab",facilities,municipalities).run(households);
//		new PlansCreateFromCensus2000(indir+"/ETHZ_Pers.tab",households).run(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

//		System.out.println("  dumping households...");
//		households.print();
//		System.out.println("  done.");
		
		//////////////////////////////////////////////////////////////////////

		System.out.println("  writing plans xml file... ");
		PlansWriter plans_writer = new PlansWriter(plans);
		plans_writer.write();
		System.out.println("  done.");

		System.out.println("  writing facilities xml file... ");
		FacilitiesWriter fac_writer = new FacilitiesWriter(facilities);
		fac_writer.write();
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(Gbl.getWorld());
		world_writer.write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
		config_writer.write();
		System.out.println("  done.");

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Gbl.createConfig(args);
		Gbl.createWorld();

		createHomeFacilities();

		Gbl.printElapsedTime();
	}
}
