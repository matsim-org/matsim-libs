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

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.utils.io.IOUtils;

/**
 * @author ychen
 * 
 */
public class SameActLocTest {
	public static class SameActLoc extends AbstractPersonAlgorithm {
		private BufferedWriter writer;
		private boolean actsAtSameLink;
		private int actLocCount = 0, personCount = 0, carActLocCount = 0,
				ptActLocCount = 0;

		public SameActLoc(final String filename) {
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
								+ "\namong them "
								+ carActLocCount
								+ " car-legs and "
								+ ptActLocCount
								+ " pt-legs;"
								+ "\npersons, who has such acts: "
								+ personCount);
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run(final Person person) {
			actsAtSameLink = false;
			String tmpLinkId = null;
			String nextTmpLinkId = null;
			int i = 0;
			if (person != null) {
				Plan p = person.getSelectedPlan();
				if (p != null) {
					// Plan.Type planType = p.getType();
					for (ActIterator ai = p.getIteratorAct(); ai.hasNext();) {
						nextTmpLinkId = ai.next().getLinkId().toString();
						if (tmpLinkId != null && nextTmpLinkId != null)
							if (tmpLinkId.equals(nextTmpLinkId)) {
								actLocCount++;
								if (
								// planType != null
								// && Plan.Type.UNDEFINED != planType
								!PlanModeJudger.useUndefined(p))
									if (
									// planType.equals(Plan.Type.CAR)
									PlanModeJudger.useCar(p))
										carActLocCount++;
									else if (
									// planType.equals(Plan.Type.PT)
									PlanModeJudger.usePt(p))
										ptActLocCount++;
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
						tmpLinkId = nextTmpLinkId;
						i++;
					}
					if (actsAtSameLink)
						personCount++;
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// final String netFilename = "./test/yu/ivtch/input/network.xml";
		final String netFilename = "../data/ivtch/input/network.xml";
		// final String netFilename = "./test/yu/equil_test/equil_net.xml";
		// final String plansFilename = "../runs/run266/100.plans.xml.gz";
		final String plansFilename = "../data/ivtch/carPt_opt_run266/ITERS/it.100/100.plans.xml.gz";
		// final String plansFilename =
		// "./test/yu/equil_test/output/100.plans.xml.gz";
		// final String outFilename = "./output/actLoc.txt.gz";
		final String outFilename = "../data/ivtch/carPt_opt_run266/actLoc.txt";

		Gbl.startMeasurement();
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();

		SameActLoc alt = new SameActLoc(outFilename);
		population.addAlgorithm(alt);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		population.runAlgorithms();

		alt.end();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
