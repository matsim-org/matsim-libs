/* *********************************************************************** *
 * project: org.matsim.*
 * GeneratePlansTest.java
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

package playground.arvid_daniel.coopers.fromArvid;

import java.util.Random;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.Route;

public class GeneratePlansTest {

	/**
	 * Generating an extra stream of traffic in a certain area of the Berlin network
	 *
	 * @param plans2
	 * @param net
	 */

	public static void createCOOPERSSpecificVehicles(Population plans2, NetworkLayer net) {
		// Erzeuge einen strom von vehicles, die die autobahn runter wollen
//		Link startLink1 = net.getLinks().get("7829");
//		Link startLink2 = net.getLinks().get("8453");
//		Link destLink = net.getLinks().get("8380");
		Link startLink1 = net.getLinks().get("0");
		Link startLink2 = net.getLinks().get("2");
//		Link startLink8 = net.getLinks().get("8");
		Link destLink = net.getLinks().get("12");
		Random rnd = new Random(4711);

		Gbl.getConfig().setParam("planCalcScore", "traveling", "-3600");
//		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
//		final Dijkstra dijkstra = new Dijkstra(net, timeCostCalc, timeCostCalc);

		for (int i = 0; i< 4000; i++) {
			double earliestStartTime = 7*3600;

			String ID = Integer.toString(i);
			Person person = new Person(new IdImpl(ID));
			Plan plan = new Plan(person);
			double endTime = earliestStartTime + (int)(rnd.nextDouble()*2.0*3600);
			double arrivalTime = earliestStartTime + 7.*3600;
//			Link startLink = rnd.nextDouble() < 0.5 ? startLink1 : startLink2;
			Act actstart = new Act("h", 0,0, startLink1, 0, endTime, endTime, false);
			Act actEnd = new Act("w", 0,0, destLink, arrivalTime + i, arrivalTime + i + 3*3600, 3*3600, false);

			Leg leg = new Leg(0, "car", endTime, 1.0, (arrivalTime + i));
//			org.matsim.demandmodeling.plans.Route route = dijkstra.calcLeastCostPath(startLink1.getToNode(), destLink.getFromNode(), endTime);
//			if (route == null) throw new RuntimeException("No route found from start node to end node");

			Route route = new Route();
			route.setRoute("1 4 5 13 8 14 11");
			leg.setRoute(route);
			plan.addAct(actstart);
			plan.addLeg(leg);
			plan.addAct(actEnd);
			person.addPlan(plan);
			person.setSelectedPlan(plan);

			try {
				plans2.addPerson(person);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//creating agents on route 2
		for (int i = 4001; i< 8000; i++) {
			double earliestStartTime = 7*3600;

			Person person = new Person(new IdImpl(i));
			Plan plan = new Plan(person);
			double endTime = earliestStartTime + (int)(rnd.nextDouble()*2.*3600);
			double arrivalTime = earliestStartTime + 7.*3600;
			Act actstart = new Act("h", 0,0, startLink1, 0, endTime, endTime, false);
			Act actEnd = new Act("w", 0,0, destLink, arrivalTime + i, arrivalTime + i + 3*3600, 3*3600, false);
			Leg leg = new Leg(0, "car", endTime, 1.0, (arrivalTime + i));

			Route route = new Route();
			route.setRoute("1 4 6 9 11");
			leg.setRoute(route);
			plan.addAct(actstart);
			plan.addLeg(leg);
			plan.addAct(actEnd);


			person.addPlan(plan);
			person.setSelectedPlan(plan);

			try {
				plans2.addPerson(person);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		//creating agents on route 1, starting on link 2
		for (int i = 8001; i< 9000; i++) {
			double earliestStartTime = 7*3600;

			String ID = Integer.toString(i);
			Person person = new Person(new IdImpl(ID));
			Plan plan = new Plan(person);
			double endTime = earliestStartTime + (int)(rnd.nextDouble()*2.*3600);
			double arrivalTime = earliestStartTime + 7.*3600;
			Act actstart = new Act("h", 0,0, startLink2, 0, endTime, endTime, false);
			Act actEnd = new Act("w", 0,0, destLink, arrivalTime + i, arrivalTime + i + 3*3600, 3*3600, false);
			Leg leg = new Leg(0, "car", endTime, 1.0, (arrivalTime + i));

			Route route = new Route();
			route.setRoute("5 13 8 14 11");
			leg.setRoute(route);
			plan.addAct(actstart);
			plan.addLeg(leg);
			plan.addAct(actEnd);


			person.addPlan(plan);
			person.setSelectedPlan(plan);

			try {
				plans2.addPerson(person);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		PopulationWriter plansWriter = new PopulationWriter(plans2, "./test/arvid_daniel/input/testExtended/plansGen.xml", "v4");
//		plansWriter.setUseCompression(true);
		plansWriter.write();
	}

	public static void main(String [] args)  {
		String networkFile = "./test/arvid_daniel/input/testExtended/testNetExtended.xml";
		String plansFile =   "./test/arvid_daniel/input/testExtended/plansGen.xml";
		Gbl.createConfig(null);
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);
		Gbl.getWorld().setNetworkLayer(network);

		Population population = new Population(Population.NO_STREAMING);
		// new MatsimPlansReader(population).readFile(plansFile);

		GeneratePlansTest.createCOOPERSSpecificVehicles(population, network);
	}


}
