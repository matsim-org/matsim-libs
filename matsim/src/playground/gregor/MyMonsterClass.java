/* *********************************************************************** *
 * project: org.matsim.*
 * MyMonsterClass.java
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

package playground.gregor;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.evacuation.EvacuationAreaFileWriter;
import org.matsim.evacuation.EvacuationAreaLink;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.network.NetworkWriter;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.plans.Act;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.algorithms.ActLocationFalsifier;
import org.matsim.plans.algorithms.PersonRemoveLinkAndRoute;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.plans.algorithms.PlansFilterActInArea;
import org.matsim.plans.algorithms.XY2Links;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;
import org.matsim.writer.MatsimWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

//import com.vividsolutions.jts.geom.Coordinate;
//import com.vividsolutions.jts.geom.CoordinateSequence;
//import com.vividsolutions.jts.geom.GeometryFactory;
//import com.vividsolutions.jts.geom.LinearRing;
//import com.vividsolutions.jts.geom.Point;
//import com.vividsolutions.jts.geom.Polygon;
//import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * @author laemmel
 *
 */
public class MyMonsterClass {


	//////////////////////////////////////////////////////////////////////
	// asciiNetParser
	//////////////////////////////////////////////////////////////////////
	//
	public static void asciiNetParser(){
		String asciiNet = "./data/berlin_center.arc.txt";
		String asciiNode = "./data/berlin_center.nod.txt";
		String configFile = "./configs/evacuationConf.xml";

		Gbl.createConfig(new String[] {configFile});

		System.out.println("  create new empty network...");
		NetworkLayer network = null;
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		network.setCapacityPeriod("01:00:00");
		try {
			BufferedReader file = IOUtils.getBufferedReader(asciiNode);
			String line = file.readLine();
			if (line.charAt(0) >= '0' && line.charAt(0) <= '9') {
				/* The line starts with a number, so assume it's an event and parse it.
				 * Otherwise it is likely the header.  */
				parseNode(line,network);
			}
			// now all other lines should contain events
			while ( (line = file.readLine()) != null) {
				if (line.charAt(0) != '#')
					parseNode(line,network);
			}
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		String id = "0";
		int i = 0;
		try {
			BufferedReader file = IOUtils.getBufferedReader(asciiNet);
			String line = file.readLine();
			if (line.charAt(0) >= '0' && line.charAt(0) <= '9') {
				/* The line starts with a number, so assume it's an event and parse it.
				 * Otherwise it is likely the header.  */
				parseLink(line,network,id);
			}
			// now all other lines should contain events
			while ( (line = file.readLine()) != null) {
				if (line.charAt(0) == '#') continue;
				id = Integer.toString(++i);
				parseLink(line,network,id);
			}
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		NetworkWriter nw = new NetworkWriter(network,"./networks/berlincenter_net.xml");
		nw.write();

	}





	//////////////////////////////////////////////////////////////////////
	// evacuationTimeCalc
	//////////////////////////////////////////////////////////////////////
	// calcs the evacuation time for each agent ... each iteration
	//for now every thing is hardcoded
	public static void evacuationTimeCalc() {
		String configFile = "./configs/evacuationConf.xml";

		Config config = Gbl.createConfig(new String[] {configFile});

		int agents = 165571;
		int [][] agentsLeft = new int[10000][2];

//		EvacuationTimeCalculator etc = new EvacuationTimeCalculator(agentsLeft,agents, 11*3600);

		Events events =  new Events();
//		events.addHandler(etc);

		String outdir_prefix = "/home/laemmel/workspace/matSimJ/output/ITERS/it.";
		int first_iter = 0;
		int last_iter = 200;
		int [] evacTime = new int[last_iter-first_iter];

		for (int i = 0; i<= last_iter; i++ ){
			String current_dir = outdir_prefix + i;
			String current_file = i + ".events.txt.gz";

			String eventsFile = current_dir + "/" + current_file;
			new MatsimEventsReader(events).readFile(eventsFile);
//			agentsLeft = etc.getEventArray();

			StatsWriter writer = new StatsWriter();
			writer.openFile(current_dir + "/"  + i + ".evacuationTime.txt");

			for (int j = 0; j<agentsLeft.length; j++){

				if (agentsLeft[j][0] == 0)
					break;
				writer.write((agentsLeft[j][0] - 39600) + "\t" + agentsLeft[j][1] +"\n");

			}
			writer.close();
//			evacTime[i] = etc.getLastEventTime() - 11*3600;
//			etc.reset(i);
			events.resetCounter();
			System.out.println("done");

		}

		StatsWriter writer = new StatsWriter();
		writer.openFile("./output/evacTime.txt");
		for (int i = 0; i< evacTime.length; i++) {
			writer.write(evacTime[i] + "\n");
		}

	}




	//////////////////////////////////////////////////////////////////////
	// networkToPedestrianConverter
	//////////////////////////////////////////////////////////////////////
	// reads and writes plans, without running any algorithms
	public static void networkToPedestrianConverter(){
		String configFile = "./configs/evacuationConf.xml";

		Config config = Gbl.createConfig(new String[] {configFile});

//		System.out.println("  reading world xml file... ");
//		final WorldParser world_parser = new WorldParser(Gbl.getWorld());
//		world_parser.parse();
//		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");
		for (Link link : network.getLinks().values()) {
			link.setFreespeed(1.666);
			link.setCapacity(link.getCapacity() * 8.19 );
		}
		NetworkWriter nw = new NetworkWriter(network,"./networks/evacuationnet_zurich_navteq.xml");
		nw.write();

	}

	//////////////////////////////////////////////////////////////////////
	// networkReadWrite
	//////////////////////////////////////////////////////////////////////
	// reads and writes networ
	public static void networkReadWrite(){
		String configFile = "./configs/evacuationConf.xml";

		Config config = Gbl.createConfig(new String[] {configFile});

//		System.out.println("  reading world xml file... ");
//		final WorldParser world_parser = new WorldParser(Gbl.getWorld());
//		world_parser.parse();
//		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");
		NetworkWriter nw = new NetworkWriter(network,"./networks/padang_net_evac_2.xml");
		nw.write();

	}


	//////////////////////////////////////////////////////////////////////
	// workActExtractor
	//////////////////////////////////////////////////////////////////////
	// reads and writes plans, without running any algorithms
	public static void workActExtractor() {
		String configFile = "./configs/evacuationConf.xml";

		Config config = Gbl.createConfig(new String[] {configFile});

//		System.out.println("  reading world xml file... ");
//		final WorldParser world_parser = new WorldParser(Gbl.getWorld());
//		world_parser.parse();
//		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		Plans population = new Plans(Plans.NO_STREAMING);

		System.out.println("reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();

		Collection<Person> persons = population.getPersons().values();


		PlansWriter writer = new PlansWriter(population,"./output/evacuationplans_zurich_navteq1.xml","v4");
		writer.writeStartPlans();
		for (Person person : persons){
			Person nperson = new Person(person.getId().toString(), person.getSex(), (new Integer(person.getAge())).toString(),person.getLicense(),person.getCarAvail(),person.getEmployed());

			BasicAct act = new BasicAct();
			Iterator it = person.getPlans().get(0).getIteratorAct();

			while (it.hasNext()){
				act  = (BasicAct) it.next();
				if (act.getType().contains("w")) break;
			}


			BasicAct nact = new Act("w",act.getLink().getCenter().getX(),act.getLink().getCenter().getY(),(Link)(act.getLink()),0,39600,39600,true);
			Plan plan = new Plan(nperson);
			plan.addAct(nact);
			nperson.addPlan(plan);
			writer.writePerson(nperson);



		}
		writer.writeEndPlans();

	}




	//////////////////////////////////////////////////////////////////////
	// convertPlans
	//////////////////////////////////////////////////////////////////////
	// reads and writes plans, without running any algorithms

//	public static void convertPlans(final String[] args) {




//		System.out.println("RUN: convertPlans");
//
//		String configFile = "./configs/evacuationConf.xml";
//
//		Gbl.createConfig(new String[] {configFile});
//
//
//		Gbl.createFacilities();
//
////		System.out.println("  reading world xml file... ");
////		final WorldParser world_parser = new WorldParser(Gbl.getWorld());
////		world_parser.parse();
////		System.out.println("  done.");
//
//		System.out.println("  reading the network...");
//		NetworkLayer network = null;
//		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
//		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,"false",null);
//		final NetworkParser network_parser = new NetworkParser(network);
//		network_parser.parse();
//		System.out.println("  done.");
//
////		System.out.println("  reading facilities xml file... ");
////		final FacilitiesParser facilities_parser = new FacilitiesParser(Facilities.getSingleton());
////		facilities_parser.parse();
////		System.out.println("  done.");
//
//		System.out.println("  setting up plans objects...");
//		final Plans plans = new Plans(Plans.USE_STREAMING);
//		final PlansWriter plansWriter = new PlansWriter(plans);
//		plans.setPlansWriter(plansWriter);
//		final PlansReaderI plansReader = PlansReaderBuilder.getPlansReader(plans);
//		System.out.println("  done.");
//
//		System.out.println("  reading and writing plans...");
//		plansReader.read();
//		plans.printPlansCount();
//		plansWriter.write();
//		System.out.println("  done.");
//
//		System.out.println("RUN: convertPlans finished.");
//		System.out.println();
//	}
//
	//////////////////////////////////////////////////////////////////////
	// cuts out a subset from network that is within the specified
	// polygon ... saves set network and the evacuation area as well
	//////////////////////////////////////////////////////////////////////
	public static void networkClipping(final String[] args){
		//for now hardcoded
		World world = Gbl.createWorld();
		Config config = Gbl.createConfig(new String[] {"./configs/evacuationConf.xml"});
		String networkFile = config.network().getInputFile();


		System.out.println("reading network xml file... ");
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);
		world.setNetworkLayer(network);
		System.out.println("done. ");



		ArrayList<Coordinate> coords = new ArrayList<Coordinate>();

		coords.add(MGC.coord2Coordinate(network.getNode("5425").getCoord()));
		coords.add(MGC.coord2Coordinate(network.getNode("5411").getCoord()));
		coords.add(MGC.coord2Coordinate(network.getNode("5313").getCoord()));
//		coords.add(MGC.coord2Coordinate(network.getNode("5952").getCoord()));
		coords.add(MGC.coord2Coordinate(network.getNode("5411").getCoord()));
		coords.add(MGC.coord2Coordinate(network.getNode("5002").getCoord()));


		Coordinate[] c = new Coordinate[coords.size()];
		for (int i = 0; i < coords.size(); i++)
			c[i] = coords.get(i);


		GeometryFactory geofac = new GeometryFactory();
		CoordinateSequence seq = new CoordinateArraySequence(c);
		LinearRing ring = new LinearRing(seq,geofac);
		Polygon poly = new Polygon( ring, null, geofac);



		int one = 0, two = 0;

		ConcurrentLinkedQueue<Node> n = new ConcurrentLinkedQueue<Node>();
		ConcurrentLinkedQueue<Link> l = new ConcurrentLinkedQueue<Link>();
		ConcurrentLinkedQueue<Link> oneWay = new ConcurrentLinkedQueue<Link>();
		HashMap<IdI,EvacuationAreaLink> links = new HashMap<IdI,EvacuationAreaLink>();
		Iterator it = network.getLinks().values().iterator();

		while (it.hasNext()) {
			QueueLink link = (QueueLink) it.next();
			Node a = link.getFromNode();
			Node b = link.getToNode();
			CoordinateSequence seqA = new CoordinateArraySequence(new Coordinate[]{MGC.coord2Coordinate(a.getCoord())});
			Point pointA = new Point(seqA,geofac);
			CoordinateSequence seqB = new CoordinateArraySequence(new Coordinate[]{MGC.coord2Coordinate(b.getCoord())});
			Point pointB = new Point(seqB,geofac);

			if (!poly.contains(pointA) && !poly.contains(pointB) && !poly.touches(pointA) && !poly.touches(pointB)) {
				l.add(link);
				n.add(a);
				n.add(b);
				continue;
			} else {
				Iterator outIt = b.getOutLinks().values().iterator();
				QueueLink oneWayLink = link;
				while(outIt.hasNext()){

					Node temp = ((QueueLink)outIt.next()).getToNode();
					if (temp == a){
						oneWayLink = null;
						break;
					}
				}
				if (oneWayLink != null) {
					oneWay.add(oneWayLink);
					one++;
				}else {
					link.setCapacity(link.getCapacity()*2);
					two++;
				}
				EvacuationAreaLink el = new EvacuationAreaLink((Id) link.getId(),3600.0 * 11 + 85*60);
				links.put(el.getId(),el);
			}






		}



int three=0;
		System.out.println(oneWay.size() + " " +  one + " " + two + " " + network.getLocations().size());
		Link oneWayLink = oneWay.poll();
		while (oneWayLink != null){
			Node toNode = oneWayLink.getFromNode();
			Node fromNode = oneWayLink.getToNode();
			Link testlink = network.createLink(oneWayLink.getId().toString() + "666", fromNode.getId().toString(), toNode.getId().toString(),((Double) oneWayLink.getLength()).toString(), ((Double)oneWayLink.getFreespeed()).toString(),((Double)oneWayLink.getCapacity()).toString(), ((Integer)oneWayLink.getLanes()).toString(), oneWayLink.getOrigId() + "666",oneWayLink.getType());

			EvacuationAreaLink el = new EvacuationAreaLink((Id) testlink.getId(),3600.0 * 11 + 85*60);
			links.put(el.getId(),el);
			oneWayLink = oneWay.poll();
			three++;
		}
		System.out.println(oneWay.size() + " " +  one + " " + two + " " + three + " " + network.getLocations().size());
		EvacuationAreaFileWriter enfw = new EvacuationAreaFileWriter(links);
		try {
			enfw.writeFile("./output/evacuationarea_padang_cutout.xml.gz");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		Link link = l.poll();
		while (link != null){
			network.removeLink(link);
			link = l.poll();
		}

		Node node = n.poll();
		while (node != null) {
			network.removeNode(node);
			node = n.poll();
		}




		new NetworkCleaner().run(network);

		NetworkWriter nw = new NetworkWriter(network,"./output/padang_net_cutout.xml.gz");
		nw.write();




	}

	//////////////////////////////////////////////////////////////////////
	// checks whether a route intersects a specific area
	// if it is the case the correspondig Person will be dumped
	// to a plansfile
	//////////////////////////////////////////////////////////////////////
	public static void routeTransitionCheck(final String[] args){
		System.out.println("RUN: routeTransitionCheck");
		//for now hardcoded
		String networkFile = "./networks/wip_net.xml";
		String plansFile = "./networks/kutter001car5.debug.router_wip.plans.xml.gz";
		String configFile = "./configs/evacuationConf.xml";

		double radius = 1000;
		Coord center = new Coord(4595406.5, 5821171.5);

		HashMap<IdI,Link> areaOfInteresst = new HashMap<IdI,Link>();

		Config config = Gbl.createConfig(new String[] {configFile});
		World world = Gbl.getWorld();


		System.out.println("reading network xml file... ");
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);
		world.setNetworkLayer(network);
		System.out.println("done. ");

		Plans population = new Plans(Plans.NO_STREAMING);

		System.out.println("reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(plansFile);
		population.printPlansCount();
		world.setPopulation(population);

		System.out.println("done. ");

		System.out.println("extracting aoi ...");
		for (Link link : network.getLinks().values()) {
			Node from = link.getFromNode();
			Node to = link.getToNode();
			double fromDist = from.getCoord().calcDistance(center);
			double toDist = to.getCoord().calcDistance(center);
			if (Math.min(fromDist, toDist) <= radius) areaOfInteresst.put(link.getId(),link);
		}
		System.out.println("done. ");

		System.out.println("filtering persons ...");
		PlansWriter writer = new PlansWriter(population,"testplans.xml","v4");
		writer.writeStartPlans();
//		PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(writer, areaOfInteresst);
//		Collection<Person> persons = (Collection<Person>) population.getPersons().values();
//		for (Person person : persons){
//			filter.run(person);
//
//		}
//		writer.writeEndPlans();
//		System.out.println("done. ");

		System.out.println("RUN: routeTransitionCheck finished");

	}

	//////////////////////////////////////////////////////////////////////
	// position extracter
	//
	//////////////////////////////////////////////////////////////////////
	public static void positionExtractor(){
		//for now hardcoded
		String plans = "./networks/census2000dilZh30km_navteq.xml.gz";
		String subNetfile = "./networks/evacuationnet_zurich_navteq.xml";
//		String netfile = "./networks/evacuationnet_zurich_navteq.xml";
		String netfile = "./networks/navteq_network.xml.gz";
		String configFile = "./configs/evacuationConf.xml";

		World world = Gbl.getWorld();
		Config config = Gbl.createConfig(new String[] {configFile});

		System.out.println("reading network xml file... ");
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
		world.setNetworkLayer(network);
		System.out.println("done. ");

		System.out.println("reading subnetwork xml file... ");
		QueueNetworkLayer subNetwork = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(subNetfile);
//		world.setNetworkLayer(subnetwork);
		System.out.println("done. ");


		Plans population = new Plans(Plans.USE_STREAMING);
		PlansAlgorithm algo = new PlansFilterActInArea(subNetwork,"w");
		population.addAlgorithm(algo);

		System.out.println("reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(plans);
		System.out.println("done. ");

		population = ((PlansFilterActInArea)algo).getPlans();
		population.printPlansCount();
		world.setPopulation(population);

		PlansWriter writer = new PlansWriter(population,"./networks/evacuationplans_zurich_navteq.xml","v4");
		writer.writeStartPlans();
				Collection<Person> persons = population.getPersons().values();
		for (Person person : persons){
			writer.run(person);

		}
		writer.writeEndPlans();

	}

	public static void planMinMax(){
		String plans = "/home/laemmel/workspace/runs/run301/run/output/ITERS/it.100/100.plans.xml.gz";

		String configFile = "./configs/evacuationConf.xml";
		String net = "./networks/padang_net_evac.xml";
		World world = Gbl.getWorld();
		Config config = Gbl.createConfig(new String[] {configFile});


		System.out.println("reading old network xml file... ");
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(net);
		world.setNetworkLayer(network);
		System.out.println("done. ");


		Plans population = new Plans(Plans.NO_STREAMING);

		System.out.println("reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(plans);
		population.printPlansCount();

		world.setPopulation(population);

		System.out.println("done. ");

		Writer wr = null;
		try {
			wr = new FileWriter(new File("scores.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter out = new BufferedWriter(wr);






		for (Person pers : population.getPersons().values()){
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;

			for (Plan plan : pers.getPlans()){
				if (plan.getScore() < min) min = plan.getScore();
				if (plan.getScore() > max) max = plan.getScore();
			}
			try {
				out.write(min + " " + max + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//////////////////////////////////////////////////////////////////////
	// maps agents from one network to an other - based on their
	// rondomized xy coords
	//////////////////////////////////////////////////////////////////////
	public static void planMapper(){
		String in_plans = "./zurich100p/positionInfoPlansFile09-00-00.xml";
		String out_plans = "./zurich100p/positionInfoPlansFile09-00-00_zurich_navteq.xml";
		String netfile = "./networks/navteq_network_zurich.xml";
		String old_netfile = "./networks/zurich_net.xml";
		String configFile = "./configs/evacuationConf.xml";

		World world = Gbl.getWorld();
		Config config = Gbl.createConfig(new String[] {configFile});


		System.out.println("reading old network xml file... ");
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(old_netfile);
		world.setNetworkLayer(network);
		System.out.println("done. ");


		Plans population = new Plans(Plans.NO_STREAMING);

		System.out.println("reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(in_plans);
		population.printPlansCount();

		world.setPopulation(population);

		System.out.println("done. ");

		System.out.println("performing network2network mapping... ");
		PlansAlgorithm rm = new PersonRemoveLinkAndRoute();
		PlansAlgorithm alf = new ActLocationFalsifier(500.0);


		rm.run(population);
		alf.run(population);

		System.out.println("reading new network xml file... ");
		network = null;
		QueueNetworkLayer new_network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
//		world.setNetworkLayer(network);
		System.out.println("done. ");

		PlansAlgorithm xy = new XY2Links(new_network);
		world.setNetworkLayer(new_network);
		xy.run(population);

		PlansWriter writer = new PlansWriter(population,out_plans,"v4");
		writer.writeStartPlans();
		Collection<Person> persons = population.getPersons().values();
		for (Person person : persons){
			writer.run(person);

		}
		writer.writeEndPlans();


	}

	public static void networkCutter(){
		String netfile = "./networks/navteq_network.xml.gz";
		String configFile = "./configs/routerTest.xml";


		World world = Gbl.getWorld();
		Config config = Gbl.createConfig(new String[] {configFile});

		System.out.println("reading network xml file... ");
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
		world.setNetworkLayer(network);
		System.out.println("done. ");

		network.beforeSim();

		System.out.println("extracting links ... ");
		Coord A = network.getNode("101472392").getCoord();
		Coord B = network.getNode("101465289").getCoord();
		double maxX = Math.max(A.getX(),B.getX());
		double maxY = Math.max(A.getY(),B.getY());
		double minX = Math.min(A.getX(),B.getX());
		double minY = Math.min(A.getY(),B.getY());

		QueueNetworkLayer new_network = new QueueNetworkLayer();

		for (QueueNode node : network.getNodes().values()) {
			if (node.getCoord().getX() <= maxX && node.getCoord().getX() >= minX )
				if (node.getCoord().getY() <= maxY && node.getCoord().getY() >= minY )
					new_network.createNode(node.getId().toString(), Double.toString(node.getCoord().getX()), Double.toString(node.getCoord().getY()), node.getType());
		}

		int extracted = 0;
		for (QueueLink link : network.getLinks().values()) {
			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			if (from.getX() <= maxX && (from.getX() >= minX)) {
				if (from.getY() <= maxY && (from.getY() >= minY)) {
					if (to.getX() <= maxX && (to.getX() >= minX)) {
						if (to.getY() <= maxY && (to.getY() >= minY)) {

							new_network.createLink(link.getId().toString(),
									link.getFromNode().getId().toString(), link
											.getToNode().getId().toString(),
									Double.toString(link.getLength()),
									Double.toString(link.getFreespeed()),
									Double.toString(link.getCapacity()),
									Integer.toString(link.getLanes()),
									link.getOrigId(), link.getType());
							extracted++;
							if (extracted % 1000 == 0)
								System.out.println(extracted
										+ " extracted so far");
							continue;
						}
					}
				}
			}
		}
		new_network.setCapacityPeriod(Integer.toString(network.getCapacityPeriod()));
		network = null;
		System.out.println("done");


		System.out.println("Cleaning up network ...");
		new_network.beforeSim();
		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(new_network);

		System.out.println("done");

		System.out.println("writing modified networkfile ...");
		 NetworkWriter writer = new NetworkWriter(new_network,Gbl.getConfig().findParam("network", "outputNetworkFile"));
		 writer.write();
		 System.out.println("done");


	}

	public static void networkClipperTest(){
		String netfile = "./networks/navteq_network.xml.gz";
		String configFile = "./configs/evacuationConf.xml";


		World world = Gbl.getWorld();
		Config config = Gbl.createConfig(new String[] {configFile});

		System.out.println("reading network xml file... ");
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
		world.setNetworkLayer(network);
		System.out.println("done. ");

		List<Node> shell = new ArrayList<Node>();
		shell.add(network.getNode("101489910"));
		shell.add(network.getNode("101489913"));
		shell.add(network.getNode("101463848"));
		shell.add(network.getNode("101464213"));
		shell.add(network.getNode("101455893"));
		shell.add(network.getNode("101453477"));
		shell.add(network.getNode("101453987"));
		shell.add(network.getNode("101457923"));
		shell.add(network.getNode("101500181"));
		shell.add(network.getNode("101490852"));
		shell.add(network.getNode("101458204"));
		shell.add(network.getNode("101457358"));
		shell.add(network.getNode("101499942"));
		shell.add(network.getNode("101452753"));
		shell.add(network.getNode("101452198"));
		shell.add(network.getNode("101452171"));
		shell.add(network.getNode("101463394"));
		shell.add(network.getNode("101489910"));
//		NetworkClipper nc = new NetworkClipper(network,shell);
//		nc.run();
		NetworkWriter nw = new NetworkWriter(network,"./networks/geotoolsTest.xml");
		nw.write();
	}

	public static void plansReduction(){
		String configFile = "./configs/evacuationConf.xml";

		Config config = Gbl.createConfig(new String[] {configFile});

//		System.out.println("  reading world xml file... ");
//		final WorldParser world_parser = new WorldParser(Gbl.getWorld());
//		world_parser.parse();
//		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		Plans population = new Plans(Plans.NO_STREAMING);

		System.out.println("reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();

		PlansWriter writer = new PlansWriter(population,"./networks/padang_plans10p.xml","v4");
		writer.write();
	}

	public static void main(final String[] args){

		//PlansAlgorihtm
//		routeTransitionCheck(args);

//		planMapper();

//		networkCutter();

		networkClipping(args);

//		positionExtractor();

//		convertPlans(args);

//		workActExtractor();

//		networkToPedestrianConverter();

//		evacuationTimeCalc();

//		networkClipperTest();

//		asciiNetParser();

//		plansReduction();
//		planMinMax();

//		networkReadWrite();
	}


//	//helper methods
//	private static Coordinate coord2Coordinate(Coord coord){
//
//
//		return new Coordinate(coord.getX(),coord.getY());
//
//	}

	private static void parseNode(final String line, final NetworkLayer network) {

		//// Sioux Falls
//		String[] result = line.split("\t", 4);
		//// Berlin
		String[] result = line.split(" ", 4);
		double x = Double.parseDouble(result[1]) * 1000;
		double y = Double.parseDouble(result[2]) * 1000;
		network.createNode(result[0], Double.toString(x), Double.toString(y), null);
	}
	private static void parseLink(final String line, final NetworkLayer network, final String id) {


		//// Sioux Falls
//		String[] result = line.split("\t", 7);
//		Node fromNode = network.getNode(result[1]);
//		Node toNode = network.getNode(result[2]);
//		double length = Double.parseDouble(result[4])*1600; // length is given in miles
//		double freeFlowTime = (Double.parseDouble(result[5])/100)*3600; // free flow time is given in 0.01 hours ...
//		double freeSpeed = length / freeFlowTime;
//		network.createLink(id, result[1], result[2],Double.toString(length) ,Double.toString(freeSpeed),result[3] ,"1", id, null);

////	 Berlin
		String[] result = line.split(" ", 7);
		if (result[5].equals("0")) return;
		Node fromNode = network.getNode(result[1]);
		Node toNode = network.getNode(result[2]);

//
		double length = fromNode.getCoord().calcDistance(toNode.getCoord());
//		double freeFlowTime = (Double.parseDouble(result[5])/100)*3600; // free flow time is given in 0.01 hours ...
//		double freeSpeed = length / freeFlowTime;
		network.createLink(result[0], result[1], result[2],Double.toString(length),"13.88","2000" ,"1", id, null);
	}


	//a simple writer to dump the stats to file
	private static class StatsWriter extends MatsimWriter{

		@Override
		public void close(){
			try {
				super.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


		@Override
		public void openFile(final String filename){
			try {
				super.openFile(filename);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void write(final String str) {
			try {
				this.writer.write(str);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}


