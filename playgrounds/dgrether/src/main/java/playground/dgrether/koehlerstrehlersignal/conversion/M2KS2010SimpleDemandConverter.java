/* *********************************************************************** *
 * project: org.matsim.*
 * DgMatsim2KoehlerStrehler2010SimpleDemandConverter
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
package playground.dgrether.koehlerstrehlersignal.conversion;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;

/**
 * @author dgrether
 */
public class M2KS2010SimpleDemandConverter {

	
	private DgCommodities commodities;

	public DgCommodities convert(Scenario sc,  DgKSNetwork dgNetwork) {
		this.commodities = this.createCommodities(dgNetwork, sc.getPopulation());
		return this.commodities;
	}

	private DgCommodities createCommodities(DgKSNetwork net, Population population){
		DgCommodities coms = new DgCommodities();

		// the array contains the from node id, the to node id, the from link id and the to link id in this order
		// the Double contains the flow value of this origin destination pair
		Map<Id[], Double> fromNodeToNodeCountMap = new HashMap<Id[], Double>();
		
		for (Person p : population.getPersons().values()){
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					Leg leg = (Leg)pe;
					Id fromLinkId = leg.getRoute().getStartLinkId();
					DgStreet startStreet = net.getStreets().get(fromLinkId);
					Id fromNodeId = startStreet.getToNode().getId();
					Id toLinkId = leg.getRoute().getEndLinkId();
					DgStreet endStreet = net.getStreets().get(toLinkId);
					Id toNodeId = endStreet.getFromNode().getId();
					Id[] index = new Id[]{fromNodeId, toNodeId, fromLinkId, toLinkId};
					if (!fromNodeToNodeCountMap.containsKey(index)){
						fromNodeToNodeCountMap.put(index, 0.0);
					}
					double count = fromNodeToNodeCountMap.get(index);
					count = count + 1;
					fromNodeToNodeCountMap.put(index, count);
				}
			}
		}
		
		int comId = 0;
		for (Id[] index : fromNodeToNodeCountMap.keySet()){
			comId++;
			Id coId = new IdImpl(comId);
			DgCommodity co = new DgCommodity(coId);
			coms.addCommodity(co);
			co.setSourceNode(index[0], index[2], fromNodeToNodeCountMap.get(index));
			co.setDrainNode(index[1], index[3]);
		}
		
		return coms;
	}

	
	
}
