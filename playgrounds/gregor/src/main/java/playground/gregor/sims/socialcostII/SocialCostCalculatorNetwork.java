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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.IntegerCache;
import org.matsim.core.utils.misc.Time;

public class SocialCostCalculatorNetwork implements IterationStartsListener,  AgentDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler{

	private final int travelTimeBinSize;
	private final int numSlots;
	private final NetworkLayer network;

	
	private final HashMap<String,AgentInfo> agentInfos = new HashMap<String, AgentInfo>();
	private final HashMap<String,LinkInfo> linkInfos = new HashMap<String, LinkInfo>();
	private final HashMap<String,SocialCostRole> socCosts = new HashMap<String, SocialCostRole>();
	
	private final static int MSA_OFFSET = 20;
	private final static double CONGESTION_RATION_THRESHOLD = 0.9;
	
	static double oldCoef = 0;
	static double newCoef = 1;
	static int iteration = 0;
	
	public SocialCostCalculatorNetwork(final NetworkLayer network) {
		this(network, 15*60, 30*3600);	// default timeslot-duration: 15 minutes
	}

	public SocialCostCalculatorNetwork(final NetworkLayer network, final int timeslice) {
		this(network, timeslice, 30*3600); // default: 30 hours at most
	}

	public SocialCostCalculatorNetwork(final NetworkLayer network, final int timeslice,	final int maxTime) {
		this.travelTimeBinSize = timeslice;
		this.numSlots = (maxTime / this.travelTimeBinSize) + 1;
		this.network = network;
	}
	
	public double getSocialCost(final LinkImpl link, final double time) {
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

			if (info.causesSpillback()) {
				oldInfo.getCongestedSlot(getTimeSlotIndex(ai.enterTime)).add(ai.id);
				oldInfo.con++;
			} else {
				oldInfo.getUncongestedSlot(getTimeSlotIndex(ai.enterTime)).add(ai.id);
				oldInfo.uncon++;
			}
		}
		ai.currentLink = event.getLinkId().toString();
		ai.enterTime = event.getTime();
		ai.id = event.getPersonId().toString();
		info.agentsOnLink++;
	}
	
	

	public void handleEvent(final AgentDepartureEvent event) {
		LinkInfo info = getLinkInfo(event.getLinkId().toString());
		AgentInfo ai = getAgentInfo(event.getPersonId().toString());
		ai.enterTime = event.getTime();
		ai.id = event.getPersonId().toString();
		ai.currentLink = event.getLinkId().toString();
		info.agentsOnLink++;
	}
	
	public void handleEvent(final LinkLeaveEvent event) {
		
		LinkInfo info = getLinkInfo(event.getLinkId().toString());
		info.agentsOnLink--;
		AgentInfo ai = getAgentInfo(event.getPersonId().toString());

		if ((event.getTime() - ai.enterTime) <= info.t_free){
//			info.lastFSSlice  = getTimeSlotIndex(event.time);
			ai.stucked = false;	
			
			// optimization
			if (info.congested) {
				clacSocCost(info,info.lastFSTime,ai.enterTime);
			}
			info.lastFSTime = ai.enterTime;
		} else {
			ai.stucked = true;
			info.congested = true;// optimization
		}
				
	}
	
	private void clacSocCost(final LinkInfo info, final double lastTimeUncongested, final double currentTimeUncongested) {
		
				
		int lB = getTimeSlotIndex(lastTimeUncongested)+1;
		int uB = getTimeSlotIndex(currentTimeUncongested)-1;
		
		if (uB <= lB) {
			info.cleanUpCongestionInfo();
			info.congested = false; // optimization
			return;
		}
		
		SocialCostRole sc = this.socCosts.get(info.id);
		if (sc == null) {
			sc = new SocialCostRole();
			this.socCosts.put(info.id, sc);
		}
		
		double p = info.uncon == 0 ? 0. : ((double)info.uncon / (info.uncon + info.con));
		double socCost = 0;
		for (int i = uB; i >= lB; i--) {
			ArrayList<String> toCongested = info.getCongestedSlot(i);
			ArrayList<String> toUncongested = info.getUncongestedSlot(i);
		
			
//			double p = toUncongested.size() == 0 ? 0 : ((double)toUncongested.size() / (toCongested.size() + toUncongested.size())); 
			
			
			socCost += this.travelTimeBinSize;
			double socCostCurrent = p * Math.max(0,socCost-this.travelTimeBinSize/2 - info.t_free); 
//			sc.setSocCost(i,w * (socCost-this.travelTimeBinSize/2));
			sc.setSocCost(i, socCostCurrent);
//			sc.setSocCost(i, Math.max(0,socCost-this.travelTimeBinSize/2));
//			sc.setSocCost(i, socCost);
			
			double scoringCost =  sc.getSocCost(i) / -600;
//			double scoringCost =  socCostCurrent / -600;
			for (String agentId : toCongested) {
				Id id = new IdImpl(agentId);
				AgentMoneyEventImpl e = new AgentMoneyEventImpl(currentTimeUncongested,id,scoringCost);
				QueueSimulation.getEvents().processEvent(e);
			}
			for (String agentId : toUncongested) {
				Id id = new IdImpl(agentId);
				AgentMoneyEventImpl e = new AgentMoneyEventImpl(currentTimeUncongested,id,scoringCost);
				QueueSimulation.getEvents().processEvent(e);
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
			ret.t_free = Math.ceil(this.network.getLinks().get(new IdImpl(id)).getFreespeedTravelTime(Time.UNDEFINED_TIME)); //TODO make this dynamic, since we have time variant networks
			LinkImpl link = this.network.getLinks().get(new IdImpl(id));
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
	
	
	private int calcCapacity(final LinkImpl link) {
		// network.capperiod is in hours, we need it per sim-tick and multiplied with flowCapFactor
		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();

//		this.inverseSimulatedFlowCapacity = 1.0 / this.simulatedFlowCapacity;
//		this.bufferStorageCapacity = (int) Math.ceil(this.simulatedFlowCapacity);
//		this.flowCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;

		// first guess at storageCapacity:
		double storageCapacity = (link.getLength() * link.getNumberOfLanes(Time.UNDEFINED_TIME))
				/ ((NetworkLayer) link.getLayer()).getEffectiveCellSize() * storageCapFactor;
				
		return (int) Math.max(Math.floor(storageCapacity),1);
	}
	
	
	private static class LinkInfo {
		
		
		public int con = 0;
		public int uncon = 0;
		
		public double lastFSTime = 0;

		public String id;

		public boolean congested = false;// optimization
		
		public int storageCap;

		public int agentsOnLink = 0;
		
		private final Map<Integer,ArrayList<String>> agentsLeftToUncongestedLink = new HashMap<Integer,ArrayList<String>>();
		private final Map<Integer,ArrayList<String>> agentsLeftToCongestedLink = new HashMap<Integer,ArrayList<String>>();

		double t_free;
		
		public boolean causesSpillback() {
			return this.agentsOnLink / this.storageCap > CONGESTION_RATION_THRESHOLD;
		}
		
		public ArrayList<String> getUncongestedSlot(final int slot) {
			Integer slice = IntegerCache.getInteger(slot);
			ArrayList<String> al = this.agentsLeftToUncongestedLink.get(slice);
			if (al == null) {
				al = new ArrayList<String>();
				this.agentsLeftToUncongestedLink.put(slice,al);
			}
			
			return al;
		}
		public ArrayList<String> getCongestedSlot(final int slot) {
			Integer slice = IntegerCache.getInteger(slot);
			ArrayList<String> al = this.agentsLeftToCongestedLink.get(slice);
			if (al == null) {
				al = new ArrayList<String>();
				this.agentsLeftToCongestedLink.put(slice,al);
			}
			return al;
		}
		
		public void cleanUpCongestionInfo(){
			this.agentsLeftToCongestedLink.clear();
			this.agentsLeftToUncongestedLink.clear();
			this.con = 0;
			this.uncon = 0;
		}
		
	}
	
	private static class AgentInfo {
		
		public boolean stucked;
		String id;
		double enterTime;
		String currentLink;
		
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
