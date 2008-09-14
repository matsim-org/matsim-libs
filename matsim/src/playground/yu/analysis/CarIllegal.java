/* *********************************************************************** *
 * project: org.matsim.*
 * CarLicense.java
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
 * checks how many people drive car illegally (under 18 years old or without license)
 */
package playground.yu.analysis;

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.world.World;

/**
 * @author ychen
 * 
 */
public class CarIllegal extends AbstractPersonAlgorithm {
	private int count = 0;

	// private Plan.Type planType = null;

	/**
	 */
	public CarIllegal() {
	}

	@Override
	public void run(final Person person) {
		if (person != null) {
			Plan selectedPlan = person.getSelectedPlan();
			if (
			// planType != null && planType != Plan.Type.CAR
			PlanModeJudger.useCar(selectedPlan))
				if (person.getAge() < 18 || person.getLicense().equals("no"))
					count++;
		}
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// final String netFilename = "./test/yu/ivtch/input/network.xml";
		final String netFilename = "../data/ivtch/input/network.xml";
		// final String netFilename = "./test/yu/equil_test/equil_net.xml";
		// final String plansFilename = "../runs/run264/100.plans.xml.gz";
		final String plansFilename = "../data/ivtch/carPt_opt_run266/ITERS/it.100/100.plans.xml.gz";
		// final String plansFilename =
		// "./test/yu/equil_test/output/100.plans.xml.gz";
		// final String outFilename = "./output/legsCount.txt.gz";
		// final String outFilename =
		// "../data/ivtch/carPt_opt_run266/legsCount.txt";

		Gbl.startMeasurement();

		Gbl.createConfig(null);
		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Population population = new Population();

		CarIllegal cl = new CarIllegal();
		population.addAlgorithm(cl);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population).readFile(plansFilename);

		population.runAlgorithms();

		System.out.println("--> Done!\n-->There is " + cl.getCount()
				+ " illeagel drivers!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
