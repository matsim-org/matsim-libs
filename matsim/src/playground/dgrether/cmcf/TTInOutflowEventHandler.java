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
package playground.dgrether.cmcf;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.interfaces.basic.v01.Id;

public class TTInOutflowEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

	private Id linkIdIn;

	private Map<Id, LinkEnterEvent> enterEvents;
	
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
		this.enterEvents = new HashMap<Id, LinkEnterEvent>();
		this.inflow = new TreeMap<Double, Integer>();
		this.outflow =  new TreeMap<Double, Integer>();
		this.travelTimes = new TreeMap<Double, Double>();
	}

	
	public Id getLinkId() {
		return this.linkIdIn;
	}
	
	public void handleEvent(LinkEnterEvent event) {
		Id id = new IdImpl(event.linkId);
		if (linkIdIn.equals(id)) {
			Integer in = getInflowMap().get(event.time);
			if (in == null) {
				in = Integer.valueOf(1);
			  getInflowMap().put(event.time, in);
			}
			else {
				in = Integer.valueOf(in.intValue() + 1);
			  getInflowMap().put(event.time, in);
			}
			this.enterEvents.put(new IdImpl(event.agentId), event);
		}
	}


	public void handleEvent(LinkLeaveEvent event) {
		Id id = new IdImpl(event.linkId);
		if (linkIdOut.equals(id)) {
			Integer out = getOutflowMap().get(event.time);
			if (out == null) {
				out = Integer.valueOf(1);
				getOutflowMap().put(event.time, out);
			}
			else {
				out = Integer.valueOf(out.intValue() + 1);
				getOutflowMap().put(event.time, out);
			}
			
			LinkEnterEvent enterEvent = this.enterEvents.get(new IdImpl(event.agentId));
			double tt = event.time - enterEvent.time;
			Double ttravel = getTravelTimesMap().get(enterEvent.time);
			if (ttravel == null) {
				getTravelTimesMap().put(Double.valueOf(enterEvent.time), Double.valueOf(tt));
			}
			else {
				ttravel = Double.valueOf(((ttravel.doubleValue() * (out.doubleValue() - 1)) + tt) / out.doubleValue());
				getTravelTimesMap().put(Double.valueOf(enterEvent.time), ttravel);
			}
		}
	}

	public void reset(int iteration) {
		this.enterEvents.clear();
		this.getInflowMap().clear();
		this.getOutflowMap().clear();
		this.getTravelTimesMap().clear();
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