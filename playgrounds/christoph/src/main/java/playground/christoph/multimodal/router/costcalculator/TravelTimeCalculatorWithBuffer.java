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

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

public class TravelTimeCalculatorWithBuffer extends TravelTimeCalculator {

	private Map<Id, double[]> bufferedTravelTimes;
	
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

		super.reset(iteration);
	}
}
