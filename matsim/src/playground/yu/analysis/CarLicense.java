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
 * 
 */
package playground.yu.analysis;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.world.World;

/**
 * @author ychen
 * 
 */
public class CarLicense extends PersonAlgorithm {
	private int count = 0;
	private String planType = null;

	/**
	 * 
	 */
	public CarLicense() {
	}

	@Override
	public void run(Person person) {
		if (person != null) {
			planType = person.getSelectedPlan().getType();
			if (!((planType != null) && (planType != "car"))) {
				if ((person.getAge() < 18) || person.getLicense().equals("no")) {
					count++;
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
		// final String plansFilename = "../runs/run264/100.plans.xml.gz";
		final String plansFilename = "../data/ivtch/carPt_opt_run266/ITERS/it.100/100.plans.xml.gz";
		// final String plansFilename =
		// "./test/yu/equil_test/output/100.plans.xml.gz";
		// final String outFilename = "./output/legsCount.txt.gz";
		// final String outFilename =
		// "../data/ivtch/carPt_opt_run266/legsCount.txt";

		Gbl.startMeasurement();
		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(null);
		//
		World world = Gbl.getWorld();
		//
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();

		CarLicense cl = new CarLicense();
		population.addAlgorithm(cl);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPlansReader(population).readFile(plansFilename);
		world.setPopulation(population);

		population.runAlgorithms();

		// cl.end();

		System.out.println("--> Done!\n-->There is " + cl.getCount()
				+ " illeagel drivers!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

}
