/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeModalSplitTest.java
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

import org.matsim.basic.v01.BasicPlan.ActIterator;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

/**
 * @author ychen
 * 
 */
public class SameActLocTest {
	public static class SameActLoc extends PersonAlgorithm {
		private BufferedWriter writer;
		private boolean actsAtSameLink;
		private int actLocCount = 0, personCount = 0;

		public SameActLoc(String filename) {
			try {
				writer = IOUtils.getBufferedWriter(filename);
				writer.write("personId\tlinkId\tactIdx\n");
				writer.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void end() {
			try {
				writer
						.write("------------------------------------\nacts at same link: "
								+ actLocCount
								+ "\npersons, who has such acts: "
								+ personCount);
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run(Person person) {
			actsAtSameLink = false;
			String tmpLinkId = null;
			String nextTmpLinkId = null;
			int i = 0;
			if (person != null) {
				Plan p = person.getSelectedPlan();
				// System.out.println("person id: " + person.getId());
				if (p != null) {
					for (ActIterator ai = p.getIteratorAct(); ai.hasNext();) {
						nextTmpLinkId = ai.next().getLink().getId().toString();
						if (tmpLinkId != null && nextTmpLinkId != null) {
							if (tmpLinkId.equals(nextTmpLinkId)) {
								actLocCount++;
								actsAtSameLink = true;
								try {
									writer.write(person.getId().toString()
											+ "\t" + tmpLinkId + "\t" + i
											+ "\n");
									writer.flush();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						tmpLinkId = nextTmpLinkId;
						// System.out.println(tmpLinkId);
						i++;
					}
					if (actsAtSameLink) {
						personCount++;
					}
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// final String netFilename = "./test/yu/ivtch/input/network.xml";
		final String netFilename = "../data/ivtch/input/network.xml";
		// final String netFilename = "./test/yu/equil_test/equil_net.xml";
		// final String plansFilename = "../runs/run263/100.plans.xml.gz";
		final String plansFilename = "../data/ivtch/carPtSimActTime_run264/ITERS/it.100/100.plans.xml.gz";
		// final String plansFilename =
		// "./test/yu/equil_test/output/100.plans.xml.gz";
		// final String outFilename = "./output/actLoc.txt.gz";
		final String outFilename = "../data/ivtch/analysis/run264actLoc.txt.gz";

		Gbl.startMeasurement();
		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(null);

		World world = Gbl.getWorld();

		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();

		SameActLoc alt = new SameActLoc(outFilename);
		population.addAlgorithm(alt);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPlansReader(population).readFile(plansFilename);
		world.setPopulation(population);

		population.runAlgorithms();

		alt.end();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
