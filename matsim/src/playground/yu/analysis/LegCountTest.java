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
public class LegCountTest {
	public static class LegCount extends AbstractPersonAlgorithm {
		private BufferedWriter writer;
		// private BasicLeg tmpLeg;
		// private boolean actsAtSameLink;
		private int carUserCount = 0, carLegCount = 0, ptUserCount = 0,
				ptLegCount = 0;

		public LegCount(final String filename) {
			try {
				writer = IOUtils.getBufferedWriter(filename);
				writer.write("personId\tLegNumber\n");
				writer.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void end() {
			try {
				writer.write("------------------------------------\ncarUser:\t"
						+ carUserCount + ";\tcarLegs:\t" + carLegCount + ";\t"
						+ (double) carLegCount / (double) carUserCount
						+ "\tLegs pro carUser." + "\nptUser:\t" + ptUserCount
						+ ";\tptLegs:\t" + ptLegCount + ";\t"
						+ (double) ptLegCount / (double) ptUserCount
						+ "\tLegs pro ptUser.");
				writer.flush();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void carAppend(final int legsNumber) {
			carUserCount++;
			carLegCount += legsNumber;
		}

		@Override
		public void run(final Person person) {
			Plan p = person.getSelectedPlan();
			if (p != null) {
				int nLegs = (p.getActsLegs().size() + 1) / 2;
				try {
					writer.write(person.getId() + "\t" + nLegs + "\n");
					writer.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
				// Plan.Type planType = p.getType();
				if (
				// planType != null && Plan.Type.UNDEFINED != planType
				!PlanModeJudger.useUndefined(p)) {
					if (// planType.equals(Plan.Type.CAR)
					PlanModeJudger.useCar(p))
						carAppend(nLegs);
					else if (// planType.equals(Plan.Type.PT)
					PlanModeJudger.usePt(p)) {
						ptUserCount++;
						ptLegCount += nLegs;
					}
				} else
					carAppend(nLegs);
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
		// final String plansFilename = "../runs/run264/100.plans.xml.gz";
		final String plansFilename = "../data/ivtch/legCount/263.100.plans.xml.gz";
		// final String plansFilename =
		// "./test/yu/equil_test/output/100.plans.xml.gz";
		// final String outFilename = "./output/legsCount.txt.gz";
		final String outFilename = "../data/ivtch/legCount/263.legsCount.txt";

		Gbl.startMeasurement();
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();

		LegCount lc = new LegCount(outFilename);
		population.addAlgorithm(lc);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		population.runAlgorithms();

		lc.end();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
