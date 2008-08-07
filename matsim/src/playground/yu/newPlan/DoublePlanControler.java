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
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.world.World;

/**
 * test of DoublePtPlan
 * 
 * @author ychen
 * 
 */
public class DoublePlanControler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		World world = Gbl.getWorld();
		Config config = Gbl
				.createConfig(new String[] { "./test/yu/newPlans/newPlans.xml" });

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());
		world.setNetworkLayer(network);

		Population population = new Population();
		DoublePlan dp = new DoublePlan(population);
		population.addAlgorithm(dp);
		PopulationReader plansReader = new MatsimPlansReader(population);
		plansReader.readFile(config.plans().getInputFile());
		population.runAlgorithms();
		dp.writeEndPlans();
	}

}
