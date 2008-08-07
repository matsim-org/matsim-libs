/* *********************************************************************** *
 * project: org.matsim.*
 * GeneratePlans.java
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
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Plans;
import org.matsim.router.Dijkstra;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;

public class GeneratePlans {

	/**
	 * Generating an extra stream of traffic in a certain area of the Berlin network
	 *
	 * @param plans2
	 * @param net
	 */

	public static void createCOOPERSSpecificVehicles(Plans plans2, NetworkLayer net) {
		// Erzeuge einen strom von vehicles, die die autobahn runter wollen
		Link startLink1 = net.getLinks().get("7829");
		Link startLink2 = net.getLinks().get("8453");
		Link destLink = net.getLinks().get("8380");
		Random rnd = new Random(4711);

		Gbl.getConfig().setParam("planCalcScore", "traveling", "-3600");
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		final Dijkstra dijkstra = new Dijkstra(net, timeCostCalc, timeCostCalc);


		for (int i = 0; i< 4000; i++) {
			// Hope this does not clash...
			double earliestStartTime = 8*3600;

			String ID = Integer.toString(Integer.MAX_VALUE - i);
			Person person = new Person(new IdImpl(ID));
			Plan plan = new Plan(person);
			plan.setScore(0.0);
			double endTime = earliestStartTime + (int)(rnd.nextDouble()*2.*3600);
			double arrivalTime = earliestStartTime + 3.*3600;
			Link startLink = rnd.nextDouble() < 0.5 ? startLink1 : startLink2;
			Act actstart = new Act("h", 0,0, startLink, 0, endTime, endTime, false);
			Act actEnd = new Act("h", 0,0, destLink, arrivalTime, arrivalTime + 3*3600, 3*3600, false);

			Leg leg = new Leg(0, "car", "00","00","00");
			org.matsim.population.Route route = dijkstra.calcLeastCostPath(startLink.getToNode(), destLink.getFromNode(), endTime);
			if (route == null) throw new RuntimeException("No route found from node 2 to 22");
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
	}


}
