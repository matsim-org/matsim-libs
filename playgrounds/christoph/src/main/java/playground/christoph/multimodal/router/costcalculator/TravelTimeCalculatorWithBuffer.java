/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorWithBuffer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.multimodal.router.costcalculator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

public class TravelTimeCalculatorWithBuffer extends TravelTimeCalculator implements AgentDepartureEventHandler {

	private Map<Id, double[]> bufferedTravelTimes;
	private Set<Id> nonCarAgents = new HashSet<Id>();
	
	public TravelTimeCalculatorWithBuffer(Network network, int timeslice,
			int maxTime, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		super(network, timeslice, maxTime, ttconfigGroup);
		
		initBuffer(network);
	}

	public TravelTimeCalculatorWithBuffer(Network network, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		super(network, ttconfigGroup);
		
		initBuffer(network);
	}
		
	/*
	 * Initially use FreeSpeedTravelTimes
	 */
	private void initBuffer(Network network)
	{			
		int timeSlice = this.getTimeSlice();
		int numSlots = this.getNumSlots();
		
		bufferedTravelTimes = new ConcurrentHashMap<Id, double[]>();
		
		for (Link link : network.getLinks().values())
		{
			double[] travelTimeArray = new double[numSlots];
			
			int time = 0;
			for (int i = 0; i < numSlots; i++)
			{
				travelTimeArray[i] = link.getLength() / link.getFreespeed(time);
				time = time + timeSlice;
			}
			
			bufferedTravelTimes.put(link.getId(), travelTimeArray);
		}
	}
	
	private void updateBufferedTravelTimes(int iteration) {
		if (iteration == 0) return;	// before the first Iteration -> nothing to do
		
		for (Entry<Id, double[]> entry : bufferedTravelTimes.entrySet())
		{			
			double[] travelTimeArray = entry.getValue();

			int time = 0;
			for (int i = 0; i < this.getNumSlots(); i++)
			{
				travelTimeArray[i] = this.getLinkTravelTime(entry.getKey(), time);
				time = time + this.getTimeSlice();
			}
		}
	}
	
	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.getTimeSlice();
		if (slice >= this.getNumSlots()) slice = this.getNumSlots() - 1;
		return slice;
	}
	
	public double getBufferedLinkTravelTime(Link link, double time) {
		double[] travelTimeArray = bufferedTravelTimes.get(link.getId());
		
		int slice = getTimeSlotIndex(time);
		
		return travelTimeArray[slice];
	}
	
	/*
	 * Before resetting the data, we copy them to the buffer.
	 */
	@Override
	public void reset(int iteration) {
		updateBufferedTravelTimes(iteration);
		nonCarAgents = new HashSet<Id>();
		
		super.reset(iteration);
	}

	/*
	 * We only want to calculate traveltimes of car legs. Therefore we collect
	 * all agents that perform non-car legs.
	 */
	public void handleEvent(AgentDepartureEvent event) {
		if (!event.getLegMode().equals(TransportMode.car)) nonCarAgents.add(event.getPersonId()); 
	}
	
	/*
	 * We try to remove the agent from the nonCarAgents List (if it was a carAgent,
	 * nothing will happen).
	 */
	@Override
	public void handleEvent(final AgentArrivalEvent event) {
		nonCarAgents.remove(event.getPersonId());
		super.handleEvent(event);
	}
	
	/*
	 * If it is an agent with a car leg, we pass the event to the superclass.
	 */
	@Override
	public void handleEvent(final LinkEnterEvent e) {
		if (!nonCarAgents.contains(e.getPersonId())) super.handleEvent(e);
	}
	
	/*
	 * If it is an agent with a car leg, we pass the event to the superclass.
	 */
	@Override
	public void handleEvent(final LinkLeaveEvent e) {
		if (!nonCarAgents.contains(e.getPersonId())) super.handleEvent(e);
	}
	
	/*
	 * We try to remove the agent from the nonCarAgents List and pass the
	 * event then to the superclass.
	 */
	@Override
	public void handleEvent(AgentStuckEvent event) {
		nonCarAgents.remove(event.getPersonId());
		super.handleEvent(event);
	}
}
