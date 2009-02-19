/* *********************************************************************** *
 * project: org.matsim.*
 * LegDistanceTest.java
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

/**
 *
 */
package playground.yu.analysis;

import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;

/**
 * @author ychen
 * 
 */
public class LegDistanceTest {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch/network/ivtch.xml";
		final String eventsFilename = "../runs/run265/100.events.txt.gz";
		final String chartFilename = "./output/run265legDistance";
		final String outFilename = "./output/run265legDistance.txt.gz";

		// final String netFilename = "./test/yu/test/input/equil_net.xml";
		// final String eventsFilename =
		// "./test/yu/test/input/3k.100.events.txt.gz";
		// final String chartFilename = "./test/yu/test/output/3kChart.png";
		// final String outFilename = "./test/yu/test/output/3klegDist.txt.gz";

		Gbl.startMeasurement();
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		// Plans population = new Plans();
		// System.out.println("-->reading plansfile: " + plansFilename);
		// new MatsimPopulationReader(population).readFile(plansFilename);
		// world.setPopulation(population);

		Events events = new Events();

		LegDistance legDist = new LegDistance(300, network);
		events.addHandler(legDist);

		System.out.println("-->reading evetsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		legDist.write(outFilename);
		legDist.writeCharts(chartFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
