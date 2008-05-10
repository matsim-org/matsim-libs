/* *********************************************************************** *
 * project: org.matsim.*
 * NewPlansControler.java
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
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.world.World;

/**
 * test of NewAgentPtPlan
 *
 * @author ychen
 *
 */
public class NewCarPlansControler {

	public static void main(final String[] args) {
		final String netFilename = "./test/yu/newPlans/input/network.xml";
		final String plansFilename = "./test/yu/newPlans/input/10pctZrhPlans.xml.gz";

		World world = Gbl.getWorld();
		Config config = Gbl
				.createConfig(new String[] { "./test/yu/newPlans/configMake10pctZrhCarPlans.xml" });

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		NewAgentCarPlan nac = new NewAgentCarPlan(population);
		population.addAlgorithm(nac);
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(plansFilename);
		population.runAlgorithms();
		nac.writeEndPlans();
	}
}
