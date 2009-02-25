/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcRandomViaRoute.java
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

package playground.balmermi.algos;

import java.util.ArrayList;
import java.util.List;

import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.router.Dijkstra;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.router.util.LeastCostPathCalculator.Path;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

public class PlansCalcRandomViaRoute extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final NetworkLayer network;
	protected final LeastCostPathCalculator routeAlgo;
	protected final LeastCostPathCalculator dijkstraEmpty;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCalcRandomViaRoute(NetworkLayer network, TravelCost costCalculator, TravelTime timeCalculator) {
		this.network = network;
		this.routeAlgo = new Dijkstra(network, costCalculator, timeCalculator);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		this.dijkstraEmpty = new Dijkstra(network, timeCostCalc, timeCostCalc);
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			handlePlan(plan);
		}
	}

	public void run(Plan plan) {
		handlePlan(plan);
	}

	//////////////////////////////////////////////////////////////////////
	// helper methods
	//////////////////////////////////////////////////////////////////////

	// changed the method from public to private. use run(plan) instead
	protected void handlePlan(Plan plan) {
		ArrayList<?> actslegs = plan.getActsLegs();
		Act fromAct = (Act)actslegs.get(0);
		double travTime = 0;
		double now = fromAct.getEndTime(); // must be available according spec

		// loop over all <act>s
		for (int j = 2; j < actslegs.size(); j=j+2) {
			Act toAct = (Act)actslegs.get(j);
			Leg leg = (Leg)actslegs.get(j-1);

			travTime = handleLeg(leg, fromAct, toAct, now);

			now += travTime;
			// TODO balmermi: setting toAct start time to leg arrival time
			//                please check if you are happy with that
			toAct.setStartTime(now);

			double endTime = toAct.getEndTime();
			double dur = toAct.getDuration();
			if (endTime != Time.UNDEFINED_TIME && dur != Time.UNDEFINED_TIME) {
				double min = Math.min(endTime, now + dur);
				if (now < min) {
					now = min;
				}
			} else if (endTime != Time.UNDEFINED_TIME) {
				if (now < endTime) {
					now = endTime;
				}
			} else if (dur != Time.UNDEFINED_TIME) {
				now += dur;
			} else if ((j+1) != actslegs.size()) {
				// if it's the last act on the plan, we don't care, otherwise exception
				throw new RuntimeException("act " + j + " has neither end-time nor duration.");
			}
			// TODO balmermi: updating duration and end time of toAct according to the
			//                calculated 'now' time (which is the new end time of the toAct)
			//                please check if you are happy with that
			if ((j+1) != actslegs.size()) {
				toAct.setEndTime(now);
				toAct.setDuration(now-toAct.getStartTime());
			}
			else {
				// balmermi: remove durations and endtimes for the last act
				toAct.setDuration(Time.UNDEFINED_TIME);
				toAct.setEndTime(Time.UNDEFINED_TIME);
			}
			
			fromAct = toAct;
		}
	}

	protected double handleLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		String legmode = leg.getMode().toString();

		if (legmode == "car") {
			return handleCarLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == "ride") {
			return handleRideLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == "pt") {
			return handlePtLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == "walk") {
			return handleWalkLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == "bike") {
			return handleBikeLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == "undef") {
			// TODO balmermi: No clue how to handle legs with 'undef' mode
			//                Therefore, handle it similar like bike mode with 50 km/h
			//                and no route assigned
			return handleUndefLeg(leg, fromAct, toAct, depTime);
		} else {
			throw new RuntimeException("cannot handle legmode '" + legmode + "'.");
		}
	}

	private double handleCarLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		double travTime = 0;
		Link fromLink = fromAct.getLink();
		Link toLink = toAct.getLink();
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link
		
		// getting a random via node
		double x = 0.5*(startNode.getCoord().getX() + endNode.getCoord().getX());
		double y = 0.5*(startNode.getCoord().getY() + endNode.getCoord().getY());
		double dist = endNode.getCoord().calcDistance(startNode.getCoord());
		if (dist < 1000.0) { dist = 1000.0; } 
		x = x + (MatsimRandom.random.nextDouble()-0.5)*dist;
		y = y + (MatsimRandom.random.nextDouble()-0.5)*dist;
		CoordImpl coord = new CoordImpl(x,y);
		Node viaNode = this.network.getNearestNode(coord);
		while ((viaNode == startNode) || (viaNode == endNode)) {
			dist = dist + 1000.0;
			x = x + (MatsimRandom.random.nextDouble()-0.5)*dist;
			y = y + (MatsimRandom.random.nextDouble()-0.5)*dist;
			coord = new CoordImpl(x,y);
			viaNode = this.network.getNearestNode(coord);
		}

		// calc first part of the route
		Path path1 = null;
		if (startNode != viaNode) {
			path1 = this.routeAlgo.calcLeastCostPath(startNode,viaNode,depTime);
			if (path1 == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + viaNode.getId() + ".");
			travTime = path1.travelTime;
		}

		// calc second part of the route
		Path path2 = null;
		if (viaNode != endNode) {
			path2 = this.routeAlgo.calcLeastCostPath(viaNode,endNode,depTime+travTime);
			if (path2 == null) throw new RuntimeException("No route found from node " + viaNode.getId() + " to node " + endNode.getId() + ".");
			travTime += path2.travelTime;
		}
		
		
		List<Node> nodes = (path1 != null ? path1.nodes : new ArrayList<Node>());
		if (!nodes.isEmpty()) { nodes.remove(nodes.size()-1); } // remove the via node
		if (path2 != null) {
			nodes.addAll(path2.nodes);
		}
		CarRoute route = new NodeCarRoute();
		route.setStartLink(fromLink);
		route.setEndLink(toLink);
		route.setNodes(nodes);
		leg.setRoute(route);
		
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}

	private double handleRideLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		// handle a ride exactly the same was as a car
		// the simulation has to take car that this leg is not really simulated as a stand-alone driver
		return handleCarLeg(leg, fromAct, toAct, depTime);
	}

	private double handlePtLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		// currently: exactly the same as for Car Leg, because i'm just too lazy...
		return handleCarLeg(leg, fromAct, toAct, depTime);
	}

	private double handleWalkLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		// make simple assumption about distance and walking speed
		double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
		double speed = 3.0 / 3.6; // 3.0 km/h --> m/s
//	 create an empty route, but with realistic traveltime
		CarRoute route = new NodeCarRoute();
		int travTime = (int)(dist / speed);
		route.setTravelTime(travTime);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}

	private double handleBikeLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		// make simple assumption about distance and cycling speed
		double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
		double speed = 15.0 / 3.6; // 15.0 km/h --> m/s
//	 create an empty route, but with realistic traveltime
		CarRoute route = new NodeCarRoute();
		int travTime = (int)(dist / speed);
		route.setTravelTime(travTime);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}

	private double handleUndefLeg(Leg leg, Act fromAct, Act toAct, double depTime) {
		// make simple assumption about distance and a dummy speed (50 km/h)
		double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
		double speed = 50.0 / 3.6; // 50.0 km/h --> m/s
//	 create an empty route, but with realistic traveltime
		CarRoute route = new NodeCarRoute();
		int travTime = (int)(dist / speed);
		route.setTravelTime(travTime);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}

}
