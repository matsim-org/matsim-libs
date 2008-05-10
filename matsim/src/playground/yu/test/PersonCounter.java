/* *********************************************************************** *
 * project: org.matsim.*
 * PersonCounterTest.java
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
package playground.yu.test;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.world.World;

/**
 * @author ychen
 *
 */
public class PersonCounter extends PersonAlgorithm {
	private int cnt, nullCnt;

	/**
	 *
	 */
	public PersonCounter() {
		this.cnt = 0;
		this.nullCnt = 0;
	}

	@Override
	public void run(Person person) {
		if (person != null) {
			this.cnt++;
		} else {
			this.nullCnt++;
		}
	}

	@Override
	public String toString() {
		return "There are " + this.cnt + " persons and " + this.nullCnt
				+ " (null)persons";
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String netFilename = "./test/yu/test/input/network.xml";
		final String plansFilename = "./test/yu/test/input/10pctZrhCarPtPlans.xml.gz";

		World world = Gbl.getWorld();
		@SuppressWarnings("unused")
		Config config = Gbl
				.createConfig(new String[] { "./test/yu/test/configTest.xml" });

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		PersonCounter pc = new PersonCounter();
		population.addAlgorithm(pc);
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(plansFilename);
		population.runAlgorithms();
		System.out.println(pc.toString());
	}
}
