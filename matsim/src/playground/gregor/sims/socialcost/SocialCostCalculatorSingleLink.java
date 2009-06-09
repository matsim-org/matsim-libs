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

package playground.gregor.sims.socialcost;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.AgentMoneyEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;

public class SocialCostCalculatorSingleLink implements SocialCostCalculator, IterationStartsListener,  AgentDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler{

	
	private static Logger log = Logger.getLogger(SocialCostCalculatorSingleLink.class);
	private final int travelTimeBinSize;
	private final int numSlots;
	private final NetworkLayer network;
	private final HashMap<Id,LinkInfo> linkInfos = new HashMap<Id, LinkInfo>();
	private final HashMap<Id,SocialCostRole> socCosts = new HashMap<Id, SocialCostRole>();;

	private final static int MSA_OFFSET = 20;

	static double oldCoef = 0;
	static double newCoef = 1;
	static int iteration = 0;

	public SocialCostCalculatorSingleLink(final NetworkLayer network) {
		this(network, 15*60, 30*3600);	// default timeslot-duration: 15 minutes
	}

	public SocialCostCalculatorSingleLink(final NetworkLayer network, final int timeslice) {
		this(network, timeslice, 30*3600); // default: 30 hours at most
	}

	public SocialCostCalculatorSingleLink(final NetworkLayer network, final int timeslice,	final int maxTime) {
		this.travelTimeBinSize = timeslice;
		this.numSlots = (maxTime / this.travelTimeBinSize) + 1;
		this.network = network;
	}

	public double getSocialCost(final Link link, final double time) {
		SocialCostRole sc = this.socCosts.get(link.getId());
		if (sc == null) {
			return 0;
		}
		return sc.getSocCost(getTimeSlotIndex(time));
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
		double maxCost = 0;
		double minCost = Double.POSITIVE_INFINITY;
		double costSum = 0;
		
		for (SocialCostRole scr : this.socCosts.values()) {
			scr.update();
			for (SocCostInfo sci : scr.socCosts.values()) {
				double cost = sci.cost;
				if (cost < minCost) {
					minCost = cost;
				} else if (cost > maxCost) {
					maxCost = cost;
				}
				costSum += cost;
			}
		}
		log.info("maxCost: " + maxCost + " minCost: " + minCost + " avg: " + costSum/this.socCosts.size());
	}


	public void handleEvent(final LinkEnterEvent event) {
		LinkInfo info = getLinkInfo(event.getLinkId());
		AgentInfo aol = new AgentInfo();
		aol.enterTime = event.getTime();
		aol.id = event.getPersonId();
		info.agentsOnLink.add(aol);
	}

	public void handleEvent(final AgentDepartureEvent event) {
		LinkInfo info = getLinkInfo(event.getLinkId());
		AgentInfo aol = new AgentInfo();
		aol.enterTime = event.getTime();
		aol.id = event.getPersonId();
		info.agentsOnLink.add(aol);
	}

	public void handleEvent(final LinkLeaveEvent event) {
		LinkInfo info = getLinkInfo(event.getLinkId());
		AgentInfo aol = info.agentsOnLink.poll();
		aol.exitTime = event.getTime();

		if ((event.getTime() - aol.enterTime) <= info.t_free) {
			if (info.agentsLeftLink.size() > 0){
				computeSocCost(info,event.getLinkId(),event.getTime());
			}
			info.lastFSSlice = getTimeSlotIndex(event.getTime());
		} else {
			info.agentsLeftLink.add(aol);
		}

	}

	private void computeSocCost(final LinkInfo info, final Id linkId, final double eventTime) {

		SocialCostRole sc = this.socCosts.get(linkId);
		if (sc == null) {
			sc = new SocialCostRole();
			this.socCosts.put(linkId, sc);
		}

		int lB = info.lastFSSlice + 1; 
			
			//Math.max(info.lastFSSlice ,getTimeSlotIndex(info.agentsLeftLink.peek().enterTime));
		int uB = getTimeSlotIndex(eventTime) - 1;






		double socCost = 0;
		for (int i = uB; i >= lB; i--) {
			socCost += this.travelTimeBinSize;
			sc.setSocCost(i, Math.max(0,socCost - info.t_free));
		}

		while (info.agentsLeftLink.size() > 0) {
			AgentInfo ai = info.agentsLeftLink.poll();
			int slot = getTimeSlotIndex(ai.enterTime);
			if (slot < lB || slot > uB) {
				continue;
			}
			double tmp  = sc.getSocCost(slot)  ;
			double cost =  tmp / -600;
			AgentMoneyEvent e = new AgentMoneyEvent(eventTime,ai.id,cost);
			QueueSimulation.getEvents().processEvent(e);	
		}


	}


	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.travelTimeBinSize;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}


	private LinkInfo getLinkInfo(final Id id) {
		LinkInfo ret = this.linkInfos.get(id);
		if (ret == null) {
			ret = new LinkInfo();
			ret.t_free = Math.ceil(this.network.getLink(id).getFreespeedTravelTime(Time.UNDEFINED_TIME)); //TODO make this dynamic, since we have time variant networks
			this.linkInfos.put(id, ret);
		}
		return ret;
	}



	private static class LinkInfo {
		ConcurrentLinkedQueue<AgentInfo> agentsOnLink = new ConcurrentLinkedQueue<AgentInfo>();
		ConcurrentLinkedQueue<AgentInfo> agentsLeftLink = new ConcurrentLinkedQueue<AgentInfo>();
		double t_free;
		//		double cTime = Time.UNDEFINED_TIME;
		int lastFSSlice = 0;
	}

	private static class AgentInfo {

		public double exitTime;
		Id id;
		double enterTime;

	}

	private static class SocCostInfo {

		double cost = 0;
		boolean updated = false;
	}

	private static class SocialCostRole {
		HashMap<Integer,SocCostInfo> socCosts = new HashMap<Integer, SocCostInfo>();


		public void setSocCost(final int timeSlice, final double socCost) {
			if (Double.isInfinite(socCost)) {
				System.err.println("inf costs!!");
			}
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

	public void reset(final int iteration) {
		// TODO Auto-generated method stub
		
	}
}