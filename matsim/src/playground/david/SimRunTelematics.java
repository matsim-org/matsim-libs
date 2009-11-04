/* *********************************************************************** *
 * project: org.matsim.*
 * SimRunTelematics.java
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

package playground.david;

import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;

public class SimRunTelematics {

	public static void main(String[] args) {
		String netFileName = "test/simple/equil_net.xml";
		String popFileName = "test/simple/equil_plans.xml";

		Gbl.createConfig(args);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);

		PopulationImpl population = new MyPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(population, network);
		plansReader.readFile(popFileName);

		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(new EventWriterTXT("EventsTelematicsSimWrapper.txt"));

		//TelematicsSimWrapper sim = new TelematicsSimWrapper(netFileName,population, events);
		//sim.setStartEndTime(0,30000);
		//sim.run();
		// oder
		//sim.run(0*60*60, 10*60*60);

	}

}
