/* *********************************************************************** *
 * project: org.matsim.*
 * License.java
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
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.world.World;

/**
 * checks, how many people have license for driving car and can choose from car
 * or pt
 *
 * @author ychen
 *
 */
public class License extends PersonAlgorithm {
	private int hasLicenseCount;

	/**
	 *
	 */
	public License() {
		this.hasLicenseCount = 0;
	}

	@Override
	public void run(Person person) {
		if (person != null) {
			if (person.getLicense().equals("yes")) {
				this.hasLicenseCount++;
			}
		}
	}

	/**
	 * @return the hasLicenseCount
	 */
	public int getHasLicenseCount() {
		return this.hasLicenseCount;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String netFilename = "../data/ivtch/input/network.xml";
		final String plansFilename = "../data/ivtch/input/_10pctZrhCarPtPlans_opt.xml.gz";

		Gbl.startMeasurement();
		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(null);
		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();

		License l = new License();
		population.addAlgorithm(l);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPlansReader(population).readFile(plansFilename);

		population.runAlgorithms();

		System.out.println("--> Done!\n-->There is " + l.getHasLicenseCount()
				+ " legal drivers!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
