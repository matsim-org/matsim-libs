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

package playground.yu.test;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

public class VolumefromEventsTest {

	@SuppressWarnings("unchecked")
	public static void main(final String[] args) {
		final String netFilename = "./test/yu/test/input/network.xml";
		// final String plansFilename = "./examples/equil/plans100.xml";
		final String eventsFilename = "./test/yu/test/input/20.events.txt.gz";
		@SuppressWarnings("unused")
		Config config = Gbl
				.createConfig(new String[] { "./test/yu/test/configTest.xml" });

		World world = Gbl.getWorld();

		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		// Map<IdI, QueueLink> links = (Map<IdI, QueueLink>) network.getLinks();
		/*
		 * try { // BufferedWriter out = IOUtils //
		 * .getBufferedWriter("./test/yu/test/output/capTest.txt.gz"); //
		 * out.write("linkId\tCapacity\tSimulationFlowCapacity\n"); //
		 * out.flush(); // for (QueueLink ql : links.values()) { //
		 * out.write(ql.getId().toString() + "\t" + ql.getCapacity() // + "\t" +
		 * ql.getSimulatedFlowCapacity() + "\n"); // out.flush(); // } //
		 * out.close(); // } catch (FileNotFoundException e) { //
		 * e.printStackTrace(); // } catch (IOException e) { //
		 * e.printStackTrace(); // } // Plans population = new Plans(); // new
		 * MatsimPlansReader(population).readFile(plansFilename); //
		 * world.setPopulation(population);
		 */

		Events events = new Events();
		VolumesAnalyzer volumes = new VolumesAnalyzer(3600, 24 * 3600 - 1,
				network);
		events.addHandler(volumes);
		new MatsimEventsReader(events).readFile(eventsFilename);
		Map<IdI, QueueLink> links = (Map<IdI, QueueLink>) network.getLinks();
		try {
			BufferedWriter out = IOUtils
					.getBufferedWriter("./test/yu/test/output/20.volumeTest.txt.gz");
			out
					.write("linkId\tCapacity\tSimulationFlowCapacity\tH6-7\tH7-8\tH8-9\n");
			out.flush();
			for (QueueLink ql : links.values()) {
				int[] v = volumes.getVolumesForLink(ql.getId().toString());
				out.write(ql.getId().toString()
						+ "\t"
						+ ql.getCapacity()
						+ "\t"
						+ ql.getSimulatedFlowCapacity()
						+ "\t"
						+ ((v != null) ? (v[6] + "\t" + v[7] + "\t" + v[8])
								: ("-1\t-1\t-1")) + "\n");
				out.flush();
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("-> Done!");
		System.exit(0);
	}
}
