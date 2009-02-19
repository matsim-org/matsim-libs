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

package playground.yu.newPlans;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;

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
		Config config = Gbl
				.createConfig(new String[] { "./test/yu/newPlans/newPlans.xml" });

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());

		Population population = new Population();
		DoublePlan dp = new DoublePlan(population);
		population.addAlgorithm(dp);
		PopulationReader plansReader = new MatsimPopulationReader(population,
				network);
		plansReader.readFile(config.plans().getInputFile());
		population.runAlgorithms();
		dp.writeEndPlans();
	}

}
