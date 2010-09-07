/* *********************************************************************** *
 * project: org.matsim.*
 * SocialCostCalculatorMultiLinkII.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.IntegerCache;

public class SocialCostCalculatorMultiLinkII implements TravelCost, SimulationBeforeCleanupListener, BeforeMobsimListener, LinkEnterEventHandler, AgentStuckEventHandler{


	private final NetworkLayer network;
	private final int binSize;
	private final TravelTimeCalculator travelTimeCalculator;
	private final Population population;
	private final EventsManager events ;

	private Integer maxK;
	private final int minK;

	Map<Id,LinkInfo> linkInfos = new HashMap<Id,LinkInfo>();
	Set<Id> stuckedAgents = new HashSet<Id>();


	public SocialCostCalculatorMultiLinkII(NetworkLayer network, int binSize, TravelTimeCalculator travelTimeCalculator, Population population, EventsManager events ) {
		this.network = network;
		this.binSize = binSize;
		this.minK = (int)(3 * 3600 / (double)binSize); //just a HACK needs to be fixed
		this.travelTimeCalculator = travelTimeCalculator;
		this.population = population;
		this.events = events ;
	}

	public double getLinkTravelCost(Link link, double time) {
		LinkInfo li = this.linkInfos.get(link.getId());
		if (li == null) {
			return 0.;
		}
		HashMap<Integer,LinkTimeCostInfo> ltcs = li.getLinkTimeCostInfos();
		LinkTimeCostInfo ltc = ltcs.get(getTimeBin(time));
		if (ltc == null) {
			return 0.;
		}

		return ltc.cost;
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}


	public void notifySimulationBeforeCleanup(
			SimulationBeforeCleanupEvent e) {
		recalculateSocialCosts();

	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		System.out.println("cleanup");
		this.stuckedAgents.clear();
		for (LinkInfo li : this.linkInfos.values()) {
			li.cleanUp();
		}
	}

	private void recalculateSocialCosts() {
		calcLinkTimeCosts();
		for (Person pers : this.population.getPersons().values()) {
			if ( this.stuckedAgents.contains(pers.getId())) {
				continue;
			}
			Plan plan = pers.getSelectedPlan();
			List<Id> links = ((NetworkRoute) ((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity()).getRoute()).getLinkIds();
			traceAgentsRoute(links,pers.getId());

		}

//		scorePlans();
	}

//	private void scorePlans() {
//		for (Person pers : this.population.getPersons().values()) {
//			if ( this.stuckedAgents.contains(pers.getId())) {
//				continue;
//			}
//			Plan plan = pers.getSelectedPlan();
//			List<Id> links = plan.getNextLeg(plan.getFirstActivity()).getRoute().getLinkIds();
//			double cost = 0;
//			for (Id id : links) {
//				LinkInfo li = this.linkInfos.get(id);
//				LinkTimeCostInfo lct = li.getLinkTimeCostInfo(getTimeBin(li.getAgentEnterTime(pers.getId())));
//				cost += lct.c1;
//			}
//			AgentMoneyEvent e = new AgentMoneyEvent(this.maxK * this.binSize,pers.getId(),cost/-600);
//			QueueSimulation.getEvents().processEvent(e);
//		}
//
//	}

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
				double tauAFree = Math.ceil(((LinkImpl) link).getFreespeedTravelTime(k*this.binSize))+1;
				if (tauAk <= tauAFree) {
					kE = k;
					continue;
				}
				LinkTimeCostInfo ltc = li.getLinkTimeCostInfo(kInteger);
				ltc.linkDelay = Math.max(0, (kE-k)*this.binSize - tauAFree);
			}
		}
	}

	private void traceAgentsRoute(List<Id> links,Id agentId) {
		double agentDelay = 0;
		for (int i = links.size()-1; i >= 0; i--) {
			Id linkId = links.get(i);
			LinkInfo li = this.linkInfos.get(linkId); //Direct access
			double enterTime = li.getAgentEnterTime(agentId);
			Integer timeBin = getTimeBin(enterTime);
			LinkTimeCostInfo ltc = li.getLinkTimeCostInfo(timeBin);
			agentDelay += ltc.linkDelay;
			ltc.cost += agentDelay / ltc.in;


		}
		AgentMoneyEventImpl e = new AgentMoneyEventImpl(this.maxK * this.binSize,agentId,agentDelay/-600);
//		QueueSimulation.getEvents().processEvent(e);
		events.processEvent(e);

	}

	public void handleEvent(LinkEnterEvent event) {
		LinkInfo li = getLinkInfo(event.getLinkId());
		li.incrementInFlow(getTimeBin(event.getTime()));
		li.setAgentEnterTime(event.getPersonId(), event.getTime());

		this.maxK = getTimeBin(event.getTime()) + 1; //FIXME!!
	}

//	public void handleEvent(LinkLeaveEvent event) {
//		this.maxK = getTimeBin(event.getTime());
//		getLinkInfo(event.getLinkId()).incrementOutFlow(this.maxK);
//	}

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
		HashMap<Integer,LinkTimeCostInfo> linkTimeCosts = new HashMap<Integer, LinkTimeCostInfo>();
		HashMap<Id,Double> agentEnterInfos = new HashMap<Id, Double>();

		public void incrementInFlow(Integer timeBin) {
			LinkTimeCostInfo ltc = this.linkTimeCosts.get(timeBin);
			if (ltc == null) {
				ltc = new LinkTimeCostInfo();
				this.linkTimeCosts.put(timeBin, ltc);
			}
			ltc.in++;
		}

//		public void incrementOutFlow(Integer timeBin) {
//			LinkTimeCostInfo ltc = this.linkTimeCosts.get(timeBin);
//			if (ltc == null) {
//				ltc = new LinkTimeCostInfo();
//				this.linkTimeCosts.put(timeBin, ltc);
//			}
//			ltc.out++;
//		}

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

		public double getAgentEnterTime(Id agentId) {
			return this.agentEnterInfos.get(agentId);
		}

		public LinkTimeCostInfo getLinkTimeCostInfo(Integer timeBin) {
			LinkTimeCostInfo ltc = this.linkTimeCosts.get(timeBin);
			if (ltc == null) {
				ltc = new LinkTimeCostInfo();
				this.linkTimeCosts.put(timeBin, ltc);
			}
			return ltc;
		}

		public HashMap<Integer,LinkTimeCostInfo> getLinkTimeCostInfos() {
			return this.linkTimeCosts;
		}

		public void cleanUp() {
			this.agentEnterInfos.clear();
			this.linkTimeCosts.clear();
		}
	}

	private static class LinkTimeCostInfo {
//		double c2 = 0;
//		double c1 = 0;
		double cost = 0;
		double linkDelay = 0;
//		public int out = 0;
		public int in = 0;
	}








}
