/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioStreaming.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.balmermi;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.balmermi.modules.FacilitiesRandomizeHectareCoordinates;
import playground.balmermi.modules.PersonResetCoordAndLink;
import playground.balmermi.modules.PersonStupidDeleteKnowledgeForStreamingModule;

public class ScenarioStreaming {

	//////////////////////////////////////////////////////////////////////
	// printUsage
	//////////////////////////////////////////////////////////////////////

	private static void printUsage() {
		System.out.println();
		System.out.println("ScenarioStreaming");
		System.out.println();
		System.out.println("Usage1: ScenarioStreaming configfile");
		System.out.println("        add a MATSim config file as the only input parameter.");
		System.out.println();
		System.out.println("Note: config file should at least contain the following parameters:");
		System.out.println("      inputNetworkFile");
		System.out.println("      outputNetworkFile");
		System.out.println("      inputFacilitiesFile");
		System.out.println("      outputFacilitiesFile");
		System.out.println("      inputPlansFile");
		System.out.println("      outputPlansFile");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2009, matsim.org");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		if (args.length != 1) { printUsage(); return; }

		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(args[0]);
		ScenarioImpl sc = sl.getScenario();

		System.out.println("loading facilities...");
		sl.loadActivityFacilities();
		new FacilitiesRandomizeHectareCoordinates().run(sl.getScenario().getActivityFacilities());
		Gbl.printMemoryUsage();
		System.out.println("done. (loading facilities)");

		System.out.println("loading network...");
		sl.loadNetwork();
		Gbl.printMemoryUsage();
		System.out.println("done. (loading network)");

		Config config = sl.getScenario().getConfig();
		NetworkImpl network = sl.getScenario().getNetwork();
		ActivityFacilitiesImpl af = sl.getScenario().getActivityFacilities();

		System.out.println("complete world...");
		Set<String> exTxpes = new TreeSet<String>();
		// teleatlas new
		exTxpes.add("0");  // motorway
		exTxpes.add("-1"); // not defined
		exTxpes.add("-2"); // ferry
		exTxpes.add("-3");
		exTxpes.add("-4");
		exTxpes.add("-5");
		exTxpes.add("-6");
		exTxpes.add("-7");
		exTxpes.add("-8");
		exTxpes.add("-9");

		// teleatlas
//		exTxpes.add("0-4110-0"); // motorway
//		exTxpes.add("1-4110-0"); // motorway
//		exTxpes.add("2-4130-1"); // ferry
//		exTxpes.add("2-4130-2"); // train
//		exTxpes.add("3-4130-2"); // train
//		exTxpes.add("4-4130-1"); // ferry
//		exTxpes.add("4-4130-2"); // train
//		exTxpes.add("7-4130-1"); // ferry

		// ivtch
//		exTxpes.add("0"); exTxpes.add("1"); exTxpes.add("2"); exTxpes.add("3");
//		exTxpes.add("4"); exTxpes.add("5"); exTxpes.add("6"); exTxpes.add("7");
//		exTxpes.add("8"); exTxpes.add("9");
//		exTxpes.add("10"); exTxpes.add("11"); exTxpes.add("12"); exTxpes.add("13");
//		exTxpes.add("14"); exTxpes.add("15"); exTxpes.add("16"); exTxpes.add("17");
//		exTxpes.add("18"); exTxpes.add("19");
//		exTxpes.add("20"); exTxpes.add("21"); exTxpes.add("22"); exTxpes.add("23");
//		exTxpes.add("24"); exTxpes.add("25"); exTxpes.add("26"); exTxpes.add("27");
//		exTxpes.add("28"); exTxpes.add("29");
//		exTxpes.add("90"); exTxpes.add("91"); exTxpes.add("92"); exTxpes.add("93");
//		exTxpes.add("94"); exTxpes.add("95"); exTxpes.add("96"); exTxpes.add("97");
//		exTxpes.add("98"); exTxpes.add("99");
//		sc.getWorld().complete(exTxpes, sc.getConfig());
		Gbl.printMemoryUsage();
		System.out.println("done. (complete world)");

//		System.out.println("writing facilities...");
//		new FacilitiesWriter(af).write(config.facilities().getOutputFile());
//		System.out.println("done. (writing facilities)");

		final PopulationImpl population = (PopulationImpl) sl.getScenario().getPopulation();
		population.setIsStreaming(true);
		PopulationReader plansReader = new MatsimPopulationReader(sl.getScenario());
		PopulationWriter plansWriter = new PopulationWriter(population,network, sl.getScenario().getKnowledges());
		plansWriter.startStreaming(null);//sl.getScenario().getConfig().plans().getOutputFile());

		System.out.println("adding algorithms...");
		population.addAlgorithm(new PersonResetCoordAndLink(af));
		population.addAlgorithm(plansWriter);
		population.addAlgorithm(new PersonStupidDeleteKnowledgeForStreamingModule(sl.getScenario().getKnowledges()));
		Gbl.printMemoryUsage();
		System.out.println("done. (adding algorithms)");

		System.out.println("stream population...");
		plansReader.readFile(config.plans().getInputFile());
		population.printPlansCount();
		plansWriter.closeStreaming();
		Gbl.printMemoryUsage();
		System.out.println("done. (stream population)");
	}
}
