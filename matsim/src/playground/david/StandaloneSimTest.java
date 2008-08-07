/* *********************************************************************** *
 * project: org.matsim.*
 * StandaloneSimTest.java
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

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Plans;
import org.matsim.population.PlansReaderI;
import org.matsim.utils.misc.Time;
import org.matsim.world.World;

public class StandaloneSimTest {

	public static void main(final String[] args) {
//		String netFileName = "test/simple/equil_net.xml";
//		String popFileName = "test/simple/equil_plans.xml";
		String netFileName = "/TU Berlin/workspace/berlin-wip/network/wip_net.xml";
		String popFileName = "/TU Berlin/workspace/berlin-wip/synpop-2006-04/kutter_population/kutter010car_hwh.routes_wip.plans.xml";

		Gbl.startMeasurement();
		final Config config = Gbl.createConfig(args);

		String localDtdBase = "./dtd/";
		Gbl.getConfig().global().setLocalDtdBase(localDtdBase);
		
		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
				new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);

		Plans population = new Plans();
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(popFileName);

		Events events = new Events() ;
//		events.addHandler(new EventWriterXML("MatSimJEventsXML.txt"));
//		events.addHandler(new EventWriterTXT("MatSimJEvents2.txt"));

//		config.simulation().setStartTime(Time.parseTime("05:55:00"));
		config.simulation().setEndTime(Time.parseTime("24:00:00"));
//		config.simulation().setStartTime(Time.parseTime("05:55:00"));

		config.simulation().setStuckTime(100);
		config.simulation().removeStuckVehicles(false);
		config.simulation().removeStuckVehicles(true);

//		QueueLink link = (QueueLink)network.getLinks().get("15");
//		link.setCapacity()
		QueueSimulation sim = new QueueSimulation(network, population, events);
		//sim.openNetStateWriter("testWrite", netFileName, 10);

		sim.run();

		events.resetHandlers(1); //for closing files etc..

		Gbl.printElapsedTime();
//
//		String[] visargs = {"testWrite"};
//		NetVis.main(visargs);
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
//	QueueNetworkLayer net = new QueueNetworkLayer();
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
