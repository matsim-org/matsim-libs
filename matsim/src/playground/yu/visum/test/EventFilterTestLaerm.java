/* *********************************************************************** *
 * project: org.matsim.*
 * EventFilterTestLaerm.java
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

package playground.yu.visum.test;

import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.yu.visum.filter.EventFilterAlgorithm;
import playground.yu.visum.filter.finalFilters.TraVolCal;
import playground.yu.visum.writer.PrintStreamLinkATT;
import playground.yu.visum.writer.PrintStreamUDANET;

/**
 * @author yu chen
 */
public class EventFilterTestLaerm {

	public static void testRunTraVolCal(Config config) throws IOException {
		// network
		System.out.println("  creating network object... ");
		NetworkLayer network = new NetworkLayer();
		System.out.println("  done.");

		System.out.println("  reading network file... ");
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());
		System.out.println("  done.");
		// plans
		System.out.println("  creating plans object... ");
		PopulationImpl plans = new PopulationImpl();
		plans.setIsStreaming(true);
		System.out.println("  done.");
		// events
		System.out.println("  creating events object... ");
		EventsManagerImpl events = new EventsManagerImpl();
		System.out.println("  done.");

		System.out.println("  adding events algorithms...");
		TraVolCal tvc = new TraVolCal(plans, network);
		EventFilterAlgorithm efa = new EventFilterAlgorithm();
		efa.setNextFilter(tvc);
		events.addHandler(efa);
		System.out.println("  done");

		// read file, run algos
		System.out.println("  reading events file and running events algos");
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		System.out.println("  done.");

		System.out.println("\tprinting additiv netFile of Visum...");
		PrintStreamUDANET psUdaNet = new PrintStreamUDANET(config.getParam(
				"attribut_Laerm", "outputAttNetFile"));
		psUdaNet.output(tvc);
		psUdaNet.close();
		System.out.println("\tdone.");

		System.out.println("\tprinting attributsFile of link...");
		PrintStreamLinkATT psLinkAtt = new PrintStreamLinkATT(config.getParam(
				"attribut_Laerm", "outputAttFile"), network);
		psLinkAtt.output(tvc);
		psLinkAtt.close();
		System.out.println("\tdone.");

		// tvc.getTraVol(6, 2864);
		// tvc.getTraVol(7, 2864);
		// tvc.getTraVol(8, 2864);
		// tvc.getTraVol(9, 2864);
		System.out.println("  done.");
	}

	/**
	 * @param args
	 *            test/yu/config_hms.xml config_v1.dtd
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();
		Config config = new ScenarioLoaderImpl(args[0]).loadScenario().getConfig();
		testRunTraVolCal(config);
		Gbl.printElapsedTime();
	}
}
