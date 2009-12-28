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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;

public class TTInOutflowEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {
	
	private static final Logger log = Logger.getLogger(TTInOutflowEventHandler.class);
	
	private Id linkIdIn;

	private Map<Id, LinkEnterEvent> enterEvents;
	
	private SortedMap<Double, Integer> inflow;
	
	private SortedMap<Double, Integer> outflow;
	
	private SortedMap<Double, Double> travelTimes;
	
	private SortedMap<Integer, Integer> countPerItertation;

	private Id linkIdOut;
	
	private int count = 0;
	
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
		this.countPerItertation = new TreeMap<Integer, Integer>();
	}

	
	
	public void handleEvent(LinkEnterEvent event) {
		if (linkIdIn.equals(event.getLinkId())) {
			//inflow
			Integer in = getInflowMap().get(event.getTime());
			if (in == null) {
				in = Integer.valueOf(1);
				getInflowMap().put(event.getTime(), in);
			}
			else {
				in = Integer.valueOf(in.intValue() + 1);
				getInflowMap().put(event.getTime(), in);
			}
			//traveltime
			this.enterEvents.put(new IdImpl(event.getPersonId().toString()), event);
		}
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		if (linkIdOut.equals(event.getLinkId())) {
			//flow
			Integer out = getOutflowMap().get(event.getTime());
			if (out == null) {
				out = Integer.valueOf(1);
				getOutflowMap().put(event.getTime(), out);
			}
			else {
				out = Integer.valueOf(out.intValue() + 1);
				getOutflowMap().put(event.getTime(), out);
			}
			//travel time
			LinkEnterEvent enterEvent = this.enterEvents.get(new IdImpl(event.getPersonId().toString()));
			double tt = event.getTime() - enterEvent.getTime();
//			log.error("Travel time on link " + event.getLinkId() + " " + tt);
			Double ttravel = getTravelTimesMap().get(enterEvent.getTime());
			if (ttravel == null) {
				getTravelTimesMap().put(Double.valueOf(enterEvent.getTime()), Double.valueOf(tt));
			}
			else {
				ttravel = Double.valueOf( ((ttravel.doubleValue() * (out.doubleValue() - 1)) + tt) / out.doubleValue() );
				getTravelTimesMap().put(Double.valueOf(enterEvent.getTime()), ttravel);
			}
			//total count
			this.count++;
		}
	}

	public void reset(int iteration) {
		this.countPerItertation.put(Integer.valueOf(iteration), Integer.valueOf(this.count));
		this.count = 0;
		this.enterEvents.clear();
		this.getInflowMap().clear();
		this.getOutflowMap().clear();
		this.getTravelTimesMap().clear();
	}
	
	public Id getLinkId() {
		return this.linkIdIn;
	}

	public SortedMap<Integer, Integer> getCountPerIteration(){
		return this.countPerItertation;
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