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

import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NodeNetworkRoute;

import playground.msieg.structure.Commodity;

public class BestFitRouter extends CMCFRouter {

	public BestFitRouter(String networkFile, String plansFile, String cmcfFile) {
		super(networkFile, plansFile, cmcfFile);
	}

	
	@Override
	public	void route() {
		Population pop = this.population;
		//to keep tracking about the used paths, we need a new Map where we can change the flow values;
		Map<List<LinkImpl>, Double> flowValues = new HashMap<List<LinkImpl>, Double>();
		for(Commodity<NodeImpl> c: this.pathFlow.getCommodities()){
			for(List<LinkImpl> path: this.pathFlow.getFlowPaths(c))
				flowValues.put(path, this.pathFlow.getFlowValue(c, path));
		}
		int routedPersons = 0;
		for(PersonImpl p: pop.getPersons().values()){
			LegImpl leg = p.getSelectedPlan().getNextLeg(p.getSelectedPlan().getFirstActivity());
			NodeImpl 	from = leg.getRoute().getStartLink().getToNode(),
					to = leg.getRoute().getEndLink().getFromNode();
			// now search path for rerouting
			List<LinkImpl> path = null;
			Commodity<NodeImpl> com = this.pathFlow.getCommodity(from, to);
			if(com == null){
				System.out.println("Warning, no commodity in CMCF solution found from "+from+" to "+to+"!  Skipping person with id "+p.getId());
				continue;
			}
			assert(com != null);
			double max = 0;
			for(List<LinkImpl> pp: this.pathFlow.getFlowPaths(com)){
				double flow = flowValues.get(pp);
				if( (flow > max) && (flow > 0)){
					path = pp;
					max = flow; 
				}
			}
			//path is found, therefore reroute:
			assert(path != null);
			if (path != null) {
				NodeNetworkRoute route = new NodeNetworkRoute(leg.getRoute().getStartLink(), leg.getRoute().getEndLink());
				route.setLinks(	leg.getRoute().getStartLink(), path, leg.getRoute().getEndLink());
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

