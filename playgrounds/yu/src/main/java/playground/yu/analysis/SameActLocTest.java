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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

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
				this.writer = IOUtils.getBufferedWriter(filename);
				this.writer.write("personId\tlinkId\tactIdx\n");
				this.writer.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void end() {
			try {
				this.writer
						.write("------------------------------------\nacts at same link: "
								+ this.actLocCount
								+ "\namong them "
								+ this.carActLocCount
								+ " car-legs and "
								+ this.ptActLocCount
								+ " pt-legs;"
								+ "\npersons, who has such acts: "
								+ this.personCount);
				this.writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run(final Person person) {
			this.actsAtSameLink = false;
			String tmpLinkId = null;
			String nextTmpLinkId = null;
			int i = 0;
			if (person != null) {
				Plan p = person.getSelectedPlan();
				if (p != null) {
					// Plan.Type planType = p.getType();
					for (PlanElement pe : p.getPlanElements()) {
						if (pe instanceof ActivityImpl) {
							ActivityImpl act = (ActivityImpl) pe;
							nextTmpLinkId = act.getLinkId().toString();
							if (tmpLinkId != null && nextTmpLinkId != null)
								if (tmpLinkId.equals(nextTmpLinkId)) {
									this.actLocCount++;
									if (
									// planType != null
									// && Plan.Type.UNDEFINED != planType
									!PlanModeJudger.useUndefined(p))
										if (
										// planType.equals(Plan.Type.CAR)
										PlanModeJudger.useCar(p))
											this.carActLocCount++;
										else if (
										// planType.equals(Plan.Type.PT)
										PlanModeJudger.usePt(p))
											this.ptActLocCount++;
									this.actsAtSameLink = true;
									try {
										this.writer.write(person.getId().toString()
												+ "\t" + tmpLinkId + "\t" + i
												+ "\n");
										this.writer.flush();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							tmpLinkId = nextTmpLinkId;
							i++;
						}
					}
					if (this.actsAtSameLink)
						this.personCount++;
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
		ScenarioImpl scenario = new ScenarioImpl();

		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = scenario.getPopulation();

		SameActLoc alt = new SameActLoc(outFilename);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		alt.run(population);

		alt.end();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
