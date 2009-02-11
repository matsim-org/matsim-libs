/* *********************************************************************** *
 * project: org.matsim.*
 * DDcontroller.java
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

package playground.dressler.util;

import java.util.*;

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueSimulation;
//import org.matsim.network.MatsimNetworkReader;
//import org.matsim.network.NetworkLayer;
import org.matsim.network.*;
//import org.matsim.population.MatsimPopulationReader;
//import org.matsim.population.Population;
import org.matsim.population.*;
import org.matsim.run.OTFVis;
import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.utils.vis.netvis.streaming.StreamConfig;
import org.matsim.utils.vis.otfvis.executables.OTFEvent2MVI;
import org.matsim.world.World;
import org.matsim.utils.geometry.Coord;
import org.matsim.population.routes.*;


public class DDcontroller {

	public static void main(final String[] args) {
		
    	// choose instance
		//final String netFilename = "./examples/equil/network.xml";
		//final String plansFilename = "./examples/equil/plans100.xml";
		final String netFilename = "./examples/meine_EA/siouxfalls_network_test.xml";
		
		//final String netFilename = "/homes/combi/dressler/V/Project/padang/network/padang_net_evac.xml";
		//final String plansFilename = "/homes/combi/dressler/V/Project/padang/plans/padang_plans_10p.xml.gz";
		//final String plansFilename = "/homes/combi/dressler/V/code/workspace/matsim/examples/meine_EA/padangplans.xml";
		final String plansFilename = "./examples/meine_EA/siouxfalls_plans_test.xml";

		boolean testplans = false; // FIXME !
		boolean dosim = true;		
		boolean otfvis = true;
		boolean netvis = false & (!otfvis);
		
		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(null);		

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();		
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);
		world.complete();
		
		Population population = new Population();
		new MatsimPopulationReader(population).readFile(plansFilename);
		
		if (testplans) {
			for (Person person : population.getPersons().values()) {
				Plan plan = person.getSelectedPlan();
				if (plan == null) {
					System.out.println("Person " + person.getId() + " has no plan.");
					continue;
				}
				Act act = plan.getFirstActivity();			
				if (act == null) {
					System.out.println("Person " + person.getId() + " has no act.");
					continue;
				}
								
				Leg leg = plan.getNextLeg(act);			
				if (leg == null) {
					System.out.println("Person " + person.getId() + " has no leg.");
					continue;
				}
				Route route = leg.getRoute();
				if (route == null) {
					System.out.println("Person " + person.getId() + " has no route.");
					continue;
				}
				
				Node node2 = route.getStartLink().getToNode();
				Node node1 = null;
				for (int n = 1; n < route.getLinkIds().size(); n++) {					
					node1 = network.getLink(route.getLinkIds().get(n)).getFromNode();
					if (node1.getId() != node2.getId()) {
					System.out.println("Person " + person.getId() + " starts on link " + act.getLinkId());
					System.out.println(route.getLinkIds().get(n) + " does not match next link.");					
					System.out.println(node1.getId() + " != " + node2.getId());
					}
					node2 = network.getLink(route.getLinkIds().get(n)).getToNode();
				}
				
				
			}

			System.out.println("Paths tested.");
		}
		
		if (dosim) {
			Events events = new Events();

			EventWriterTXT eventWriter = new EventWriterTXT("./output/events.txt");
			events.addHandler(eventWriter);

			QueueSimulation sim = new QueueSimulation(network, population, events);
			sim.openNetStateWriter("./output/simout", netFilename, 10);
			sim.run();

			eventWriter.closeFile();
			System.out.println("Simulation done.");
		}
		
		if (otfvis) {
			QueueNetwork qnet = new QueueNetwork(network);

			//String eventFile = Gbl.getConfig().getParam("events","outputFile");
			String eventFile = "./output/events.txt";
			OTFEvent2MVI mviconverter = new OTFEvent2MVI(qnet, eventFile, "./output/otfvis.mvi", 10);
			mviconverter.convert();

			String[] visargs = {"./output/otfvis.mvi"};
			OTFVis.main(visargs);
		}
		
		if (netvis) {
			String[] visargs = {"./output/simout"};
			NetVis.main(visargs);			
		}
		
		System.out.println("Done.");
				
	}

}
