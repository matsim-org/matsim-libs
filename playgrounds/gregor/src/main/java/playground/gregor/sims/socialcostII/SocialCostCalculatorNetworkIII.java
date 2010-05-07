/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeAndSocialCostCalculatorHeadMultiLink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.utils.misc.IntegerCache;

public class SocialCostCalculatorNetworkIII implements TravelCost, IterationStartsListener,  AgentDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler{

	private final int travelTimeBinSize;
	private final int numSlots;
	private final NetworkLayer network;
	private final EventsManager events ;


	private final HashMap<String,AgentInfo> agentInfos = new HashMap<String, AgentInfo>();
	private final HashMap<String,LinkInfo> linkInfos = new HashMap<String, LinkInfo>();
	private final HashMap<String,SocialCostRole> socCosts = new HashMap<String, SocialCostRole>();

	private final HashMap<String,HashMap<Integer,LinkCongestionInfo>> linkCongestion = new HashMap<String, HashMap<Integer,LinkCongestionInfo>>();

	private final static int MSA_OFFSET = 20;
	private final double storageCapFactor;

	static double oldCoef = 0;
	static double newCoef = 1;
	static int iteration = 0;

	public SocialCostCalculatorNetworkIII(final NetworkLayer network, final double storageCapFactor, final EventsManager events ) {
		this(network, 15*60, 30*3600, storageCapFactor, events);	// default timeslot-duration: 15 minutes
	}

	public SocialCostCalculatorNetworkIII(final NetworkLayer network, final int timeslice, final double storageCapFactor, final EventsManager events) {
		this(network, timeslice, 30*3600, storageCapFactor, events ); // default: 30 hours at most
	}

	public SocialCostCalculatorNetworkIII(final NetworkLayer network, final int timeslice,	final int maxTime, final double storageCapFactor, final EventsManager events ) {
		this.travelTimeBinSize = timeslice;
		this.numSlots = (maxTime / this.travelTimeBinSize) + 1;
		this.network = network;
		this.storageCapFactor = storageCapFactor;
		this.events = events ;
	}

	public double getLinkTravelCost(final Link link, final double time) {
		SocialCostRole sc = this.socCosts.get(link.getId().toString());
		if (sc == null) {
			return 0;
		}
		return sc.getSocCost(getTimeSlotIndex(time));
	}


	public void notifyIterationStarts(final IterationStartsEvent event) {
		iteration = event.getIteration();
		this.linkInfos.clear();
		this.agentInfos.clear();
		updateSocCosts();
//		this.socCosts.clear();
		if (event.getIteration() > MSA_OFFSET) {
			double n = event.getIteration() - MSA_OFFSET;
			oldCoef = n / (n+1);
			newCoef = 1 /(n+1);
		}

	}

	private void updateSocCosts() {
		for (SocialCostRole scr : this.socCosts.values()) {
			scr.update();
		}
	}


	public void handleEvent(final LinkEnterEvent event) {

		LinkInfo info = getLinkInfo(event.getLinkId().toString());
		AgentInfo ai = getAgentInfo(event.getPersonId().toString());

		if (ai.stucked) {
			LinkInfo oldInfo = this.linkInfos.get(ai.currentLink);
			AgentCongestionInfo aci = new AgentCongestionInfo();
			aci.agentId = event.getPersonId().toString();
			aci.timeSlot = getTimeSlotIndex(ai.enterTime);
			oldInfo.agentsLeftLink.add(aci);
			oldInfo.incrCongestionInfo(event.getLinkId().toString(), aci.timeSlot, info.congested);


//			if (info.congested) { //TODO this probably wrong because if this link gets uncongested in the samt ttbin than con counter overestimates the #veh
//				oldInfo.agentsLeftToCongestedLink.add(aci);
//			} else {
//				oldInfo.agentsLeftToUncongestedLink.add(aci);
//			}
		}
		ai.currentLink = event.getLinkId().toString();
		ai.enterTime = event.getTime();
		ai.id = event.getPersonId().toString();
//		info.agentsOnLink++;
	}



	public void handleEvent(final AgentDepartureEvent event) {
		LinkInfo info = getLinkInfo(event.getLinkId().toString());
		AgentInfo ai = getAgentInfo(event.getPersonId().toString());
		ai.enterTime = event.getTime();
		ai.id = event.getPersonId().toString();
		ai.currentLink = event.getLinkId().toString();
//		info.agentsOnLink++;
	}

	public void handleEvent(final LinkLeaveEvent event) {

		LinkInfo info = getLinkInfo(event.getLinkId().toString());
		AgentInfo ai = getAgentInfo(event.getPersonId().toString());

		if ((event.getTime() - ai.enterTime) <= info.t_free){
			ai.stucked = false;

			// optimization
			if (info.congested) {
				clacSocCost(info,info.lastFSTime,ai.enterTime);
			} else if (info.agentsLeftLink.size() != 0 ) {
				throw new RuntimeException("something went wrong!");
			}
			info.lastFSTime = ai.enterTime;
			handleLinkCongestionInformation(event.getLinkId().toString(),false,getTimeSlotIndex(event.getTime()));
		} else {
			ai.stucked = true;
			info.congested = true;// optimization
			handleLinkCongestionInformation(event.getLinkId().toString(),true,getTimeSlotIndex(event.getTime()));
		}

	}

	private void handleLinkCongestionInformation(final String linkId, final boolean b,
			final int timeSlotIndex) {
		HashMap<Integer,LinkCongestionInfo> cm = this.linkCongestion.get(linkId);
		if (cm == null) {
			cm = new HashMap<Integer, LinkCongestionInfo>();
			this.linkCongestion.put(linkId, cm);
		}

		LinkCongestionInfo lci = cm.get(IntegerCache.getInteger(timeSlotIndex));
		if (lci == null) {
			lci = new LinkCongestionInfo();
			cm.put(IntegerCache.getInteger(timeSlotIndex), lci);
		}
		if (!b) {
			lci.congested = false;
		}

	}


	private boolean isLinkCongested(final int slot, final String linkId) {
		HashMap<Integer,LinkCongestionInfo> cm = this.linkCongestion.get(linkId);
		if (cm == null) {
			return false;
		}
		LinkCongestionInfo lci = cm.get(IntegerCache.getInteger(slot));
		if (lci == null) {
			return false;
		}

		return lci.congested;
	}

	private void clacSocCost(final LinkInfo info, final double lastTimeUncongested, final double currentTimeUncongested) {


		int lB = getTimeSlotIndex(lastTimeUncongested)+1;
		int uB = getTimeSlotIndex(currentTimeUncongested)-1;

		if (uB <= lB || info.agentsLeftLink.size() == 0) {
			info.cleanUpCongestionInfo();
			info.congested = false; // optimization
			return;
		}

		SocialCostRole sc = this.socCosts.get(info.id);
		if (sc == null) {
			sc = new SocialCostRole();
			this.socCosts.put(info.id, sc);
		}

		for (int i = lB; i<= uB; i++){

			double cost = 0;
			double baseCost = this.travelTimeBinSize * (uB - i + 1)  - info.t_free;
			int n = 0; //#veh left current link until congestion dissolves
			for (int j = i; j <= uB; j++) {
				HashMap<String,LinkCongestionInfo> lcis = info.getConInfo(IntegerCache.getInteger(j));
				for (LinkCongestionInfo lci : lcis.values() ) {
					n += lci.agents;
				}
			}

			if (n == 0) {
				return;
			}

			HashMap<String,Integer> tmp = new HashMap<String, Integer>();
			for (int j = i; j <= uB; j++) {
				HashMap<String,LinkCongestionInfo> lcis = info.getConInfo(IntegerCache.getInteger(j));
				for (Entry<String,LinkCongestionInfo> e : lcis.entrySet() ) {
					Integer old = tmp.get(e.getKey());
					if (old == null) {
						old = 0;
					}
					tmp.put(e.getKey(), IntegerCache.getInteger(old + e.getValue().agents));
				}
			}


			for (Entry<String,Integer> e : tmp.entrySet()) {
				double p = (double) e.getValue() / n;
				int con = 0;
				int notCon = 0;
				int sum = 0;
				for (int j = i; j <= uB; j++) {
					if (isLinkCongested(j, e.getKey())) {
						con++;
					} else {
						notCon++;
					}
					sum++;
				}
				double c = ((double)con/sum);
				double nc = ((double)notCon/sum);
				cost += c * (1.0 - p) * baseCost * p + nc * baseCost * p;

			}


			sc.setSocCost(i, Math.max(0,cost));
			double scoringCost =  sc.getSocCost(i) / -600;
			while (info.agentsLeftLink.size() > 0 && info.agentsLeftLink.peek().timeSlot <= i) {
				AgentCongestionInfo aci = info.agentsLeftLink.poll();
				if (aci.timeSlot == i) {
					Id id = new IdImpl(aci.agentId);
					AgentMoneyEventImpl e = new AgentMoneyEventImpl(currentTimeUncongested,id,scoringCost);
//					QueueSimulation.getEvents().processEvent(e);
					events.processEvent(e);
				}
			}

		}



		info.cleanUpCongestionInfo();
		info.congested = false; // optimization

	}

	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.travelTimeBinSize;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}


	private LinkInfo getLinkInfo(final String id) {
		LinkInfo ret = this.linkInfos.get(id);
		if (ret == null) {
			ret = new LinkInfo();
			ret.t_free = Math.ceil(((LinkImpl) this.network.getLinks().get(new IdImpl(id))).getFreespeedTravelTime()); //TODO make this dynamic, since we have time variant networks
			Link link = this.network.getLinks().get(new IdImpl(id));
			ret.storageCap = calcCapacity(link);
			ret.id = id;
			this.linkInfos.put(id, ret);
		}
		return ret;
	}

	private AgentInfo getAgentInfo(final String agentId) {
		AgentInfo ai = this.agentInfos.get(agentId);
		if (ai == null) {
			ai = new AgentInfo();
			ai.stucked = false;
			this.agentInfos.put(agentId, ai);
		}
		return ai;
	}


	private int calcCapacity(final Link link) {
		// network.capperiod is in hours, we need it per sim-tick and multiplied with flowCapFactor

//		this.inverseSimulatedFlowCapacity = 1.0 / this.simulatedFlowCapacity;
//		this.bufferStorageCapacity = (int) Math.ceil(this.simulatedFlowCapacity);
//		this.flowCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;

		// first guess at storageCapacity:
		double storageCapacity = (link.getLength() * link.getNumberOfLanes())
				/ ((NetworkLayer) link.getLayer()).getEffectiveCellSize() * storageCapFactor;

		return (int) Math.max(Math.floor(storageCapacity),1);
	}


	private static class LinkInfo {


		public double lastFSTime = 0;

		public String id;

		public boolean congested = false;// optimization

		public int storageCap;


		private final Map<Integer,HashMap<String,LinkCongestionInfo>> conInfo = new HashMap<Integer, HashMap<String,LinkCongestionInfo>>();
		private final ConcurrentLinkedQueue<AgentCongestionInfo> agentsLeftLink = new ConcurrentLinkedQueue<AgentCongestionInfo>();

		double t_free;

		public void incrCongestionInfo(final String linkId,final Integer slot, final boolean congested) {
			HashMap<String,LinkCongestionInfo> info = getConInfo(slot);
			LinkCongestionInfo lci = info.get(linkId);
			if (lci == null) {
				lci = new LinkCongestionInfo();
				info.put(linkId, lci);
			}
			lci.agents++;
			if (!congested) {
				lci.congested = false;
			}
		}

		public HashMap<String,LinkCongestionInfo> getConInfo(final Integer slot) {
			HashMap<String,LinkCongestionInfo> ret = this.conInfo.get(slot);
			if (ret == null) {
				ret = new HashMap<String, LinkCongestionInfo>();
				this.conInfo.put(slot, ret);
			}
			return ret;
		}


		public void cleanUpCongestionInfo(){
//			this.agentsLeftToCongestedLink.clear();
//			this.agentsLeftToUncongestedLink.clear();
			this.agentsLeftLink.clear();
			this.conInfo.clear();

		}

	}

	private static class AgentInfo {

		public boolean stucked;
		String id;
		double enterTime;
		String currentLink;

	}

	private static class AgentCongestionInfo {
		public Integer timeSlot;
//		public boolean congested;
		public String agentId;
	}

	private static class LinkCongestionInfo {
		boolean congested = true;
		int agents = 0;
	}

	private static class SocCostInfo {

		double cost = 0;
		boolean updated = false;
	}

	private static class SocialCostRole {
		HashMap<Integer,SocCostInfo> socCosts = new HashMap<Integer, SocCostInfo>();


		public void setSocCost(final int timeSlice, final double socCost) {

			Integer slot = IntegerCache.getInteger(timeSlice);
			SocCostInfo sci = this.socCosts.get(slot);
			if (sci == null) {
				sci = new SocCostInfo();
				this.socCosts.put(slot, sci);
			}
			sci.cost = oldCoef * sci.cost + newCoef * socCost;
			sci.updated = true;
		}

		public double getSocCost(final int timeSlice) {
			Integer slot = IntegerCache.getInteger(timeSlice);
			SocCostInfo sci = this.socCosts.get(slot);
			if (sci == null) {
				return 0;
			}
			return sci.cost;
		}

		public void update() {
			for (SocCostInfo sci : this.socCosts.values()) {
				if (!sci.updated) {
					sci.cost = oldCoef * sci.cost;
				}
				sci.updated = false;
			}
		}
	}

	public void reset(final int iteration) {
		// TODO Auto-generated method stub

	}





}
