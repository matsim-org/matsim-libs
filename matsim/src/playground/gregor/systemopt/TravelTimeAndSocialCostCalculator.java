/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeAndSocialCostCalculator.java
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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentUtilityEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.IntegerCache;

public class TravelTimeAndSocialCostCalculator extends TravelTimeCalculator implements IterationStartsListener {

	
	private HashMap<String,HashMap<Integer,LinkInfo>> linkInfos;
	private final int travelTimeBinSize;
	private final int numSlots;
	private final NetworkLayer network;
	private int iteration;
	private final int offset = 20;
	private BufferedWriter writer;
	
	
	public TravelTimeAndSocialCostCalculator(final NetworkLayer network) {
		this(network, 15*60, 30*3600);	// default timeslot-duration: 15 minutes
	}

	public TravelTimeAndSocialCostCalculator(final NetworkLayer network, final int timeslice) {
		this(network, timeslice, 30*3600); // default: 30 hours at most
	}

	public TravelTimeAndSocialCostCalculator(final NetworkLayer network, final int timeslice,	final int maxTime) {
		this(network, timeslice, maxTime, new TravelTimeCalculatorFactory());
	}
	
	public TravelTimeAndSocialCostCalculator(final NetworkLayer network, final int timeslice, final int maxTime, final TravelTimeCalculatorFactory factory) {
		super(network,timeslice,maxTime,factory);
		this.travelTimeBinSize = timeslice;
		this.numSlots = (maxTime / this.travelTimeBinSize) + 1;
		this.network = network;
		
		try {
			 this.writer = IOUtils.getBufferedWriter("tt.csv", false);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			this.writer.write("it,cumMSA\n");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	
	
	public double getSocialCost(final Link link, final double time) {
		
		
		HashMap<Integer,LinkInfo> linkInfoM = this.linkInfos.get(link.getId().toString());
		if (linkInfoM == null) {
			return 0;
		}
		
		Integer ts = IntegerCache.getInteger(getTimeSlotIndex(time));
		LinkInfo linkInfo = linkInfoM.get(ts);
		if (linkInfo == null) {
			return 0;
		}
		
		return getSocialCost(linkInfo);
	}
	
	
	private double getSocialCost(final LinkInfo linkInfo) {
		
		if (!linkInfo.updated) {
			updateLinkInfo(linkInfo);
		}
		return linkInfo.socCost;
		
	}
	
	public void updateLinkInfo(final LinkInfo linkInfo) {
		
		double tmp ;
		if (linkInfo.congestionIndex == 0) {
			tmp = 0;
		} else if (linkInfo.agentsLeftLink.size() == 0) {
			tmp = Double.MAX_VALUE;
		} else {
			double aLL = linkInfo.agentsLeftLink.size();
			tmp = linkInfo.congestionIndex * this.travelTimeBinSize / (aLL * aLL);				
		}

		if (this.iteration >= this.offset) {
			double n = this.iteration - this.offset;
			double msaCost = n / (n + 1) * linkInfo.socCost + 1 / (n + 1) * tmp;
			
			linkInfo.socCost = msaCost;
			
		} else {
			linkInfo.socCost = tmp;
		}
		linkInfo.updated = true;
	}
	
	
	
	
	
	@Override
	public void handleEvent(final LinkEnterEvent event) {
		super.handleEvent(event);

		LinkInfo linkInfo = getLinkInfo(event.linkId, event.time);
		AgentOnLink aol = new AgentOnLink();
		aol.enterTime = event.time;
		aol.id = event.agentId;
		linkInfo.agentsOnLink.add(aol);
		
//		linkInfo.agentsOnLink.put(event.agentId,event.time);
	}
	
	

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		super.handleEvent(event);

		LinkInfo linkInfo = getLinkInfo(event.linkId, event.time);
		AgentOnLink aol = linkInfo.agentsOnLink.peek();
		
		//Agent never entered the link - its probably the first link after an activity
		if (aol == null || !aol.id.equals(event.agentId)) {
//			linkInfo.agentsLeftLink.add(event.agentId);
			return;			
		}
	
		linkInfo.agentsOnLink.poll();
		linkInfo.agentsLeftLink.add(event.agentId);
		
//		EST model
		double tt = event.time - aol.enterTime;
		double congestionQueue = (1 - linkInfo.freeSpeedTravelTime / tt) * linkInfo.agentsOnLink.size();
		
		linkInfo.congestionIndex += congestionQueue;
		
		
//		if (linkInfo.agentsOnLink.size() == 0) {
//			return;
//		}
////		
////		//TODO replace it with a more efficient method ...
//		Iterator<AgentOnLink> it =  linkInfo.agentsOnLink.iterator();
//		int inCongestion = 0;
//		double congestionTime = event.time - linkInfo.freeSpeedTravelTime;
//		
//		for (AgentOnLink tmp = it.next(); it.hasNext(); tmp = it.next()) {
//			if (tmp.enterTime >= congestionTime) {
//				break;
//			}
//			inCongestion++;
//		}
//		
//		
//		linkInfo.congestionIndex += inCongestion;
		
//		Double enterTime = aol.enterTime;
//		
//		double travelTime = event.time - enterTime;
//		if (travelTime > linkInfo.freeSpeedTravelTime) {
//			linkInfo.congested = true;
//		}
		
//		linkInfo.agentsOnLink.remove(event.agentId);
//		linkInfo.agentsLeftLink.add(event.agentId);
//		linkInfo.leftLink++;
//		linkInfo.onLink--;
	}
	
	
	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.iteration = event.getIteration();
	}
	
	
	@Override
	public void handleEvent(final AgentArrivalEvent event) {
		super.handleEvent(event);
		LinkInfo linkInfo = getLinkInfo(event.linkId, event.time);
		
		//FIXME this an evil hack - but works fine for evacuation scenarios ...
		//because, there are no social costs on "arrival links" (sinks)
		linkInfo.agentsOnLink.clear();
		linkInfo.agentsLeftLink.clear();
		linkInfo.congestionIndex = 0;
//		linkInfo.onLink--;
	}

	@Override
	public void handleEvent(final AgentStuckEvent event) {
		super.handleEvent(event);
		LinkInfo linkInfo = getLinkInfo(event.linkId, event.time);
		AgentOnLink aol = linkInfo.agentsOnLink.peek();
		//Agent never entered the link - its probably the first link after an activity
		if (aol == null || !aol.id.equals(event.agentId)) {
//			linkInfo.agentsLeftLink.add(event.agentId);
			return;			
		}
	
		linkInfo.agentsOnLink.poll();
		
		double tt = event.time - aol.enterTime;
		int onLink = linkInfo.agentsOnLink.size();
		double congestionQueue = (1 - linkInfo.freeSpeedTravelTime / tt) * onLink;
		
		linkInfo.congestionIndex += congestionQueue;
		
		
//		if (linkInfo.agentsOnLink.size() == 0) {
//			return;
//		}
////		
////		//TODO replace it with a more efficient method ...
//		Iterator<AgentOnLink> it =  linkInfo.agentsOnLink.iterator();
//		int inCongestion = 0;
//		double congestionTime = event.time - linkInfo.freeSpeedTravelTime;
//		
//		for (AgentOnLink tmp = it.next(); it.hasNext(); tmp = it.next()) {
//			if (tmp.enterTime >= congestionTime) {
//				break;
//			}
//			inCongestion++;
//		}
	}

	private LinkInfo getLinkInfo(final String linkId, final double time) {
		HashMap<Integer,LinkInfo> linkInfoM = this.linkInfos.get(linkId);
		if (linkInfoM == null) {
			linkInfoM = new HashMap<Integer, LinkInfo>();
			this.linkInfos.put(linkId, linkInfoM);
		}
		
		Integer ts = IntegerCache.getInteger(getTimeSlotIndex(time));
		LinkInfo linkInfo = linkInfoM.get(ts);
		if (linkInfo == null) {
			
			linkInfo = new LinkInfo();
			Link link = this.network.getLink(linkId);
			linkInfo.freeSpeedTravelTime = Math.ceil(link.getFreespeedTravelTime(time));
			linkInfoM.put(ts, linkInfo);
		}
		LinkInfo tsOld = linkInfoM.get(ts -1);
		
		if (tsOld != null && tsOld.agentsLeftLink.size() > 0){
			fireUtilityEvents(tsOld,time);
			linkInfo.agentsOnLink.addAll(tsOld.agentsOnLink);
			tsOld.agentsOnLink.clear();
		}

		
		return linkInfo;
	}

	private void fireUtilityEvents(final LinkInfo tsOld, final double time) {
		
	
		
		double socCost = getSocialCost(tsOld);
		
		if (socCost <= 0) {
			return;
		}
		
		//individual social costs - rescaled
		socCost = -socCost / 600; // / tsOld.agentsLeftLink.size();
		
		while (tsOld.agentsLeftLink.size() > 0) {
			String strId = tsOld.agentsLeftLink.poll();
			Id id = new IdImpl(strId);
			AgentUtilityEvent e = new AgentUtilityEvent(time,id,socCost);
			QueueSimulation.getEvents().processEvent(e);	
			
		}
		
		
		
		
	}

	@Override
	public void resetTravelTimes() {
		if (this.linkInfos == null) {
			this.linkInfos = new HashMap<String, HashMap<Integer,LinkInfo>>();
		} else {
			resetLinkInfos();
		}
		super.resetTravelTimes();
	}

	private void resetLinkInfos() {
		double MSAcost = 0;
		for (HashMap<Integer,LinkInfo> linkInfos : this.linkInfos.values()) {
			for (LinkInfo linkInfo : linkInfos.values()) {
				MSAcost += linkInfo.socCost;
				linkInfo.reset();
			}
		}
		try {
			this.writer.write(this.iteration + "," + MSAcost + "\n");
			this.writer.flush();
			if (this.iteration >= 300) {
				this.writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.travelTimeBinSize;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}

	

	private static class LinkInfo {
		

		private final ConcurrentLinkedQueue<String> agentsLeftLink = new ConcurrentLinkedQueue<String>();
		
		private final ConcurrentLinkedQueue<AgentOnLink> agentsOnLink = new ConcurrentLinkedQueue<AgentOnLink>();
		
		private double freeSpeedTravelTime = 0;
		
		private double congestionIndex = 0;
		
		private double socCost = 0;
		
		private boolean updated = false;
	

		public void reset() {
			this.agentsLeftLink.clear();
			this.agentsOnLink.clear();
			this.congestionIndex = 0;
			this.updated = false;
			
		}



	}

	private static class AgentOnLink {
		
		String id;
		double enterTime;
		
	}




	

	
}
