/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether.linkanalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;

public class TTInOutflowEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

	private Id linkIdIn;

	private Map<Id, BasicLinkEnterEvent> enterEvents;
	
	private SortedMap<Double, Integer> inflow;
	
	private SortedMap<Double, Integer> outflow;
	
	private SortedMap<Double, Double> travelTimes;

	private Id linkIdOut;
	
	public TTInOutflowEventHandler(Id linkId){
	  this(linkId, linkId);
	}
	
	
	public TTInOutflowEventHandler(Id linkIdIn, Id linkIdOut) {
		this.linkIdIn = linkIdIn;
		this.linkIdOut = linkIdOut;
		this.enterEvents = new HashMap<Id, BasicLinkEnterEvent>();
		this.inflow = new TreeMap<Double, Integer>();
		this.outflow =  new TreeMap<Double, Integer>();
		this.travelTimes = new TreeMap<Double, Double>();
	}

	
	
	public void handleEvent(BasicLinkEnterEvent event) {
		if (linkIdIn.equals(event.getLinkId())) {
			Integer in = getInflowMap().get(event.getTime());
			if (in == null) {
				in = Integer.valueOf(1);
				getInflowMap().put(event.getTime(), in);
			}
			else {
				in = Integer.valueOf(in.intValue() + 1);
				getInflowMap().put(event.getTime(), in);
			}
			this.enterEvents.put(new IdImpl(event.getPersonId().toString()), event);
		}
	}
	
	public void handleEvent(BasicLinkLeaveEvent event) {
		if (linkIdOut.equals(event.getLinkId())) {
			Integer out = getOutflowMap().get(event.getTime());
			if (out == null) {
				out = Integer.valueOf(1);
				getOutflowMap().put(event.getTime(), out);
			}
			else {
				out = Integer.valueOf(out.intValue() + 1);
				getOutflowMap().put(event.getTime(), out);
			}
			
			BasicLinkEnterEvent enterEvent = this.enterEvents.get(new IdImpl(event.getPersonId().toString()));
			double tt = event.getTime() - enterEvent.getTime();
			Double ttravel = getTravelTimesMap().get(enterEvent.getTime());
			if (ttravel == null) {
				getTravelTimesMap().put(Double.valueOf(enterEvent.getTime()), Double.valueOf(tt));
			}
			else {
				ttravel = Double.valueOf(((ttravel.doubleValue() * (out.doubleValue() - 1)) + tt) / out.doubleValue());
				getTravelTimesMap().put(Double.valueOf(enterEvent.getTime()), ttravel);
			}
		}
	}

	public void reset(int iteration) {
		this.enterEvents.clear();
		this.getInflowMap().clear();
		this.getOutflowMap().clear();
		this.getTravelTimesMap().clear();
	}
	
	public Id getLinkId() {
		return this.linkIdIn;
	}

	public SortedMap<Double, Integer> getInflowMap() {
		return inflow;
	}

	public SortedMap<Double, Integer> getOutflowMap() {
		return outflow;
	}

	public SortedMap<Double, Double> getTravelTimesMap() {
		return travelTimes;
	}
}