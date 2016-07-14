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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public class TTInOutflowEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {
	
	private Id<Link> linkIdIn;

	private Map<Id<Vehicle>, LinkEnterEvent> enterEvents;
	
	private SortedMap<Double, Integer> inflow;
	
	private SortedMap<Double, Integer> outflow;
	
	private SortedMap<Double, Double> travelTimes;
	
	private SortedMap<Integer, Integer> countPerItertation;

	private Id<Link> linkIdOut;
	
	private int count = 0;
	
	public TTInOutflowEventHandler(Id<Link> linkId){
	  this(linkId, linkId);
	}
	
	
	public TTInOutflowEventHandler(Id<Link> linkIdIn, Id<Link> linkIdOut) {
		this.linkIdIn = linkIdIn;
		this.linkIdOut = linkIdOut;
		this.enterEvents = new HashMap<>();
		this.inflow = new TreeMap<Double, Integer>();
		this.outflow =  new TreeMap<Double, Integer>();
		this.travelTimes = new TreeMap<Double, Double>();
		this.countPerItertation = new TreeMap<Integer, Integer>();
	}

	
	
	@Override
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
			this.enterEvents.put(event.getVehicleId(), event);
		}
	}
	
	@Override
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
			LinkEnterEvent enterEvent = this.enterEvents.get(event.getVehicleId());
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

	@Override
	public void reset(int iteration) {
		//reset again to avoid errors in case iterationEnds() isn't called
		this.count = 0;
		this.enterEvents.clear();
		this.getInflowMap().clear();
		this.getOutflowMap().clear();
		this.getTravelTimesMap().clear();
	}

	public void iterationsEnds(int iteration) {
		this.countPerItertation.put(Integer.valueOf(iteration), Integer.valueOf(this.count));
		this.count = 0;
		this.enterEvents.clear();
		this.getInflowMap().clear();
		this.getOutflowMap().clear();
		this.getTravelTimesMap().clear();
	}

	
	public Id<Link> getLinkId() {
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