/* *********************************************************************** *
 * project: org.matsim.*
 * DoublePtPlanControler.java
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

package playground.yu.newPlan;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.world.World;

/**
 * test of DoublePtPlan
 * @author ychen
 *
 */
public class DoublePlanControler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String netFilename = "./test/yu/newPlans/equil_net.xml";
		final String plansFilename = "./test/yu/newPlans/equil_plans1k_w_pt.xml";

		World world = Gbl.getWorld();
		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(new String[] {"./test/yu/newPlans/config.xml"});

		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		DoublePlan nap=new DoublePlan(population);
		population.addAlgorithm(nap);
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(plansFilename);
		world.setPopulation(population);
		population.runAlgorithms();
		nap.writeEndPlans();
	}

}
