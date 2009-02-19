/* *********************************************************************** *
 * project: org.matsim.*
 * TravVolCnterControler.java
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

package playground.yu.volCount;

/*
 * $Id: TravVolCnterControler.java,v 1.4 2007/11/23 13:04:04 ychen Exp $
 */

import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;

/**
 * test for TraVolCnter
 * 
 * @author ychen
 * 
 */
public class TravVolCnterControler {

	public static void main(final String[] args) {
		final String netFilename = "./equil/equil_net.xml";
		final String plansFilename = "./equil/equil_plans.xml";
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();
		PopulationReader plansReader = new MatsimPopulationReader(population,
				network);
		plansReader.readFile(plansFilename);

		Events events = new Events();

		TraVolCnter traVolCounter = new TraVolCnter();
		events.addHandler(traVolCounter);

		QueueSimulation sim = new QueueSimulation(network, population, events);
		sim.run();
	}

}
