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

package playground.gregor.systemopt;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.basic.v01.IdImpl;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentMoneyEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.NetworkLayer;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.trafficmonitoring.TravelTimeAggregatorFactory;
import org.matsim.utils.misc.Time;

public class TravelTimeAndSocialCostCalculatorHeadMultiLink extends TravelTimeCalculator implements IterationStartsListener, AgentDepartureEventHandler{

	private final int travelTimeBinSize;
	private final int numSlots;
	private final NetworkLayer network;

	
	private final HashMap<String,AgentInfo> agentInfos = new HashMap<String, AgentInfo>();
	private final HashMap<String,LinkInfo> linkInfos = new HashMap<String, LinkInfo>();
	private final HashMap<String,SocialCostRole> socCosts = new HashMap<String, SocialCostRole>();
	
	private final static int MSA_OFFSET = 20;
	
	static double oldCoef = 0;
	static double newCoef = 1;
	static int iteration = 0;
	
	public TravelTimeAndSocialCostCalculatorHeadMultiLink(final NetworkLayer network) {
		this(network, 15*60, 30*3600);	// default timeslot-duration: 15 minutes
	}

	public TravelTimeAndSocialCostCalculatorHeadMultiLink(final NetworkLayer network, final int timeslice) {
		this(network, timeslice, 30*3600); // default: 30 hours at most
	}

	public TravelTimeAndSocialCostCalculatorHeadMultiLink(final NetworkLayer network, final int timeslice,	final int maxTime) {
		this(network, timeslice, maxTime, new TravelTimeAggregatorFactory());
	}
	
	public TravelTimeAndSocialCostCalculatorHeadMultiLink(final NetworkLayer network, final int timeslice, final int maxTime, final TravelTimeAggregatorFactory factory) {
		super(network,timeslice,maxTime,factory);
		this.travelTimeBinSize = timeslice;
		this.numSlots = (maxTime / this.travelTimeBinSize) + 1;
		this.network = network;

	}
	public double getSocialCost(final Link link, final double time) {
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

	
	@Override
	public void handleEvent(final LinkEnterEvent event) {
		super.handleEvent(event);

		LinkInfo info = getLinkInfo(event.linkId);
		AgentInfo aol = this.agentInfos.get(event.agentId);
		if (aol == null) {
			aol = new AgentInfo();
			this.agentInfos.put(event.agentId, aol);
		} else if (!info.isCongested || info.storageCap > info.agentsOnLink.size()){ //no spill back from downstream link
			
			String oldLinkId = aol.currentLink;
			LinkInfo oldInfo = this.linkInfos.get(oldLinkId);
			if (oldInfo.isCongested){
				AgentInfo oldAol = new AgentInfo();
				oldAol.id = aol.id;
				oldAol.enterTime = aol.enterTime;
				oldAol.exitTime = aol.exitTime;
				oldInfo.agentsLeftLink.add(oldAol);
			}
			
		}
		
		aol.currentLink = event.linkId;
		aol.enterTime = event.time;
		aol.id = event.agentId;
		info.agentsOnLink.add(aol);
	}
	
	
	public void handleEvent(final AgentDepartureEvent event) {
		LinkInfo info = getLinkInfo(event.linkId);
		AgentInfo aol = new AgentInfo();
		aol.enterTime = event.time;
		aol.id = event.agentId;
		info.agentsOnLink.add(aol);
	}
	
	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		super.handleEvent(event);
		
		LinkInfo info = getLinkInfo(event.linkId);
		AgentInfo aol = info.agentsOnLink.poll();
		aol.exitTime = event.time;
		
		if ((event.time - aol.enterTime) <= info.t_free) {
			if (info.agentsLeftLink.size() > 0){
				computeSocCost(info,event.linkId,event.time);
			}
			info.isCongested = false;
			info.lastFSSlice = getTimeSlotIndex(event.time);
		} else {
			info.isCongested = true;
		}
		
	}
	
	@Override
	public void handleEvent(final AgentStuckEvent event) {
		super.handleEvent(event);
//		this.current.add(event.agentId);
	}

	
	private void computeSocCost(final LinkInfo info, final String linkId, final double eventTime) {
		
		SocialCostRole sc = this.socCosts.get(linkId);
		if (sc == null) {
			sc = new SocialCostRole();
			this.socCosts.put(linkId, sc);
		}
		
		int lB = Math.max(info.lastFSSlice,getTimeSlotIndex(info.agentsLeftLink.peek().enterTime));
		int uB = getTimeSlotIndex(eventTime) - 1;

		double socCost = 0;
		for (int i = uB; i >= lB; i--) {
			socCost += this.travelTimeBinSize;
//			sc.setSocCost(i,w * (socCost-this.travelTimeBinSize/2));
			sc.setSocCost(i, Math.max(0,socCost-this.travelTimeBinSize/2 - info.t_free));
//			sc.setSocCost(i, Math.max(0,socCost-this.travelTimeBinSize/2));
//			sc.setSocCost(i, socCost);
		}
		
		while (info.agentsLeftLink.size() > 0) {
			AgentInfo ai = info.agentsLeftLink.poll();
			int slot = getTimeSlotIndex(ai.enterTime);
			if (slot < lB || slot > uB) {
				continue;
			}
			double tmp  = sc.getSocCost(slot)  ;
			double cost =  tmp / -600;
			Id id = new IdImpl(ai.id);
			AgentMoneyEvent e = new AgentMoneyEvent(eventTime,id,cost);
			QueueSimulation.getEvents().processEvent(e);
			
		}
		
		
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
			ret.t_free = Math.ceil(this.network.getLink(id).getFreespeedTravelTime(Time.UNDEFINED_TIME)); //TODO make this dynamic, since we have time variant networks
			Link link = this.network.getLink(id);
			ret.storageCap = calcCapacity(link);
			
			this.linkInfos.put(id, ret);
		}
		return ret;
	}

	
	
	private int calcCapacity(final Link link) {
		if (link.getId().toString().contains("el")){
			int i = 0;
			i++;
		}
		// network.capperiod is in hours, we need it per sim-tick and multiplied with flowCapFactor
		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();

//		this.inverseSimulatedFlowCapacity = 1.0 / this.simulatedFlowCapacity;
//		this.bufferStorageCapacity = (int) Math.ceil(this.simulatedFlowCapacity);
//		this.flowCapFraction = this.simulatedFlowCapacity - (int) this.simulatedFlowCapacity;

		// first guess at storageCapacity:
		double storageCapacity = (link.getLength() * link.getLanes(Time.UNDEFINED_TIME))
				/ ((NetworkLayer) link.getLayer()).getEffectiveCellSize() * storageCapFactor;
		
		return (int) Math.floor(storageCapacity);
	}
	
	
	private static class LinkInfo {
		public int storageCap;
		public boolean isCongested = false;
		ConcurrentLinkedQueue<AgentInfo> agentsOnLink = new ConcurrentLinkedQueue<AgentInfo>();
		ConcurrentLinkedQueue<AgentInfo> agentsLeftLink = new ConcurrentLinkedQueue<AgentInfo>();
		double t_free;
//		double cTime = Time.UNDEFINED_TIME;
		int lastFSSlice = 0;
	}
	
	private static class AgentInfo {
		
		public double exitTime;
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
			SocCostInfo sci = this.socCosts.get(timeSlice);
			if (sci == null) {
				sci = new SocCostInfo();
				this.socCosts.put(timeSlice, sci);
			}
			sci.cost = oldCoef * sci.cost + newCoef * socCost;
			sci.updated = true;
		}
		
		public double getSocCost(final int timeSlice) {
			SocCostInfo sci = this.socCosts.get(timeSlice);
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



	
	
}
