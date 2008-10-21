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

import java.util.HashMap;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.utils.misc.IntegerCache;

public class TravelTimeAndSocialCostCalculator extends TravelTimeCalculator {

	private HashMap<String,HashMap<Integer,LinkInfo>> linkInfos;
	private final int travelTimeBinSize;
	private final int numSlots;
	
	
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
		
		if (linkInfo.onLink <= 0) {
			return 0;
		}
		
		return linkInfo.leftLink == 0 ? (double)linkInfo.onLink : (double)linkInfo.onLink / (double)linkInfo.leftLink;
	}
	
	
	@Override
	public void handleEvent(final LinkEnterEvent event) {
		super.handleEvent(event);

		LinkInfo linkInfo = getLinkInfo(event.linkId, event.time);
		linkInfo.onLink++;
	}
	
	

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		super.handleEvent(event);

		LinkInfo linkInfo = getLinkInfo(event.linkId, event.time);
		linkInfo.leftLink++;
		linkInfo.onLink--;
	}
	
	
	
	
	@Override
	public void handleEvent(final AgentArrivalEvent event) {
		super.handleEvent(event);
		LinkInfo linkInfo = getLinkInfo(event.linkId, event.time);
		linkInfo.onLink--;
	}

	@Override
	public void handleEvent(final AgentStuckEvent event) {
		super.handleEvent(event);
		LinkInfo linkInfo = getLinkInfo(event.linkId, event.time);
		linkInfo.onLink--;
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
			linkInfoM.put(ts, linkInfo);
		}
		return linkInfo;
	}

	@Override
	public void resetTravelTimes() {
		if (this.linkInfos == null) {
			this.linkInfos = new HashMap<String, HashMap<Integer,LinkInfo>>();
		}
		this.linkInfos.clear();
		super.resetTravelTimes();
	}

	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.travelTimeBinSize;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}

	private static class LinkInfo {
		
		int onLink = 0;
		int leftLink = 0;
		
	}


	
}
