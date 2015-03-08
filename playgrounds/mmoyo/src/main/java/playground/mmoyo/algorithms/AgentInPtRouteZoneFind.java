/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mmoyo.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.analysis.filters.population.AbstractPersonFilter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.utils.DataLoader;

public class AgentInPtRouteZoneFind extends AbstractPersonFilter {
	private NetworkImpl ptNet;
	private List <Node> nodeList;
	
	public AgentInPtRouteZoneFind(List <Node>  nodeList, NetworkImpl ptNet){
		this.nodeList = nodeList;
		this.ptNet = ptNet;
	}
	
	@Override
	public boolean judge(final Person person) {
		boolean found = false;
		Activity act;
		Activity lastAct= null;
		int i=0;
		do{
			PlanElement pe = person.getSelectedPlan().getPlanElements().get(i++);
			if (pe instanceof Activity) {
				act = (Activity)pe; 
				if (lastAct!=null){

					Coord center = CoordUtils.getCenter(lastAct.getCoord(), act.getCoord());
					double radius = CoordUtils.calcDistance(center, act.getCoord());
					Collection<Node> stopsinBetween = this.ptNet.getNearestNodes(center, radius + 2000);
					int j= 0;
					do {
						Node node = this.nodeList.get(j++);
						found = stopsinBetween.contains(node) || found;
					} while (j< this.nodeList.size() && found == false);
				}
				lastAct = act; 
			}
		}while(i< person.getSelectedPlan().getPlanElements().size() && found == false);
		return found;
	}

	public static void main(String[] args) {
		String netFilePath;
		String popFilePath;
		
		if (args.length>0){
			popFilePath = args[0];
			netFilePath = args[1];
			
		}else{
			popFilePath = "";
			netFilePath = "";
		}

		DataLoader dataLoader = new DataLoader ();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(netFilePath);  
		
		List<Node> nodeList = new ArrayList<Node>();
		nodeList.add(scn.getNetwork().getNodes().get(Id.create("10000000", Node.class)));
		nodeList.add(scn.getNetwork().getNodes().get(Id.create("10000001", Node.class)));
		nodeList.add(scn.getNetwork().getNodes().get(Id.create("8400060", Node.class)));
		nodeList.add(scn.getNetwork().getNodes().get(Id.create("8338", Node.class)));
		nodeList.add(scn.getNetwork().getNodes().get(Id.create("7782", Node.class)));
		
		//AgentInPtRouteZoneFind agentInPtRouteZoneFind = new AgentInPtRouteZoneFind(nodeList, private );
		//PopSecReader popSecReader = new PopSecReader (scn, agentInPtRouteZoneFind);
		//popSecReader.readFile(popFilePath);
	}
	
}
