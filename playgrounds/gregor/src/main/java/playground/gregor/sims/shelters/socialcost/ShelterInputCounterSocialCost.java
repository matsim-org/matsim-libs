/* *********************************************************************** *
 * project: org.matsim.*
 * ShelterInputCounterSocialCost.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sims.shelters.socialcost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.evacuation.base.Building;
import org.matsim.population.algorithms.PlanAlgorithm;


public class ShelterInputCounterSocialCost implements LinkLeaveEventHandler, BeforeMobsimListener, AfterMobsimListener{

	
	private final HashMap<Id, Building> shelterLinkMapping;
	private final Map<Id,LinkInfo> infos = new HashMap<Id, LinkInfo>();
	private LeastCostPathCalculator router;
	private final Node saveNode;
	private int iteration = 0;

	public ShelterInputCounterSocialCost(Scenario sc, HashMap<Id,Building> shelterLinkMapping) {
		
		this.shelterLinkMapping = shelterLinkMapping;
		
		for (Link link : sc.getNetwork().getLinks().values()) {
			if (link.getId().toString().contains("sl") && link.getId().toString().contains("b")) {
				LinkInfo li = new LinkInfo();
				this.infos.put(link.getId(), li);
				li.maxCount = shelterLinkMapping.get(link.getId()).getShelterSpace();
				li.startNode = link.getFromNode().getInLinks().values().iterator().next().getFromNode();
			}
		}
	
		this.saveNode = sc.getNetwork().getNodes().get(new IdImpl("en1"));
		
	}
	
	
	public double getShelterTravelCost(Link link, double time) {
		LinkInfo li = this.infos.get(link.getId());
		if (li != null) {
			if (li.altArrTime > time) {
				return li.altArrTime - time;
			}
		}
		return 0;
	}



	public void notifyAfterMobsim(AfterMobsimEvent event) {
		PlanAlgorithm alg = event.getControler().createRoutingAlgorithm();
		if ( alg instanceof PlansCalcRoute) {
			this.router = ((PlansCalcRoute)alg).getLeastCostPathCalculator();
		} else {
			throw new RuntimeException("PlanAlgorithm should be an instance of PlansCalcRoute");
		}
		double n = this.iteration;
		for (LinkInfo li : this.infos.values()) {
			if (li.overcrowdingTime == -1) {
				
				li.altArrTime = (n / (1+n)) * li.altArrTime; 
				continue;
			}
			double tt = Math.min(30*3600,this.router.calcLeastCostPath(li.startNode, this.saveNode, li.overcrowdingTime).travelTime);
			li.altArrTime = (n / (1+n)) * li.altArrTime + (1/n)*(li.overcrowdingTime + tt);
			for (Tuple<Id,Double> agent : li.agents) {
				if (li.altArrTime > agent.getSecond()) {
					double cost = (li.altArrTime - agent.getSecond()) / -600.;
					AgentMoneyEventImpl e = new AgentMoneyEventImpl(30*3600,agent.getFirst(),cost);
					event.getControler().getEvents().processEvent(e);	
				}
			}
		}
		
	}
	
	
	public void handleEvent(LinkLeaveEvent event) {
		LinkInfo li = this.infos.get(event.getLinkId());
		if (li != null) {
			li.count++;
			if (li.count > li.maxCount && li.overcrowdingTime == -1) {
				li.overcrowdingTime = event.getTime();
			}
			Tuple<Id,Double> agent = new Tuple<Id, Double>(event.getPersonId(),event.getTime());
			li.agents.add(agent);
			
		}
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		for (LinkInfo li : this.infos.values()) {
			li.overcrowdingTime = -1;
			li.count = 0;
//			li.altArrTime = 0;
			li.agents.clear();
		}
		
	}
	public void reset(int iteration) {
		this.iteration  = iteration + 1;
		
	}

	private static class LinkInfo {
		double altArrTime = 0;
		int count = 0;
		int maxCount = 0;
		double overcrowdingTime = -1;
		Node startNode;
		List<Tuple<Id, Double>> agents = new ArrayList<Tuple<Id,Double>>();
	}







	
}
