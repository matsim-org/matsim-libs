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
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Population;

/**
 * test of NewAgentPtPlan
 * 
 * @author ychen
 * 
 */
public class NewPtPlansControler {

	public static void main(final String[] args) {
		Config config = Gbl.createConfig(args);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());
		Gbl.getWorld().setNetworkLayer(network);

		Population population = new Population();
		NewAgentPtPlan nap = new NewAgentPtPlan(population);
		population.addAlgorithm(nap);
		new MatsimPlansReader(population).readFile(config.plans()
				.getInputFile());
		population.runAlgorithms();
		nap.writeEndPlans();
	}
}
