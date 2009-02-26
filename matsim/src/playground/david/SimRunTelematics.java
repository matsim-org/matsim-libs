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

import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationReader;
import org.matsim.world.World;

public class SimRunTelematics {

	public static void main(String[] args) {
		String netFileName = "test/simple/equil_net.xml";
		String popFileName = "test/simple/equil_plans.xml";

		Gbl.startMeasurement();
		Gbl.createConfig(args);

		World world = Gbl.createWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);
		world.complete();

		Population population = new MyPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(popFileName);

		Events events = new Events();
		events.addHandler(new EventWriterTXT("EventsTelematicsSimWrapper.txt"));

		//TelematicsSimWrapper sim = new TelematicsSimWrapper(netFileName,population, events);
		//sim.setStartEndTime(0,30000);
		//sim.run();
		// oder
		//sim.run(0*60*60, 10*60*60);

	}

}
