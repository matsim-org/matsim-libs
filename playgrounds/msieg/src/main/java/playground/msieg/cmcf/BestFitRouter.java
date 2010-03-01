/* *********************************************************************** *
 * project: org.matsim.*
 * BestFitRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.msieg.cmcf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.misc.NetworkUtils;

import playground.msieg.structure.Commodity;

public class BestFitRouter extends CMCFRouter {

	public BestFitRouter(String networkFile, String plansFile, String cmcfFile) {
		super(networkFile, plansFile, cmcfFile);
	}


	@Override
	public	void route() {
		PopulationImpl pop = this.population;
		//to keep tracking about the used paths, we need a new Map where we can change the flow values;
		Map<List<Link>, Double> flowValues = new HashMap<List<Link>, Double>();
		for(Commodity<Node> c: this.pathFlow.getCommodities()){
			for(List<Link> path: this.pathFlow.getFlowPaths(c))
				flowValues.put(path, this.pathFlow.getFlowValue(c, path));
		}
		int routedPersons = 0;
		for(Person p: pop.getPersons().values()){
			LegImpl leg = ((PlanImpl) p.getSelectedPlan()).getNextLeg(((PlanImpl) p.getSelectedPlan()).getFirstActivity());
			Node from = network.getLinks().get(leg.getRoute().getStartLinkId()).getToNode(),
					to = network.getLinks().get(leg.getRoute().getEndLinkId()).getFromNode();
			// now search path for rerouting
			List<Link> path = null;
			Commodity<Node> com = this.pathFlow.getCommodity(from, to);
			if(com == null){
				System.out.println("Warning, no commodity in CMCF solution found from "+from+" to "+to+"!  Skipping person with id "+p.getId());
				continue;
			}
			assert(com != null);
			double max = 0;
			for(List<Link> pp: this.pathFlow.getFlowPaths(com)){
				double flow = flowValues.get(pp);
				if( (flow > max) && (flow > 0)){
					path = pp;
					max = flow;
				}
			}
			//path is found, therefore reroute:
			assert(path != null);
			if (path != null) {
				LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId());
				route.setLinkIds(	leg.getRoute().getStartLinkId(), NetworkUtils.getLinkIds(path), leg.getRoute().getEndLinkId());
				leg.setRoute(route);
				flowValues.put(path, flowValues.get(path) < 1 ? 0 : flowValues.get(path)-1);
				routedPersons++;
			}
			else {
				System.out.println("Path is null for com " + com + "! Skipping person with id " + p.getId());
			}
		}
		System.out.println(" Agents routed: "+routedPersons+"/"+pop.getPersons().size());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length < 3){
			System.out.println("usage: java CMCFRouter network.xml plans.xml flow.cmcf [outputFile]");
			System.exit(1);
		}
		CMCFRouter router = new BestFitRouter(args[0], args[1], args[2]);
		router.loadEverything();
		router.route();
		if(args.length>3)
			router.writePlans(args[3]);
		else
			System.out.println(router.population.toString());
	}

}

