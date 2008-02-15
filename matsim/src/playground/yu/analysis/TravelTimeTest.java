/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeTest.java
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

/**
 * 
 */
package playground.yu.analysis;

import java.util.HashMap;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventAgentStuck;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;

/**
 * @author ychen
 * 
 */
public class TravelTimeTest implements EventHandlerAgentDepartureI,
		EventHandlerAgentArrivalI, EventHandlerAgentStuckI {
	private final NetworkLayer network;
	private final Plans plans;
	private int binSize;
	private double[] travelTimes;
	private int[] arrCount;
	/**
	 * @param arg0 -
	 *            String agentId
	 * @param arg1 -
	 *            Double departure time
	 */
	private HashMap<String, Double> tmpDptTimes = new HashMap<String, Double>();

	/**
	 * 
	 */
	public TravelTimeTest(final int binSize, final int nofBins,
			NetworkLayer network, Plans plans) {
		this.network = network;
		this.plans = plans;
		this.binSize = binSize;
		travelTimes = new double[nofBins + 1];
		arrCount = new int[nofBins + 1];
	}

	public TravelTimeTest(final int binSize, NetworkLayer network, Plans plans) {
		this(binSize, 30 * 3600 / binSize + 1, network, plans);
	}

	public void handleEvent(EventAgentDeparture event) {
		tmpDptTimes.put(event.agentId, event.time);
	}

	public void reset(int iteration) {
		tmpDptTimes.clear();
	}

	public void handleEvent(EventAgentArrival event) {
		double time = event.time;
		Double dptTime = tmpDptTimes.get(event.agentId);
		if (dptTime != null) {
			int binIdx=getBinIndex(time);
			double travelTime = time - dptTime;
			travelTimes[binIdx] += travelTime;
			arrCount[binIdx]++;
		}
	}

	public void handleEvent(EventAgentStuck event) {
	}

	private int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= travelTimes.length) {
			return travelTimes.length - 1;
		}
		return bin;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
