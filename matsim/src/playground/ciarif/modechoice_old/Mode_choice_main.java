/* *********************************************************************** *
 * project: org.matsim.*
 * Mode_choice_main.java
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

package playground.ciarif.modechoice_old;

import org.matsim.config.ConfigWriter;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.algorithms.NetworkCalcTopoType;
import org.matsim.network.algorithms.NetworkSummary;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.algorithms.PersonCreatePlanFromKnowledge;
import org.matsim.plans.algorithms.PlansCreateFromNetwork;
import org.matsim.plans.algorithms.PlansDefineKnowledge;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldValidation;

public class Mode_choice_main {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void testRun01() {
		
		System.out.println("TEST RUN 01:");
		final World world = Gbl.getWorld();
		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		System.out.println("  creating network layer... ");
		NetworkLayer network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		System.out.println("  done.");

		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		System.out.println();
		System.out.println("1. VALIDATE AND COMPLETE THE WORLD");
		System.out.println();

		System.out.println("  running world algorithms... ");
		new WorldValidation().run(world);
		new WorldBottom2TopCompletion().run(world);
		System.out.println("  done.");

		System.out.println();
		System.out.println("2. SUMMARY INFORMATION OF THE NETWORK");
		System.out.println();

		System.out.println("  running network algorithms... ");
		NetworkSummary ns_algo = new NetworkSummary();
		ns_algo.run(network);
		new NetworkCalcTopoType().run(network);
		System.out.println("  done.");

		System.out.println();
		System.out.println("3. CREATING A POPULATION BASED ON THE NETWORK");
		System.out.println();

		System.out.println("  creating plans object... ");
		Plans plans = new Plans();
		System.out.println("  done.");

		System.out.println("  running plans algorithms... ");
		PlansCreateFromNetwork pcfn_algo = new PlansCreateFromNetwork(network,ns_algo, 0);
		plans.addAlgorithm(pcfn_algo);
		plans.runAlgorithms();
		System.out.println("  done.");

		System.out.println();
		System.out.println("4. AGGREGATION OF THE FACILITIES TO THE NETWORK LEVEL");
		System.out.println();

		System.out.println("  reading facilities xml file... ");
		//FacilitiesParser facilities_parser = new FacilitiesParser(Facilities.getSingleton());
		//facilities_parser.parse();
		System.out.println("  done.");

		System.out.println("  running facilities algorithms... ");
		//Facilities.getSingleton().addAlgorithm(new FacilitiesAggregation(network));
		//Facilities.getSingleton().runAlgorithms();
		System.out.println("  done.");

		System.out.println();
		System.out.println("5. DEFINE CAPACITIES AND OPENTIMES FOR THE FACILITIES BASED ON THE POPULATION");
		System.out.println();

		System.out.println("  running facilities algorithms... ");
		//Facilities.getSingleton().clearAlgorithms();
		//Facilities.getSingleton().addAlgorithm(new FacilitiesDefineCapAndOpentime(plans.getPersons().size()));
		//Facilities.getSingleton().runAlgorithms();
		System.out.println("  done.");

		System.out.println();
		System.out.println("6. DEFINE SOME KNOWLEDGE FOR THE POPULATION");
		System.out.println();

		System.out.println("  running plans algorithms... ");
		plans.clearAlgorithms();
		plans.addAlgorithm(new PlansDefineKnowledge());
		plans.runAlgorithms();
		System.out.println("  done.");

		System.out.println();
		System.out.println("7. CREATE AN INITIAL DAYPLAN FOR EACH PERSON ACCORDING TO THEIR KNOWLEDGE");
		System.out.println();

		System.out.println("  running plans algorithms... ");
		plans.clearAlgorithms();
		plans.addAlgorithm(new PersonCreatePlanFromKnowledge());
		//plans.addAlgorithm(new FrancescoAlgo());
		plans.addAlgorithm (new TicketAlgo ());
			
		plans.addAlgorithm (new ModeChoiceAlgorithm ());
		//plans.addAlgorithm (new ModeAlgo ());
		plans.runAlgorithms();
		System.out.println("  done.");

		System.out.println();
		System.out.println("8. WRITING DOWN ALL DATA");
		System.out.println();

		System.out.println("  writing facilities xml file... ");
		//FacilitiesWriter facilities_writer = new FacilitiesWriter(Facilities.getSingleton());
		//facilities_writer.write();
		System.out.println("  done.");

		System.out.println("  writing plans xml file... ");
		PlansWriter plans_writer = new PlansWriter(plans);
		plans_writer.write();
		System.out.println("  done.");

		System.out.println("  writing network xml file... ");
		NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write();
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(Gbl.getWorld());
		world_writer.write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
		config_writer.write();
		System.out.println("  done.");
		System.out.println("TEST SUCCEEDED.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Gbl.startMeasurement();
		Gbl.createConfig(args);
		Gbl.createWorld();
		//Gbl.createFacilities();

		testRun01();
		Gbl.printElapsedTime();
	}

}