/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyQueueSimSWISS.java
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

package playground.david.vis;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.Simulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.Plans;
import org.matsim.world.World;

/**
 * @author DS
 *
 */
public class OnTheFlyQueueSimSWISS {
	
	public static void main(String[] args) {		
		OnTheFlyQueueSim sim;
		QueueNetworkLayer net;
		Plans population;
		Events events;
		
		String netFileName = "../../tmp/network.xml.gz";
				
		Gbl.createConfig(args);
		Gbl.startMeasurement();
		Config config = Gbl.getConfig();
		config.setParam("global", "localDTDBase", "dtd/");
		
		World world = Gbl.getWorld();

		net = new QueueNetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);
		
		population = new Plans();
		world.setPopulation(population);

		events = new Events() ;
		world.setEvents(events);
		
		config.setParam(Simulation.SIMULATION, Simulation.STARTTIME, "00:00:00");
		config.setParam(Simulation.SIMULATION, Simulation.ENDTIME, "00:00:01");

		sim = new OnTheFlyQueueSim(net, population, events);
		sim.setOtfwriter(new OTFNetFileHandler(10,net,"OTFNetfileSCHWEIZ.vis"));
		

		sim.run();

		Gbl.printElapsedTime();		

	}
	
		
}
