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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.misc.Time;

import de.schlichtherle.io.FileInputStream;

public class StandaloneSimTest {

	public static void outputFile(String netFileName, String popFileName, String outFile){
		
	}
	public static void main(final String[] args) {
//		String netFileName = "test/simple/equil_net.xml";
//		String popFileName = "test/simple/equil_plans.xml";
//		String netFileName = "/TUBerlin/workspace/berlin-wip/network/wip_net.xml";
//		String popFileName = "/TUBerlin/workspace/berlin-wip/synpop-2006-04/kutter_population/kutter010car_hwh.routes_wip.plans.xml";
//		String netFileName = "../../tmp/studies/ivtch/run657/input/ivtch-osm.xml";
//		String popFileName = "../../tmp/studies/ivtch/run657/input/plans_all_zrh30km_transitincl_10pct.xml.gz";
		String netFileName = "../../tmp/studies/ivtch/Diss/input/ivtch-osm.xml";
		String popFileName = "../../tmp/studies/ivtch/Diss/input/plans1p.xml";

		final Config config = Gbl.createConfig(args);

		String localDtdBase = "./dtd/";
		config.global().setLocalDtdBase(localDtdBase);


		Gbl.startMeasurement();
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);

		PopulationImpl population = new PopulationImpl();
		PopulationReader plansReader = new MatsimPopulationReader(population, network);
		plansReader.readFile(popFileName);
		Gbl.printElapsedTime();

		if(true){
//				FileOutputStream fos;
//				try {
////					fos = new FileOutputStream("t.tmp");
////					ObjectOutputStream oos = new ObjectOutputStream(fos);
////					
////					oos.writeInt(12345);
////					//oos.writeObject("Today");
////					oos.writeObject(network);
////					oos.writeObject(population);
////				
////					oos.close();
//					Gbl.startMeasurement();
//					BufferedInputStream fin = new BufferedInputStream(new FileInputStream("t.tmp"),500000);
//					ObjectInputStream oin = new ObjectInputStream(fin);
//					int test = oin.readInt();
//					network = (NetworkLayer)oin.readObject();
//					PopulationImpl pp = (PopulationImpl)oin.readObject();
//					test++;
//					population = pp;
//					Gbl.printElapsedTime();
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (Throwable e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				

			ZipOutputStream zos = null;
			ObjectOutputStream outFile;
			try {
				zos = new ZipOutputStream(new BufferedOutputStream(
						new FileOutputStream("output/OTFQuadfile1p+pop.mvi"), 50000));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			try {
				zos.putNextEntry(new ZipEntry("net+population.bin"));
				outFile = new ObjectOutputStream(zos);
				outFile.writeObject(network);
				outFile.writeObject(population);
				zos.closeEntry();
				zos.close();
				// END HERE
				System.exit(0);
				
//				
//				Gbl.startMeasurement();
//				File sourceZipFile = new File("testpop.zip");
//				// Open Zip file for reading
//				ZipFile zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
//				ZipEntry infoEntry = zipFile.getEntry("net+population.bin");
//				BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(infoEntry));
//				ObjectInputStream inFile = new ObjectInputStream(is);
//				network = (NetworkLayer)inFile.readObject();
//				population = (PopulationImpl)inFile.readObject();
//				Gbl.printElapsedTime();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			catch (ClassNotFoundException e) {
//				e.printStackTrace();
//			}
//			System.exit(0);
		}
		EventsManagerImpl events = new EventsManagerImpl() ;
//		events.addHandler(new EventWriterXML("MatSimJEventsXML.txt"));
//		events.addHandler(new EventWriterTXT("MatSimJEvents2.txt"));

//		config.simulation().setStartTime(Time.parseTime("05:55:00"));
		config.simulation().setEndTime(Time.parseTime("24:00:00"));
//		config.simulation().setStartTime(Time.parseTime("05:55:00"));

		config.simulation().setStuckTime(10);
//		config.simulation().removeStuckVehicles(false);
		config.simulation().setRemoveStuckVehicles(true);
		config.simulation().setFlowCapFactor(1.5);
		config.simulation().setStorageCapFactor(2.5);

//		QueueLink link = (QueueLink)network.getLinks().get("15");
//		link.setCapacity()
		QueueSimulation sim = new QueueSimulation(network, population, events);
		//sim.openNetStateWriter("testWrite", netFileName, 10);
		config.simulation().setSnapshotFormat("none");
		config.simulation().setSnapshotPeriod(300);

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
