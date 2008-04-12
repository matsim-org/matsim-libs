/* *********************************************************************** *
 * project: org.matsim.*
 * MergePlans.java
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
package playground.yu.newPlan;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.world.World;

/**
 * @author ychen
 * 
 */
public class MergePlans {
	public static class CopyPlans extends PersonAlgorithm {
		private PlansWriter writer;

		public CopyPlans(PlansWriter writer) {
			this.writer = writer;
		}

		@Override
		public void run(Person person) {
			writer.writePerson(person);
		}
	}

	private static final class PersonIdCopyPlans extends CopyPlans {
		private int lower_limit;

		public PersonIdCopyPlans(PlansWriter writer, int lower_limit) {
			super(writer);
			this.lower_limit = lower_limit;
		}

		public void run(Person person) {
			if (Integer.parseInt(person.getId().toString()) > lower_limit) {
				super.run(person);
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// final String path = "../data/ivtch/input/";
		// final String netFilename = path + "ivtch-osm.xml";
		// final String plansFilenameA = path +
		// "plans_all_zrh30km_10pct.xml.gz";
		// final String plansFilenameB = path
		// + "plans_miv_zrh30km_transitincl_10pct.xml.gz";
		// final String outputPlansFilename = path
		// + "plans_all_zrh30km_transitincl_10pct.xml.gz";

		final String path = "test/yu/equil_test/";
		final String netFilename = path + "equil_net.xml";
		final String plansFilenameA = path + "plans100pt.xml";
		final String plansFilenameB = path + "plans300.xml";
		final String outputPlansFilename = path + "sum_plans_100pt_201-300.xml";
		final int lower_limit = 1000000000;

		World world = Gbl.getWorld();
		Config config = Gbl.createConfig(null);
		Gbl.getConfig().plans().setOutputFile(outputPlansFilename);
		Gbl.getConfig().plans().switchOffPlansStreaming(false);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans plansA = new Plans();
		PlansWriter pw = new PlansWriter(plansA);
		pw.writeStartPlans();
		plansA.addAlgorithm(new CopyPlans(pw));
		new MatsimPlansReader(plansA).readFile(plansFilenameA);
		world.setPopulation(plansA);
		plansA.runAlgorithms();

		Plans plansB = new Plans();
		plansB.addAlgorithm(new PersonIdCopyPlans(pw, lower_limit));
		new MatsimPlansReader(plansB).readFile(plansFilenameB);
		world.setPopulation(plansB);
		plansB.runAlgorithms();
		pw.writeEndPlans();
	}
}
