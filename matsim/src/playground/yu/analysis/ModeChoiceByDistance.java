/* *********************************************************************** *
 * project: org.matsim.*
 * CarTripCounter.java
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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class ModeChoiceByDistance extends AbstractPersonAlgorithm {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs/run628/it.500/500.plans.xml.gz";
		// final String eventsFilename =
		// "../runs/run628/it.500/500.events.txt.gz";
		final String outputFilename = "../runs/run628/it.500/500.carDeparture.txt";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		Gbl.getWorld().setNetworkLayer(network);
		Population ppl = new Population();

		System.out.println("->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(ppl).readFile(plansFilename);

		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outputFilename);
			out.write("network :\t" + netFilename + "\n");
			out.write("plansfile :\t" + plansFilename + "\n");
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("-> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

	@Override
	public void run(Person person) {
		for (LegIterator li = person.getSelectedPlan().getIteratorLeg(); li
				.hasNext();) {
			Leg l = (Leg) li.next();
			li.next().getMode();
			li.next().getRoute().getDist();
		}
	}

}
