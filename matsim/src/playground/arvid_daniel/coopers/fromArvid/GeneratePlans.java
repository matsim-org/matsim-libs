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

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.LinkImpl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.router.Dijkstra;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;

public class GeneratePlans {

	/**
	 * Generating an extra stream of traffic in a certain area of the Berlin network
	 *
	 * @param plans2
	 * @param net
	 */

	public static void createCOOPERSSpecificVehicles(Plans plans2, QueueNetworkLayer net) {
		// Erzeuge einen strom von vehicles, die die autobahn runter wollen
		LinkImpl startLink1 = net.getLinks().get("7829");
		LinkImpl startLink2 = net.getLinks().get("8453");
		LinkImpl destLink = net.getLinks().get("8380");
		Random rnd = new Random(4711);

		Gbl.getConfig().setParam("planCalcScore", "traveling", "-3600");
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		final Dijkstra dijkstra = new Dijkstra(net, timeCostCalc, timeCostCalc);


		for (int i = 0; i< 4000; i++) {
			// Hope this does not clash...
			double earliestStartTime = 8*3600;

			String ID = Integer.toString(Integer.MAX_VALUE - i);
			Person person = new Person(ID, null, "33",null,"true","true");
			Plan plan = new Plan("0", person);
			double endTime = earliestStartTime + (int)(rnd.nextDouble()*2.*3600);
			double arrivalTime = earliestStartTime + 3.*3600;
			LinkImpl startLink = rnd.nextDouble() < 0.5 ? startLink1 : startLink2;
			Act actstart = new Act("h", 0,0, startLink, 0, endTime, endTime, false);
			Act actEnd = new Act("h", 0,0, destLink, arrivalTime, arrivalTime + 3*3600, 3*3600, false);

			Leg leg = new Leg("0", "car", "00","00","00");
			org.matsim.plans.Route route = dijkstra.calcLeastCostPath(startLink.getToNode(), destLink.getFromNode(), endTime);
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
