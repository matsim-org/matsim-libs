/* *********************************************************************** *
 * project: org.matsim.*
 * AvgRouteDistance.java
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

package playground.lnicolas.routing;

import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.network.Node;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Route;
import org.matsim.population.algorithms.PersonAlgorithm;

public class AvgRouteDistance extends PersonAlgorithm {

	int routeCnt = 0;
	
	double avgRouteDist = 0;
	
	double avgFromToDist = 0;
	
	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			BasicPlanImpl.LegIterator it = plan.getIteratorLeg();
			while (it.hasNext()) {
				Route route = (Route)it.next().getRoute();
				if (route != null && route.getRoute().size() > 0) {
					avgRouteDist = (routeCnt * avgRouteDist + route.getDist()) /
						((double)routeCnt + 1);
					Node fromNode = route.getRoute().get(0);
					Node toNode = route.getRoute().get(route.getRoute().size() - 1);
					avgFromToDist = (routeCnt * avgFromToDist 
							+ fromNode.getCoord().calcDistance(toNode.getCoord())) /
						((double)routeCnt + 1);
					routeCnt++;
				}
			}
		}
	}
	
	public void printSummary() {
		System.out.println("Avg route distance of " + routeCnt + " routes: " + avgRouteDist);
		System.out.println("Avg from-to distance of " + routeCnt + " routes: " + avgFromToDist);
	}

}
