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
public class NewSmallPlansControler {

	public static void main(final String[] args) {
		// final String netFilename = "./test/yu/ivtch/input/network.xml";
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../schweiz-ivtch-SVN/baseCase/plans/plans_all_zrh30km_transitincl_10pct.xml.gz";

		World world = Gbl.getWorld();
		Gbl.createConfig(new String[] {
		// "./test/yu/ivtch/config_for_10pctZuerich_car_pt_smallPlansl.xml"
//				"../data/ivtch/make10pctPlans.xml"
				"input/make10pctPlans.xml"});

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);
		world.complete();

		Population population = new Population();
		NewSmallPlan nsp = new NewSmallPlan(population);
		population.addAlgorithm(nsp);
		PopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(plansFilename);
		population.runAlgorithms();
		nsp.writeEndPlans();
	}
}
