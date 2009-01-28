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

package playground.yu.newPlans;

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.world.World;

/**
 * test of NewAgentPtPlan
 * 
 * @author ychen
 * 
 */
public class SelectedPlansControler {

	public static void main(final String[] args) {
		final String netFilename = "../data/schweiz/input/ch.xml";
		final String plansFilename = "../data/schweiz/input/459.100.plans.xml.gz";

		World world = Gbl.getWorld();
		Gbl.createConfig(new String[] { "../data/schweiz/selectedPlans.xml" });

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);
		world.complete();

		Population population = new Population();
		SelectedPlans sp = new SelectedPlans(population);
		population.addAlgorithm(sp);
		PopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(plansFilename);
		population.runAlgorithms();
		sp.writeEndPlans();
	}
}
