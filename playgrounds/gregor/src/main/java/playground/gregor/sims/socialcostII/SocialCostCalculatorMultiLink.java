/* *********************************************************************** *
 * project: org.matsim.*
 * SocialCostCalculatorMultiLink.java
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
package playground.gregor.sims.socialcostII;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.IntegerCache;

import playground.gregor.sims.run.deprecated.MarginalCostControlerMultiLink;

public class SocialCostCalculatorMultiLink implements TravelCost,BeforeMobsimListener, SimulationBeforeCleanupListener, LinkEnterEventHandler, LinkLeaveEventHandler, AgentStuckEventHandler{

	private static final Logger  log = Logger.getLogger(SocialCostCalculatorMultiLink.class);
	
	private final Network network;
	private final int binSize;
	private final TravelTimeCalculator travelTimeCalculator;
	private final Population population;
	private EventsManager events ;
	
	private Integer maxK;
	private final int minK;
	
	Map<Id,LinkInfo> linkInfos = new HashMap<Id,LinkInfo>();
	Set<Id> stuckedAgents = new HashSet<Id>();
	private int it;

	private final double discount;
	

	private final static int MSA_OFFSET = 0;
	
	public SocialCostCalculatorMultiLink(Network network, int binSize, TravelTimeCalculator travelTimeCalculator, Population population, EventsManager events ) {
		this.network = network;
		this.binSize = binSize;
		this.minK = (int)(3 * 3600 / (double)binSize); //just a HACK needs to be fixed
		this.travelTimeCalculator = travelTimeCalculator;
		this.population = population;
		this.discount = MarginalCostControlerMultiLink.QUICKnDIRTY;
		this.events = events ;
	}
	
	public double getLinkTravelCost(Link link, double time) {
		LinkInfo li = this.linkInfos.get(link.getId());
		if (li == null) {
			return 0.;
		}
		
		LinkTimeCostInfo ltc = li.getLinkTimeCostInfo(getTimeBin(time));
		return Math.max(0,ltc.cost);
//		return 0.;
	}

	public void reset(int iteration) {
		this.it = iteration;
		
	}

	
	public void notifySimulationBeforeCleanup(
			SimulationBeforeCleanupEvent e) {
		
		recalculateSocialCosts();
//		this.linkInfos.clear();

	}

	private void recalculateSocialCosts() {
		for (Person pers : this.population.getPersons().values()) {
			if ( this.stuckedAgents.contains(pers.getId())) {
				continue;
			}
			Plan plan = pers.getSelectedPlan();
			List<Id> links = ((NetworkRoute) ((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity()).getRoute()).getLinkIds();
			traceAgentsRoute(links,pers.getId());
			
		}
		calcLinkTimeCosts();
		updateCosts();
		scorePlans();
		cleanUp();
	}

	private void cleanUp() {
		for (LinkInfo li : this.linkInfos.values()) {
			li.resetAgentEnterInfos();
		}
		
	}

	private void updateCosts() {
		double maxCost = 0;
		double minCost = Double.POSITIVE_INFINITY;
		double costSum = 0;
		int count = 0;
		if (this.it > MSA_OFFSET) {
			double n = this.it - MSA_OFFSET;
			double oldCoef = n / (n+1);
			double newCoef = 1 /(n+1);
			for (LinkInfo li : this.linkInfos.values()) {
				for (LinkTimeCostInfo lci : li.linkTimeCosts.values()) {
//					lci.cost = newCoef * Math.max(-1., Math.min(10.,lci.c1 + lci.c2)) + oldCoef * lci.cost;
					lci.cost = newCoef * (lci.c1 + lci.c2) + oldCoef * lci.cost;
					if (lci.cost > maxCost) {
						maxCost = lci.cost;
					}
					if (lci.cost < minCost) {
						minCost = lci.cost;
					}
					costSum += lci.cost;
					count++;
					lci.in = 0;
					lci.c1 = 0;
					lci.c2 = 0;
					lci.linkDelay = 0;
					lci.out = 0; 
				}
			}
		}else {
			for (LinkInfo li : this.linkInfos.values()) {
				for (LinkTimeCostInfo lci : li.linkTimeCosts.values()) {
					lci.cost = lci.c1 + lci.c2;
					if (lci.cost > maxCost) {
						maxCost = lci.cost;
					}
					if (lci.cost < minCost) {
						minCost = lci.cost;
					}
					costSum += lci.cost;
					count++;
					lci.in = 0;
					lci.c1 = 0;
					lci.c2 = 0;
					lci.linkDelay = 0;
					lci.out = 0;
				}
			}			
		}
		log.info("maxCost: " + maxCost + " minCost: " + minCost + " avg: " + costSum/count);
	}

	private void scorePlans() {
		for (Person pers : this.population.getPersons().values()) {
			if ( this.stuckedAgents.contains(pers.getId())) {
				continue;
			}
			Plan plan = pers.getSelectedPlan();
			List<Id> links = ((NetworkRoute) ((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity()).getRoute()).getLinkIds();
			double cost = 0;
			for (Id id : links) {
				LinkInfo li = this.linkInfos.get(id);
				Double enterTime = li.getAgentEnterTime(pers.getId());
				if (enterTime == null) {
					return;
				}
				cost += getLinkTravelCost(this.network.getLinks().get(id), li.getAgentEnterTime(pers.getId()));
			}
			AgentMoneyEventImpl e = new AgentMoneyEventImpl(this.maxK * this.binSize,pers.getId(),cost/-600);
//			QueueSimulation.getEvents().processEvent(e);
			events.processEvent(e);
		}
		
	}

	private void calcLinkTimeCosts() {
		for (Link link : this.network.getLinks().values()) {
			LinkInfo li = this.linkInfos.get(link.getId());
			if (li == null) { //Link has never been used by anny agent
				continue;
			}
			int kE = this.maxK;
			for (int k = this.maxK; k >= this.minK; k--) {
				Integer kInteger = IntegerCache.getInteger(k);
				
				
				double tauAk = this.travelTimeCalculator.getLinkTravelTime(link, k*this.binSize);
				double tauAFree = Math.ceil(((LinkImpl) link).getFreespeedTravelTime(k*this.binSize));
				if (tauAk <= tauAFree) {
					kE = k;
					continue;
				} 
				
				LinkTimeCostInfo ltc = li.getLinkTimeCostInfo(kInteger);
				double last = li.getLinkTimeCostInfo(IntegerCache.getInteger(k+1)).c2;
				double delay = li.getLinkTimeCostInfo(kInteger).linkDelay; 
				ltc.c2 =  last + delay;  
				ltc.c1 = Math.max(0, (kE-k)*this.binSize - tauAFree);
			}
			for (int k = 0; k <= this.maxK; k++) {
				Integer kInteger = IntegerCache.getInteger(k);
				LinkTimeCostInfo ltc = li.getLinkTimeCostInfo(kInteger);
				double tauAk = this.travelTimeCalculator.getLinkTravelTime(link, k*this.binSize);
				double tmp = tauAk + k * this.binSize;
				Integer slot = getTimeBin(tmp);
				LinkTimeCostInfo ltc2 = li.getLinkTimeCostInfo(slot);
				if (ltc2 != null && ltc2.out > 0){
					ltc.c2 /= ltc2.out; 
				}
			}
		}
	}

	private void traceAgentsRoute(List<Id> links,Id agentId) {
		double agentDelay = 0;
		for (int i = links.size()-1; i >= 0; i--) {
			Id linkId = links.get(i);
			Link link = this.network.getLinks().get(linkId);
			LinkInfo li = this.linkInfos.get(linkId); //Direct access
			Double enterTime = li.getAgentEnterTime(agentId);
			if (enterTime == null) {
				return;
			}
			
			Integer timeBin = getTimeBin(enterTime);
			LinkTimeCostInfo ltc = li.getLinkTimeCostInfo(timeBin);
			ltc.linkDelay += agentDelay;
			double tau = this.travelTimeCalculator.getLinkTravelTime(this.network.getLinks().get(linkId),enterTime);
			double tauAFree = Math.ceil(((LinkImpl) link).getFreespeedTravelTime(enterTime));
			
			if (tau > tauAFree){			
				Integer timeBin2 = getTimeBin(this.binSize*timeBin + tau);
				LinkTimeCostInfo ltc2 = li.getLinkTimeCostInfo(timeBin2);
				if (ltc2 != null && ltc2.out > 0){
					agentDelay = this.discount * agentDelay + ((double)ltc.in/this.binSize - (double)ltc.out/this.binSize) / ((double)ltc2.out/this.binSize);
				} 
			}
		}
		AgentMoneyEventImpl e = new AgentMoneyEventImpl(this.maxK * this.binSize,agentId,agentDelay/-600);
//		QueueSimulation.getEvents().processEvent(e);
		events.processEvent(e);
		
	}

	public void handleEvent(LinkEnterEvent event) {
		LinkInfo li = getLinkInfo(event.getLinkId());
		li.incrementInFlow(getTimeBin(event.getTime()));
		li.setAgentEnterTime(event.getPersonId(), event.getTime());
	}

	public void handleEvent(LinkLeaveEvent event) {
		this.maxK = getTimeBin(event.getTime());
		getLinkInfo(event.getLinkId()).incrementOutFlow(this.maxK);
	}

	public void handleEvent(AgentStuckEvent event) {
		this.stuckedAgents.add(event.getPersonId());
		
	}
	
	private LinkInfo getLinkInfo(Id id) {
		LinkInfo li = this.linkInfos.get(id);
		if (li == null) {
			li = new LinkInfo();
			this.linkInfos.put(id, li);
		}
		return li;
	}
	
	private Integer getTimeBin(double time) {
		int slice = ((int) time)/this.binSize;
		return IntegerCache.getInteger(slice);
	}
	
	private static class LinkInfo {
		HashMap<Integer,LinkTimeCostInfo> linkTimeCosts = new HashMap<Integer, LinkTimeCostInfo>(500);
		HashMap<Id,Double> agentEnterInfos = new HashMap<Id, Double>(500);
		
		public void incrementInFlow(Integer timeBin) {
			LinkTimeCostInfo ltc = this.linkTimeCosts.get(timeBin);
			if (ltc == null) {
				ltc = new LinkTimeCostInfo();
				this.linkTimeCosts.put(timeBin, ltc);
			}
			ltc.in++;
		}

		public void incrementOutFlow(Integer timeBin) {
			LinkTimeCostInfo ltc = this.linkTimeCosts.get(timeBin);
			if (ltc == null) {
				ltc = new LinkTimeCostInfo();
				this.linkTimeCosts.put(timeBin, ltc);
			}
			ltc.out++;
		}
		
//		public LinkTimeFlowInfo getLinkTimeFlowInfo(Integer timeBin) {
//			 LinkTimeFlowInfo ltf = this.flowInfos.get(timeBin);
//			 if (ltf == null) {
//				 ltf = new LinkTimeFlowInfo();
//				 this.flowInfos.put(timeBin, ltf);
//			 }
//			 return ltf; 
//		}
		public void setAgentEnterTime(Id agentId, double enterTime) {
			this.agentEnterInfos.put(agentId, enterTime);
		}
		
		public Double getAgentEnterTime(Id agentId) {
			return this.agentEnterInfos.get(agentId);
		}
		
		public void resetAgentEnterInfos() {
			this.agentEnterInfos.clear();
		}
		
		public LinkTimeCostInfo getLinkTimeCostInfo(Integer timeBin) {
			LinkTimeCostInfo ltc = this.linkTimeCosts.get(timeBin);
			if (ltc == null) {
				ltc = new LinkTimeCostInfo();
				this.linkTimeCosts.put(timeBin, ltc);
			}
			return ltc;
		}
		
	}

	private static class LinkTimeCostInfo {
		public double cost;
		double c2 = 0;
		double c1 = 0;
		double linkDelay = 0;
		public int out = 0;
		public int in = 0;
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		this.stuckedAgents.clear();
		this.linkInfos.clear();
		
	}


}
