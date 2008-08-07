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

/*
 * $Id: NewPlansControler.java,v 1.7 2007/11/23 13:04:04 ychen Exp $
 */
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Plans;
import org.matsim.population.PlansReaderI;

/**
 * test of NewAgentPtPlan
 * 
 * @author ychen
 * 
 */
public class NewPlansControler {

	public static void main(final String[] args) {
		final String netFilename = "./test/yu/newPlans/equil_net.xml";
		final String plansFilename = "./test/yu/newPlans/equil_plans1k.xml";

		Gbl.createConfig(new String[] { "./test/yu/newPlans/config.xml" });

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		Gbl.getWorld().setNetworkLayer(network);

		Plans population = new Plans();
		NewAgentPtPlan nap = new NewAgentPtPlan(population);
		population.addAlgorithm(nap);
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(plansFilename);
		population.runAlgorithms();
		nap.writeEndPlans();
	}
}
