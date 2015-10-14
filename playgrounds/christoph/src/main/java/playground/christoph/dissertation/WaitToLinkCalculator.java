/* *********************************************************************** *
 * project: org.matsim.*
 * WaitToLinkCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.dissertation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

public class WaitToLinkCalculator implements PersonDepartureEventHandler,
		Wait2LinkEventHandler, LinkLeaveEventHandler, AfterMobsimListener {

	private final Set<Id> carAgents = new HashSet<Id>();
	private final Map<Id, Double> waitToLinkEventTimes = new HashMap<Id, Double>();
	
	private final int timeSlice;
	private final int numSlots;
	private final Map<Id, double[]> waitToLinkTimes = new HashMap<Id, double[]>();
	private final List<WaitToLinkData> waitToLinkData = new ArrayList<WaitToLinkData>();
	
	public WaitToLinkCalculator(final int timeSlice, final int maxTime) {
		this.timeSlice = timeSlice;
		this.numSlots = (maxTime / this.timeSlice) + 1;
	}
	
	@Override
	public void reset(int iteration) {
		this.carAgents.clear();
		this.waitToLinkData.clear();
		this.waitToLinkTimes.clear();
		this.waitToLinkEventTimes.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (carAgents.remove(event.getDriverId())) {
			double waitToLinkEventTime = waitToLinkEventTimes.remove(event.getDriverId());
			double waitToLinkTime = event.getTime() - waitToLinkEventTime;
			
			WaitToLinkData waitToLinkData = new WaitToLinkData();
			waitToLinkData.linkId = event.getLinkId();
			waitToLinkData.time = waitToLinkEventTime;
			waitToLinkData.waitToLinkTime = waitToLinkTime;
			this.waitToLinkData.add(waitToLinkData);
		}
	}

	@Override
	public void handleEvent(Wait2LinkEvent event) {
		if (carAgents.contains(event.getPersonId())) waitToLinkEventTimes.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			carAgents.add(event.getPersonId());
		}
	}

	public double getWaitToLinkTime(Id linkId, double time) {
		double[] array = this.waitToLinkTimes.get(linkId);
		if (array == null) return 0.0;
		else return array[getSlotIndex(time)];
	}

	private int getSlotIndex(double time) {
		return Math.min(numSlots, (int) (time / timeSlice));
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		Map<Id, List<Double>> times = new HashMap<Id, List<Double>>();
		double timeSlotEnd = timeSlice;
		int index = 0;
		for (WaitToLinkData waitToLinkData : this.waitToLinkData) {
			
			if (waitToLinkData.time >  timeSlotEnd) {
				
				for (Entry<Id, List<Double>> entry : times.entrySet()) {
					double averageTime = 0.0;
					for (double time : entry.getValue()) averageTime += time;
					averageTime /= entry.getValue().size();
					entry.getValue().clear();
					
					double[] array = this.waitToLinkTimes.get(entry.getKey());
					if (array == null) {
						array = new double[numSlots];
						this.waitToLinkTimes.put(entry.getKey(), array);
					}
					array[index] = averageTime;				
				}
				
				index++;
				timeSlotEnd += timeSlice;
			} else {
				List<Double> list = times.get(waitToLinkData.linkId);
				if (list == null) {
					list = new ArrayList<Double>();
					times.put(waitToLinkData.linkId, list);
				}
				list.add(waitToLinkData.waitToLinkTime);
			}
		}
		
		/*
		 * Consolidate data.
		 * Ensure that averageTime[i + 1] >= averageTime[i] - timeSlice.
		 * If e.g. an entry is available for index[i] which has a higher waiting time
		 * than the slot length but there is no entry available for index[i+1], then
		 * we have to add an entry there. Otherwise a waiting time of 0.0 would be 
		 * returned for index[i+1], which would be wrong.
		 */
		for (double[] array : waitToLinkTimes.values()) {
			double lastValue = array[0];
			for (int i = 1; i < array.length; i++) {
				double value = array[i];
				if (value + timeSlice < lastValue) {
					value = lastValue - timeSlice;
					array[i] = value;
				}
				lastValue = value;
			}
		}
	}
	
	private static final class WaitToLinkData {
		Id linkId;
		double time;
		double waitToLinkTime;
	}
}
