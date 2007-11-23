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

package playground.yu;
/*
 * $Id: NewPlansControler.java,v 1.7 2007/11/23 13:04:04 ychen Exp $
 */

/* *********************************************************************** *
 *                                                                         *
 *                            NewPlansControler.java                            *
 *                          ---------------------                          *
 * copyright       : (C) 2007 by Michael Balmer, Marcel Rieser,            *
 *                   David Strippgen, Gunnar Flötteröd, Konrad Meister,    *
 *                   Kai Nagel, Kay W. Axhausen                            *
 *                   Technische Universitaet Berlin (TU-Berlin) and        *
 *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
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

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.world.World;


/**
 * test of NewAgentPtPlan
 * @author ychen
 *
 */
public class NewPlansControler {

	public static void main(final String[] args) {
		final String netFilename = "./input/equil/equil_net.xml";
		final String plansFilename = "./input/equil/equil_plans.xml";

		World world = Gbl.getWorld();
		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(new String[] {"./test/yu/multipleIterations.xml"});

		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		NewAgentPtPlan nap=new NewAgentPtPlan(population);
		population.addAlgorithm(nap);
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(plansFilename);
		world.setPopulation(population);
		population.runAlgorithms();
		nap.writeEndPlans();
	}
}
