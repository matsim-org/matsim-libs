/* *********************************************************************** *
 * project: org.matsim.*
 * SocialCostCalculator.java
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
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.misc.Time;


public class SocialCostCalculator  implements IterationStartsListener, AgentDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

	HashMap<String,LinkInfo> linkInfos = new HashMap<String, LinkInfo>();
	private final HashMap<String,SocialCostRole> socCosts = new HashMap<String, SocialCostRole>();
	private final int travelTimeBinSize;
	private final int numSlots;
	private final NetworkLayer network;
	
	private final static int MSA_OFFSET = 20;
	
	static double oldCoef = 0;
	static double newCoef = 1;
	static int iteration = 0;
	
	public SocialCostCalculator(final NetworkLayer network) {
		this(network, 15*60, 30*3600);	// default timeslot-duration: 15 minutes
	}

	public SocialCostCalculator(final NetworkLayer network, final int timeslice) {
		this(network, timeslice, 30*3600); // default: 30 hours at most
	}

	public SocialCostCalculator(final NetworkLayer network, final int timeslice,	final int maxTime) {
		this.travelTimeBinSize = timeslice;
		this.numSlots = (maxTime / this.travelTimeBinSize) + 1;
		this.network = network;
	}
	
	
	
	public void notifyIterationStarts(final IterationStartsEvent event) {
		iteration = event.getIteration();
		this.linkInfos.clear();
		updateSocCosts();
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
	
	public double getSocialCost(final Link link, final double time) {
		SocialCostRole sc = this.socCosts.get(link.getId().toString());
		if (sc == null) {
			return 0;
		}
		return sc.getSocCost(getTimeSlotIndex(time));
	}

	public void handleEvent(final LinkEnterEvent event) {
		LinkInfo info = getLinkInfo(event.linkId);
		AgentInfo aol = new AgentInfo();
		aol.enterTime = event.time;
		aol.id = event.agentId;
		info.agentsOnLink.add(aol);
		
	}
	
	public void handleEvent(final LinkLeaveEvent event) {
		LinkInfo info = getLinkInfo(event.linkId);
		AgentInfo aol = info.agentsOnLink.poll();
		aol.exitTime = event.time;
		info.agentsLeftLink.add(aol);
		if ((event.time - aol.enterTime) <= info.t_free) {
			while (info.agentsLeftLink.size() > 0) {
				AgentInfo ai = info.agentsLeftLink.poll();
				computeSocCost(info,event.linkId,ai,event.time);
			}
		} 
	}
	public void handleEvent(final AgentDepartureEvent event) {
		LinkInfo info = getLinkInfo(event.linkId);
		AgentInfo aol = new AgentInfo();
		aol.enterTime = event.time;
		aol.id = event.agentId;
		info.agentsOnLink.add(aol);
		
	}

	public void reset(final int iteration) {
		// TODO Auto-generated method stub
		
	}

	private void computeSocCost(final LinkInfo info, final String linkId, final AgentInfo aol, final double time) {
		
		SocialCostRole sc = this.socCosts.get(linkId);
		if (sc == null) {
			sc = new SocialCostRole();
			this.socCosts.put(linkId, sc);
		}
		int timeSlice = getTimeSlotIndex(time);
		double socCost = time - aol.enterTime - info.t_free; 
		if (socCost <= 0) {
			sc.setZeroAndBlock(timeSlice);
		} else {
			sc.addSocCostValue(timeSlice, socCost);
			Id id = new IdImpl(aol.id);
			AgentMoneyEvent e = new AgentMoneyEvent(time,id,socCost/-600);
			QueueSimulation.getEvents().processEvent(e);	
		}
	}
	
	private LinkInfo getLinkInfo(final String id) {
		LinkInfo ret = this.linkInfos.get(id);
		if (ret == null) {
			ret = new LinkInfo();
			ret.t_free = Math.ceil(this.network.getLink(id).getFreespeedTravelTime(Time.UNDEFINED_TIME)); //TODO make this dynamic, since we have time variant networks
			this.linkInfos.put(id, ret);
		}
		return ret;
	}
	
	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.travelTimeBinSize;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}
	
	private static class LinkInfo {
		ConcurrentLinkedQueue<AgentInfo> agentsOnLink = new ConcurrentLinkedQueue<AgentInfo>();
		ConcurrentLinkedQueue<AgentInfo> agentsLeftLink = new ConcurrentLinkedQueue<AgentInfo>();
		double t_free;
	}
	
	private static class AgentInfo {
		
		public double exitTime;
		String id;
		double enterTime;
		
	}
	
	private static class SocCostInfo {
		
		double count = 0;
		double cost = 0;
		double oldCost = 0;
		public boolean blocked = false;
	}
	
	private static class SocialCostRole {
		HashMap<Integer,SocCostInfo> socCosts = new HashMap<Integer, SocCostInfo>();
		public void addSocCostValue(final int timeSlice, final double socCost) {
			SocCostInfo sci = this.socCosts.get(timeSlice);
			if (sci == null) {
				sci = new SocCostInfo();
				this.socCosts.put(timeSlice, sci);
			} else {
				if (sci.blocked) {
					return;
				}
			}
			if (sci.count == 0) {
				sci.oldCost = sci.cost;
				sci.cost = socCost;
			} else {
				sci.cost = (sci.count/(sci.count+1) * sci.cost + 1/(sci.count+1) * socCost);
			}
			sci.count++;
			
			
		}
		
		public void setZeroAndBlock(final int timeSlice) {
			SocCostInfo sci = this.socCosts.get(timeSlice);
			if (sci == null) {
				sci = new SocCostInfo();
				this.socCosts.put(timeSlice, sci);
			}
			sci.oldCost = sci.cost;
			sci.cost = 0;
			sci.blocked  = true;
			
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
				sci.cost = oldCoef * sci.oldCost + newCoef * sci.cost;
				sci.oldCost = 0;
				sci.count = 0;
			}
		}
	}

}
