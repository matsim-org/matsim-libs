/* *********************************************************************** *
 * project: org.matsim.*
 * BestFitTimeRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;

import playground.msieg.structure.Commodity;

public class BestFitTimeRouter extends CMCFRouter {

	protected final int timeSteps;
	
	public BestFitTimeRouter(String networkFile, String plansFile,
			String cmcfFile) {
		this(networkFile, plansFile, cmcfFile, 1);
	}
	
	public BestFitTimeRouter(String networkFile, String plansFile,
			String cmcfFile, int steps) {
		super(networkFile, plansFile, cmcfFile);
		this.timeSteps = steps;
	}

	
	@Override
	public void route() {
		Set<Person> unroutedPersons = new HashSet<Person>(this.population.getPersons().values());
		
		Map<List<Link>, Double> flowValues = new HashMap<List<Link>, Double>();
		for(Commodity<Node> c: this.pathFlow.getCommodities()){
			for(List<Link> path: this.pathFlow.getFlowPaths(c))
				flowValues.put(path, 0.);
		}
		
		for(int i=0; i < this.timeSteps; i++){
			/**
			 * in each timestep we do the following:
			 * 
			 * take the CMCF solution and add the flowValues to the flowValues mapping
			 * 
			 * then route an unrouted person on a path with flowValue >= 1
			 *  	add routed person to the corresponding set, decrease flowValue by 1
			 */
			
			//add flow value
			for(Commodity<Node> c: this.pathFlow.getCommodities()){
				for(List<Link> path: this.pathFlow.getFlowPaths(c))
					flowValues.put(path, flowValues.get(path)+this.pathFlow.getFlowValue(c, path));
			}
			
			//look for a path with value >= 1
			for(List<Link> path: flowValues.keySet()){
				double flow = flowValues.get(path); 
				while (flow > 0.99) //0.99 because of rounding errors
				{	//look for unrouted person with the same start and target
					Person person = null;
					LegImpl leg = null;
					for(Person p: unroutedPersons){
						leg = ((PlanImpl) p.getSelectedPlan()).getNextLeg(((PlanImpl) p.getSelectedPlan()).getFirstActivity());
						Node from = leg.getRoute().getStartLink().getToNode(),
								to = leg.getRoute().getEndLink().getFromNode();
						if(path.get(0).getFromNode() == from
								&& path.get(path.size()-1).getToNode() == to){
							person = p;
							break;
						}
					}
					
					assert(person != null && leg != null);
					
					NodeNetworkRouteImpl route = new NodeNetworkRouteImpl(leg.getRoute().getStartLink(), leg.getRoute().getEndLink());
					route.setLinks(	leg.getRoute().getStartLink(), path, leg.getRoute().getEndLink());
					leg.setRoute(route);
					double depTime = ((PlanImpl) person.getSelectedPlan()).getFirstActivity().getStartTime()
									+ i * (((PlanImpl) person.getSelectedPlan()).getFirstActivity().getEndTime()
											- ((PlanImpl) person.getSelectedPlan()).getFirstActivity().getStartTime())
											/ this.timeSteps;
					leg.setDepartureTime(depTime);
					flowValues.put(path, --flow);
					unroutedPersons.remove(person);
				}
			}
		}
		/*double unrouted =0;
		for(List<Link> path: flowValues.keySet())
			{
				System.out.println("\tvalue="+flowValues.get(path));//+"\tpath="+path);
				unrouted += flowValues.get(path);
			}
		System.out.println(" Not routed flow value = "+unrouted);*/
		System.out.println(" Agents routed: "
				+(this.population.getPersons().size()-unroutedPersons.size())
				+"/"+this.population.getPersons().size());
	}
}

