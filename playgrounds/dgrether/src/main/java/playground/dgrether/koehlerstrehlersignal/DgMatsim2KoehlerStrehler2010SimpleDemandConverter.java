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
package playground.dgrether.koehlerstrehlersignal;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgNetwork;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;

/**
 * @author dgrether
 */
public class DgMatsim2KoehlerStrehler2010SimpleDemandConverter implements DgMatsim2KoehlerStrehler2010DemandConverter {

	
	private DgCommodities commodities;

	@Override
	public DgCommodities convert(ScenarioImpl sc,  DgNetwork dgNetwork) {
		this.commodities = this.createCommodities(dgNetwork, sc.getPopulation());
		return this.commodities;
	}

	private DgCommodities createCommodities(DgNetwork net, Population population){
		DgCommodities coms = new DgCommodities();

		Map<Tuple<Id, Id>, Double> fromNodeToNodeCountMap = new HashMap<Tuple<Id, Id>, Double>();
		
		for (Person p : population.getPersons().values()){
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					Leg leg = (Leg)pe;
					DgStreet startStreet = net.getStreets().get(leg.getRoute().getStartLinkId());
					Id fromNodeId = startStreet.getToNode().getId();
					DgStreet endStreet = net.getStreets().get(leg.getRoute().getEndLinkId());
					Id toNodeId = endStreet.getFromNode().getId();
					Tuple<Id, Id> index = new Tuple<Id, Id>(fromNodeId, toNodeId);
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
		for (Tuple<Id, Id> index : fromNodeToNodeCountMap.keySet()){
			comId++;
			Id coId = new IdImpl(comId);
			DgCommodity co = new DgCommodity(coId);
			coms.addCommodity(co);
			co.addSourceNode(index.getFirst(), fromNodeToNodeCountMap.get(index));
			co.addDrainNode(index.getSecond());
		}
		
		return coms;
	}

	
	
}
