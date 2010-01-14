/* *********************************************************************** *
 * project: org.matsim.*
 * ModeChoiceNavteqMain.java
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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.world.World;




public class ModeChoiceNavteqMain {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////



	public static void testRun01(Config config) {
		ScenarioImpl scenario = new ScenarioImpl(config);
		World world = scenario.getWorld();

//		System.out.println("TEST RUN 01:");
//		System.out.println("  reading world xml file... ");
//		WorldParser world_parser = new WorldParser(Gbl.getWorld());
//		world_parser.parse();
//		System.out.println("  done.");

		System.out.println("  reading network xml file... ");
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

//		System.out.println();
//		System.out.println("1. VALIDATE AND COMPLETE THE WORLD");
//		System.out.println();
//		System.out.println("  running world algorithms... ");
//		Gbl.getWorld().addAlgorithm(new WorldValidation());
//		Gbl.getWorld().addAlgorithm(new WorldBottom2TopCompletion());
//		Gbl.getWorld().runAlgorithms();
//		System.out.println("  done.");
//
//		System.out.println();
//		System.out.println("2. SUMMARY INFORMATION OF THE NETWORK");
//		System.out.println();
//		System.out.println("  running network algorithms... ");
//		NetworkSummary ns_algo = new NetworkSummary();
//		network.addAlgorithm(ns_algo);
//		network.addAlgorithm(new NetworkCalcTopoType());
//		network.runAlgorithms();
//		System.out.println("  done.");

//		System.out.println();
//		System.out.println("3. CREATING A POPULATION BASED ON THE NETWORK");
//		System.out.println();
//		System.out.println("  creating plans object... ");
		PopulationImpl plans = scenario.getPopulation();
		plans.setIsStreaming(true);
//		System.out.println("  done.");
//		System.out.println("  running plans algorithms... ");
//		PlansCreateFromNetwork pcfn_algo = new PlansCreateFromNetwork(network,ns_algo);
//		plans.addAlgorithm(pcfn_algo);
//		plans.runAlgorithms();
//		System.out.println("  done.");

//		PlansParser plansParser = new MatsimPopulationReader(plans);
		PopulationWriter plansWriter = new PopulationWriter(plans, network);
		plansWriter.startStreaming(config.plans().getOutputFile());
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		System.out.println("  done.");
		System.out.println("  adding plans algorithm... ");
		//plans.addAlgorithm (new ModeChoiceAlgorithm2 ());
		plans.addAlgorithm(new ModeChoiceAlgorithm ());
		ModeChoiceAnalyzer modeAnalyzer = new ModeChoiceAnalyzer();
		plans.addAlgorithm(modeAnalyzer);
		System.out.println("  done.");
		System.out.println("  reading, processing, writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
		plans.runAlgorithms();
		modeAnalyzer.printInformation();
		modeAnalyzer.writeStatistics("/var/tmp/ciarif/matsim/modeChoice/modeStatistics.csv");
		modeAnalyzer.writeStatistics("C:/Documents and Settings/ciarif/sandbox00/vsp-cvs/" +
				"devel/matsim/matsimJ/output/schweiz-navtech/modeStatistics.csv");
		plansWriter.closeStreaming();
		System.out.println("  done.");
		System.out.println("RUN: ModeChoiceAlgorithm finished.");
		System.out.println();

//		System.out.println("4. AGGREGATION OF THE FACILITIES TO THE NETWORK LEVEL");
//		System.out.println();
//		System.out.println("  reading facilities xml file... ");
//		FacilitiesParser facilities_parser = new FacilitiesParser(Facilities.getSingleton());
//		facilities_parser.parse();
//		System.out.println("  done.");
//		System.out.println("  running facilities algorithms... ");
//		Facilities.getSingleton().addAlgorithm(new FacilitiesAggregation(network));
//		Facilities.getSingleton().runAlgorithms();
//		System.out.println("  done.");
//		System.out.println();

//		System.out.println("5. DEFINE CAPACITIES AND OPENTIMES FOR THE FACILITIES BASED ON THE POPULATION");
//		System.out.println();
//		System.out.println("  running facilities algorithms... ");
//		Facilities.getSingleton().clearAlgorithms();
//		Facilities.getSingleton().addAlgorithm(new FacilitiesDefineCapAndOpentime(plans.getPersons().size()));
//		Facilities.getSingleton().runAlgorithms();
//		System.out.println("  done.");
//		System.out.println();
//
//		System.out.println("6. DEFINE SOME KNOWLEDGE FOR THE POPULATION");
//		System.out.println();
//		System.out.println("  running plans algorithms... ");
//		plans.clearAlgorithms();
//		plans.addAlgorithm(new PlansDefineKnowledge());
//		plans.runAlgorithms();
//		System.out.println("  done.");
//		System.out.println();

//		System.out.println("7. CREATE AN INITIAL DAYPLAN FOR EACH PERSON ACCORDING TO THEIR KNOWLEDGE");
//		System.out.println();
//		System.out.println("  running plans algorithms... ");
		plans.clearAlgorithms();
//		plans.addAlgorithm(new PersonCreatePlanFromKnowledge());
		//plans.addAlgorithm(new FrancescoAlgo());
//		plans.addAlgorithm (new TicketAlgo ());
		//plans.addAlgorithm (new ModeChoiceAlgorithm ());
		//plans.addAlgorithm (new ModeAlgo ());
//		System.out.println("  done.");

		System.out.println();
		System.out.println("8. WRITING DOWN ALL DATA");
		System.out.println();

//		System.out.println("  writing facilities xml file... ");
//		FacilitiesWriter facilities_writer = new FacilitiesWriter(Facilities.getSingleton());
//		facilities_writer.write();
//		System.out.println("  done.");

//		System.out.println("  writing plans xml file... ");
//		PlansWriter plans_writer = new PlansWriter(plans);
//		plans_writer.write();
//		System.out.println("  done.");

//		System.out.println("  writing network xml file... ");
//		NetworkWriter network_writer = new NetworkWriter(network);
//		network_writer.write();
//		System.out.println("  done.");

//		System.out.println("  writing world xml file... ");
//		WorldWriter world_writer = new WorldWriter(Gbl.getWorld());
//		world_writer.write();
//		System.out.println("  done.");

//		System.out.println("  writing config xml file... ");
//		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
//		config_writer.write();
//		System.out.println("  done.");

		System.out.println("TEST SUCCEEDED.");
		System.out.println();

	}



	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////



	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Config config = Gbl.createConfig(args);

		testRun01(config);

		Gbl.printElapsedTime();
	}

}