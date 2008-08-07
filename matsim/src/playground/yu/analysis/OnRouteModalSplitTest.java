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

import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.world.World;

public class OnRouteModalSplitTest {

	/**
	 * @param args0
	 *            netFilename
	 * @param args1
	 *            plansFilename
	 * @param args2
	 *            eventsFilename
	 * @param args3
	 *            chartFilename
	 * @param args4
	 *            legFilename
	 */
	@SuppressWarnings("unchecked")
	public static void main(final String[] args) {
		final String netFilename = args[0];
		final String plansFilename = args[1];
		final String eventsFilename = args[2];
		final String chartFilename = args[3];
		final String legFilename = args[4];

		Gbl.startMeasurement();
		Gbl.createConfig(null);

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Population population = new Population();
		new MatsimPopulationReader(population).readFile(plansFilename);

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
