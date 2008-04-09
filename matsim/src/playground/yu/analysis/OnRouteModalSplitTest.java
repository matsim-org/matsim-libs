/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler5.java
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

package playground.yu.analysis;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.world.World;

public class OnRouteModalSplitTest {

	@SuppressWarnings("unchecked")
	public static void main(final String[] args) {
		// final String netFilename = "./test/yu/test/input/equil_net.xml";
		// final String plansFilename =
		// "./test/yu/test/input/3k.100.plans.xml.gz";
		// final String eventsFilename =
		// "./test/yu/test/input/3k.100.events.txt.gz";
		// final String volumeTestFilename =
		// "./test/yu/test/output/3kVolumeTest.txt.gz";
		// final String chartFilename = "./test/yu/test/output/3kChart.png";
		// final String legFilename = "./test/yu/test/output/3kLeg.txt.gz";
		final String netFilename = "../data/ivtch/input/network.xml";
		final String plansFilename = "../data/ivtch/input/analysis/carActTime100.plans.xml.gz";
		final String eventsFilename = "../data/ivtch/input/analysis/carActTime100.events.txt.gz";
		final String chartFilename = "../data/ivtch/analysis/carActTimeChart.png";
		final String legFilename = "../data/ivtch/analysis/carActTimeLeg.txt.gz";

		Gbl.startMeasurement();
		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(null);

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		new MatsimPlansReader(population).readFile(plansFilename);
		world.setPopulation(population);

		Events events = new Events();

		OnRouteModalSplit mlh = new OnRouteModalSplit(300, network, population);
		events.addHandler(mlh);

		new MatsimEventsReader(events).readFile(eventsFilename);

		mlh.write(legFilename);
		mlh.writeCharts(chartFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
