/* *********************************************************************** *
 * project: org.matsim.*
 * SimRunKreisverkehr.java
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

import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.vis.netvis.NetVis;

public class SimRunKreisverkehr {

	public static void main(String[] args) {
//		String netFileName = "test/simple/equil_net.xml";
//		String popFileName = "test/simple/equil_plans.xml";
		String netFileName = "..\\..\\tmp\\studies\\berlin-wip\\network\\wip_net.xml";

		String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\kutter010Jakob-Kaiser-RingONLY.plans.v4.xml";
		String arg0 = "..\\..\\tmp\\studies\\berlin-wip\\config_ds.xml";

		String[] args2 = {arg0, "E:/Development/tmp/dtd/config_v1.dtd"};
		Gbl.startMeasurement();
		Config config = Gbl.createConfig(null);
		String localDtdBase = "./dtd/";
		config.global().setLocalDtdBase(localDtdBase);
		

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);

		int cellcount = 0;
		int cellcount2 = 0;
		int count3 = 0;
		for (LinkImpl link : network.getLinks().values()) {
			double length = link.getLength()*link.getLanesAsInt(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
			cellcount += Math.ceil(length/7.5);
			cellcount2 += link.getLength();
		}
		System.out.println("Summarized Cell count is " + cellcount +" on a net of length " + cellcount2/1000 +" km");
		int buffercount = 0;
		for (LinkImpl link : network.getLinks().values()) {
			double cap = link.getFlowCapacity(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
			buffercount += Math.ceil(cap);
		}
		System.out.println("Summarized buffer count is " + buffercount);
		System.exit(0);

//		BasicLinkSetI links = network.getLinks();
//		Iterator it = links.iterator();
//		while (it.hasNext()){
//			QueueLink link = (QueueLink)it.next();
//			double veh_flow = link.getMaxFlow_veh_s();
//			if (link.getFreeTravelDuration() < (0.1/veh_flow)) {
//				Gbl.noteMsg(Object.class, "LinkCheck", "Freetraveltime < maxveh for" + link.getID() + " diff: " + link.getFreeTravelDuration() + " : " + (1.0/veh_flow));
//			}
//		}
//		System.exit(0);

		PopulationImpl population = new MyPopulation();
		// Read plans file with special Reader Implementation
		PopulationReader plansReader = new MatsimPopulationReader(population, network);
		plansReader.readFile(popFileName);

		EventsManagerImpl events = new EventsManagerImpl() ;
		events.addHandler(new EventWriterXML("MatSimJEventsXML.txt"));
		events.addHandler(new EventWriterTXT("MatSimJEvents2.txt"));

		//Config.getSingleton().setParam(Simulation.SIMULATION, Simulation.STARTTIME, "05:55:00");
		//Config.getSingleton().setParam(Simulation.SIMULATION, Simulation.ENDTIME, "08:00:00");

		QueueSimulation sim = new QueueSimulation(network, population, events);
		sim.openNetStateWriter("../../tmp/testWrite2", netFileName, 60);

		sim.run();

		events.resetHandlers(1); //for closing files etc..

		Gbl.printElapsedTime();

		String[] visargs = {"../../tmp/testWrite"};
		NetVis.main(visargs);
	}

}

///////////////////////////////////////////////////////////////////////////
// REMARKS etc
// TODO:

// Ich finde, so etwas wie new XYZSimulation ( plans, network/world, events ) macht sehr viel Sinn.

// Es muss moeglich sein, die Implementationen zu erweitern.  Bei Plans ist das klar:
// class MyPlans extends Plans ... und dann Plans plans = new MyPlans().  Bei Network/World
// ist mir das nicht mehr klar.

// Ich bin skeptisch, ob es Sinn macht, verschiedene Implementationen ueber config.xml in der
// factory-Methode zu definieren.  Bei den readern mag das bzgl. dtd-Version noch Sinn machen;
// beim networkLayer sehe ich das eher nicht.

// Allgemeineres Argument: Soweit ich das im Moment verstehe, muessen neue network types in
// NetworkLayerBuilder.newNetworkLayer eingetragen werden.  Das ist ganz sicher nicht gut, denn
// es bedeutet, dass neue Netzwerk-Typen nur eingetragen werden koennen, indem man in den
// vorhandenen code eingreift.  Das wuerden wir doch gar nicht wollen, oder???

// playground ist eigentlich schon in test

// wir muessen die tests von balmermi laufen lassen koennen

////	Gbl.createWorld();
////	Gbl.createFacilities();
//
//	World world = World.getSingleton() ;
//	//NetworkLayer network = world.createNetworkLayer() ;
//
//	QueueNetwork net = new QueueNetwork();
//	//NetworkReader  = new Networl
//	world.setNetworkLayer(net);
//
//
//
////	Ich glaube, so etwas wie
////	    Network network = new MyNetwork();
////		world.addNetwork(network) ;
////    waere mir immer noch sympathischer ...
//
////	// neue Syntax.  Die ist mir jetzt zu muehsam.  Abfangen!
////	NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
////	NetworkLayer network = (NetworkLayer) world.createLayer(NetworkLayer.LAYER_TYPE,"false",null);
//
//	NetworkParser network_parser = new NetworkParser(network);
//	network_parser.parse();
//
//	Plans plans = new Plans();
//	PlansReaderI plansReader = PlansReaderBuilder.getPlansReader(plans) ;
//	plansReader.read();
//
//	// run sim
//	Simulation sim = new QueueSimulation ( network, plans, null );
//	sim.doSim() ;
//
////	Gbl.createWorld() ;

//		// KOMISCH:
//
//		World world = World.createSingleton() ;
//
//		Network network = new MyNetwork() ;
//		network.read() ;
//		world.addNetworkLayer ( network ) ;
//
//		Population population = new MyPopulation() ;
//		population.read() ;
//		world.addPopulation(population);
//
//		Events events = new Myevents() ;
//		world.addEvents ( events ) ;
//
//		MobSim sim = new MySimulation(  ) ;
//		world.addSimulation ( sim ) ;
//
//
//		world.addAlgorithm( reroute, 10);
//		//world.addAlgorithm(...)args;
//		world.compose();
//
//
//		for ( int iteration=1 ; iteration<=99 ; iteration++ ) {
//
//			world.run() ;
//
//			PlansAlgorithm routeAlgo = new Router(network);
//			population.addAlgorithm(routeAlgo, 10);
//
//			PlansAlgorithm scndLocAlgo = new MyLocAlgo(network) ;
//			population.addAlgorithm( scndLocAlgo, 10 ) ;
//
//			population.connectAlgorithms() ;
//
//			population.runPersonAlgorithms() ;
//
//		}
