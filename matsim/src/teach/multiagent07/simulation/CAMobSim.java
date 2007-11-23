/* *********************************************************************** *
 * project: org.matsim.*
 * CAMobSim.java
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

package teach.multiagent07.simulation;

import java.io.IOException;

import org.matsim.utils.vis.netvis.NetVis;

import teach.multiagent07.net.CANetStateWriter;
import teach.multiagent07.net.CANetwork;
import teach.multiagent07.net.CANetworkReader;
import teach.multiagent07.population.Population;
import teach.multiagent07.population.PopulationReader;
import teach.multiagent07.util.EventWriterTXT;

public class CAMobSim {
	private static int time = 0;
	private CANetwork net;
	private CANetStateWriter netVis;
	
	private static EventManager eventManager;
	
	public static EventManager getEventManager() { 
		return eventManager;
	}
	
	public static int getCurrentTime() {
		return time;
	}

	public CAMobSim (CANetwork net, CANetStateWriter netVis, EventManager manager) {
		this.net = net;
		this.netVis = netVis;
		eventManager = manager;
	}
	
	public void doSim(int starttime, int endtime) {

		// prepare netVis
		netVis.open();
		
		try {
			// simulate movement
			for (time = starttime; time < endtime; time++) {
				net.move(time);
				netVis.dump(time);
				if (time % 3600 == 0 )
					System.out.println("Simulating: Time: " + time /3600 + "h 0min");
			}
			// finish netVis
			netVis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run(Population population) {
		//Create Drivers from Population
		population.runHandler(new CreatePlannedVehicles(this));
		// do sim steps
		doSim( 6*3600, 12*3600);
	}

	public static void main(String[] args) {
		// PFADE BITTE �NDERN
		String netFileName = "G:\\TUBerlin\\\\tmp\\studies\\equil\\equil_net.xml";
		String visFileName = "../../tmp/testViz";
		
		// Create network
		CANetwork net = new CANetwork();

		// Read network
		CANetworkReader reader = new CANetworkReader(net, netFileName);
		reader.readNetwork();
		// connect network
		net.connect();
		// build network
		net.build();
		// fill with vehicles
		//net.randomfill(0.4);

		// Read plans
		String popFileName = "..\\..\\tmp\\studies\\equil\\equil_plans.xml";
		
		Population population = new Population();
		PopulationReader popreader = new PopulationReader(population, net, popFileName);
		popreader.readPopulation();
		
		// open network writer
		CANetStateWriter netVis = CANetStateWriter.createWriter(net, netFileName, visFileName);

		CAMobSim sim = new CAMobSim(net, netVis, new EventManager());
		
		eventManager = new EventManager();
		
		// print ALL selected plans
		//population.runHandler(new PersonsWriterTXT());
		population.runHandler(new CreatePlannedVehicles(sim));

		// do sim steps
		sim.doSim( 5*3600, 12*3600);
		
		eventManager.runHandler(new EventWriterTXT());
		
		// start visualizer
		String[] visargs = {visFileName};
		NetVis.main(visargs);

		
	}

}
