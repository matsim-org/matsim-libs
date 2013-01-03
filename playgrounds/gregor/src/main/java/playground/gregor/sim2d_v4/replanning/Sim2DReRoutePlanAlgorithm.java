/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DReRoutePlanAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.replanning;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.gregor.sim2d_v4.scenario.TransportMode;

public class Sim2DReRoutePlanAlgorithm implements PlanAlgorithm {

	private final LeastCostPathCalculator router;
	private final Network network;
	private final LinkNetworkRouteFactory routeFac;

	public Sim2DReRoutePlanAlgorithm(Controler controller) {
		PreProcessDijkstra preProcData = new PreProcessDijkstra();
		preProcData.run(controller.getNetwork());
		
		TravelTime time = controller.getTravelTimeCalculator();
		TravelDisutility cost = controller.createTravelCostCalculator();
		if (time == null) {
			FreespeedTravelTimeAndDisutility fstt = new FreespeedTravelTimeAndDisutility(controller.getConfig().planCalcScore());
			time = fstt;
			cost = fstt;
		}
		
		this.router = new DijkstraFactory(preProcData).createPathCalculator(controller.getNetwork(), cost,time);
		try {
			IntermodalLeastCostPathCalculator tmp = (IntermodalLeastCostPathCalculator) this.router;
			tmp.setModeRestriction(TransportMode.transportModes);
		} catch (ClassCastException e) {
			throw new RuntimeException("Can not cast to Dijkstra to IntermodalLeastCostPathCalculator. This is strange, isn't it? The excpetion was: " + e);
		}
		this.network = controller.getNetwork();
		this.routeFac = new LinkNetworkRouteFactory();
	}

	@Override
	public void run(Plan plan) {
		
		List<Activity> acts = new ArrayList<Activity>();
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				acts.add((Activity) pe);
			}
		}
		List<PlanElement> newPlan = new ArrayList<PlanElement>();
		for (int i = 0; i < acts.size()-1; i++) {
			Activity o = acts.get(i);
			newPlan.add(o);
			Activity d = acts.get(i+1);
			handleTrip(o,d,newPlan,plan.getPerson());
		}
		newPlan.add(acts.get(acts.size()-1));
		plan.getPlanElements().clear();
		plan.getPlanElements().addAll(newPlan);
	}

	private void handleTrip(Activity o, Activity d, List<PlanElement> newPlan, Person person) {
		double startTime = o.getEndTime();
		Link lo = this.network.getLinks().get(o.getLinkId());
		Link ld = this.network.getLinks().get(d.getLinkId());
		Path path = this.router.calcLeastCostPath(lo.getToNode(), ld.getFromNode(), startTime, person, null);
		if (path == null) throw new RuntimeException("No route found from node " + lo.getToNode().getId() + " to node " + ld.getFromNode().getId() + ".");
		
		NetworkRoute baseRoute = (NetworkRoute)this.routeFac.createRoute(lo.getId(), ld.getId());
		baseRoute.setLinkIds(lo.getId(), NetworkUtils.getLinkIds(path.links), ld.getId());
		baseRoute.setTravelTime((int) path.travelTime);
		baseRoute.setTravelCost(path.travelCost);
		baseRoute.setDistance(RouteUtils.calcDistance(baseRoute, this.network));
		
		List<Link> fullPath = new ArrayList<Link>();
		fullPath.add(lo);
		fullPath.addAll(path.links);
		fullPath.add(ld);
		
		String beginLegMode;
		Set<String> currentAllowdModes = fullPath.get(0).getAllowedModes();
		if (currentAllowdModes.contains(TransportMode.walk2d)) {
			beginLegMode = TransportMode.walk2d;
		} else if (currentAllowdModes.contains(TransportMode.walk)){
			beginLegMode = TransportMode.walk;
		} else {
			throw new RuntimeException("No allowd mode has been found. One of " + TransportMode.transportModes + " is required. However, link" + path.links.get(0).getId() + " only supports " + currentAllowdModes);
		}
		String currentLegMode = beginLegMode;
		int io = 0;
		int id = 0;
		for (; id < fullPath.size(); id++) {
			Link l = fullPath.get(id);
//			if (switchLegMode(currentLegMode,l)) {
//				Leg leg = createLeg(fullPath, io, id, baseRoute, currentLegMode, startTime);
//				newPlan.add(leg);
//				io = id-1;
//				currentLegMode = currentLegMode.equals(TransportMode.walk) ? TransportMode.walk2d : TransportMode.walk;
//				
//			}
		}
		Leg leg = createLeg(fullPath, io, id, baseRoute, currentLegMode, startTime);
		newPlan.add(leg);
//		RouteUtils.c
//		r = RouteUtils.createNetworkRoute(null, network)
	}

	private Leg createLeg(List<Link> fullPath, int io, int id, NetworkRoute baseRoute, String legMode, double depTime) {
		Link start = fullPath.get(io);
		Link dest = fullPath.get(id-1);
		NetworkRoute subRoute = baseRoute.getSubRoute(start.getId(), dest.getId());
		
		LegImpl leg = new LegImpl(legMode);
		
		
//		DEBUG
		if (legMode.equals("walk")) {
			leg = new LegImpl("car");
		}
		
		leg.setDepartureTime(depTime);
		leg.setRoute(subRoute);
		return leg;
	}

	private boolean switchLegMode(String currentLegMode, Link l) {
		
		if (currentLegMode.equals(TransportMode.walk) && l.getAllowedModes().contains(TransportMode.walk2d)) {
			return true;
		} else if (currentLegMode.equals(TransportMode.walk2d) && !l.getAllowedModes().contains(currentLegMode)){
			return true;
		}
		return false;
	}

}
