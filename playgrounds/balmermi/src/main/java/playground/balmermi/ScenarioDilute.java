/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioIO.java
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.filters.PersonIntersectAreaFilter;

import playground.balmermi.modules.PersonStupidDeleteKnowledgeForStreamingModule;

public class ScenarioDilute {

	//////////////////////////////////////////////////////////////////////
	// printUsage
	//////////////////////////////////////////////////////////////////////

	private static void printUsage() {
		System.out.println();
		System.out.println("ScenarioDilute");
		System.out.println();
		System.out.println("Usage1: ScenarioDilute configfile");
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
		Gbl.printMemoryUsage();
		System.out.println("done. (loading facilities)");

		System.out.println("loading network...");
		sl.loadNetwork();
		Gbl.printMemoryUsage();
		System.out.println("done. (loading network)");

		Config config = sl.getScenario().getConfig();
		NetworkLayer network = sl.getScenario().getNetwork();

		System.out.println("complete world...");
		Set<String> exTxpes = new TreeSet<String>();
		exTxpes.add("0-4110-0"); // motorway
		exTxpes.add("1-4110-0"); // motorway
		exTxpes.add("2-4130-1"); // ferry
		exTxpes.add("2-4130-2"); // train
		exTxpes.add("3-4130-2"); // train
		exTxpes.add("4-4130-1"); // ferry
		exTxpes.add("4-4130-2"); // train
		exTxpes.add("7-4130-1"); // ferry
		sc.getWorld().complete(exTxpes);
		Gbl.printMemoryUsage();
		System.out.println("done. (complete world)");

		System.out.println("calculate area of interest and extract its links...");

		// dilZrh
//		double radius = 30000.0;
//		final CoordImpl center = new CoordImpl(683518.0,246836.0);
		// dilTburg
		double radius = 30000.0;
		final CoordImpl center = new CoordImpl(733400.0,243600.0);

		final Map<Id, Link> areaOfInterest = new HashMap<Id, Link>();
		System.out.println("=> area of interest (aoi): center=" + center + "; radius=" + radius);
		for (LinkImpl link : network.getLinks().values()) {
			final Node from = link.getFromNode();
			final Node to = link.getToNode();
			if ((CoordUtils.calcDistance(from.getCoord(), center) <= radius) || (CoordUtils.calcDistance(to.getCoord(), center) <= radius)) {
				areaOfInterest.put(link.getId(),link);
			}
		}
		System.out.println("=> aoi contains: " + areaOfInterest.size() + " links.");
		System.out.println(" done. " + (new Date()));

		final PopulationImpl population = (PopulationImpl) sl.getScenario().getPopulation();
		population.setIsStreaming(true);
		PopulationReader plansReader = new MatsimPopulationReader(sl.getScenario());
		PopulationWriter plansWriter = new PopulationWriter(population,network, sl.getScenario().getKnowledges());
		plansWriter.startStreaming(sl.getScenario().getConfig().plans().getOutputFile());

		System.out.println("adding algorithms...");
		PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(plansWriter,areaOfInterest, network);
		filter.setAlternativeAOI(center,radius);
		population.addAlgorithm(filter);
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
