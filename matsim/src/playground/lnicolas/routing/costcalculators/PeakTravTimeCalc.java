/* *********************************************************************** *
 * project: org.matsim.*
 * PeakTravTimeCalc.java
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

package playground.lnicolas.routing.costcalculators;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicLinkSetI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelMinCostI;
import org.matsim.router.util.TravelTimeI;

public class PeakTravTimeCalc implements TravelCostI, TravelTimeI, TravelMinCostI {
	private int[] travTimeDelta;
	private double[] amplFactor;
	private double[] travTimeFactor;
	
	private double[] trafficLoad;
	
	private int timeslice;
	private int minLinkId;

	public PeakTravTimeCalc(NetworkLayer network) {
		this(network, 15*60);	// default timeslot-duration: 15 minutes
	}
	
	public PeakTravTimeCalc(NetworkLayer network, int timeslice) {
		this.timeslice = timeslice;
		init(network);
	}

	private void init(NetworkLayer network) {
		this.minLinkId = Integer.MAX_VALUE;
		int maxId = Integer.MIN_VALUE;
		BasicLinkSetI links = network.getLinks();
		for (Object obj : links) {
			Link link = (Link)obj;
			int linkId = Integer.parseInt(link.getId().toString());
			if (linkId < minLinkId) {
				minLinkId = linkId;
			}
			if (linkId > maxId) {
				maxId = linkId;
			}
		}
		travTimeDelta = new int[maxId - minLinkId + 2];
		amplFactor = new double[maxId - minLinkId + 2];
		travTimeFactor = new double[maxId - minLinkId + 2];
		
		Gbl.random.nextDouble(); // draw one because of strange "not-randomness" in the first draw...
		
		// add some small random value to each link delta
		for (Object obj : links) {
			Link link = (Link)obj;
			int index = Integer.parseInt(link.getId().toString()) - minLinkId;
			
			amplFactor[index] = (Gbl.random.nextDouble() * 0.1) + 1;
			travTimeDelta[index] = (int)(Gbl.random.nextDouble() * 5);
			travTimeFactor[index] = Gbl.random.nextDouble() + 1;
		}
		
		// Init trafficLoad
		initTrafficLoad(2, 1.0);
		
//		for (Object obj : links) {
//			Link link = (Link) obj;
//			for (int i = 1; i < trafficLoad.length; i++) {
//				if (getLinkMinimumTravelCost(link) * trafficLoad[i]
//						* amplFactor[link.getID()- minLinkId] < getLinkMinimumTravelCost(link)) {
//					System.out.println("link " + link.getID() + 
//							",timeslice=" + i + ": amplFac=" +
//							amplFactor[link.getID()- minLinkId] + ",trafficLoad="
//							+ trafficLoad[i]);
//				}
//			}
//		}
	}

	private void initTrafficLoad(int peakCount, double peakFactor) {
		trafficLoad = new double[60*60*24/timeslice];
		int interval = trafficLoad.length / (peakCount*2);
		double delta = peakFactor / interval;
		int sign = 1;
		
		trafficLoad[0] = 1;
		for (int i = 1; i < trafficLoad.length; i++) {
			double nextValue = trafficLoad[i-1] + (delta*sign);
			if (nextValue < 1 || nextValue > peakFactor + 1) {
				sign *= -1;
				nextValue = trafficLoad[i-1] + (delta*sign);
			}
			trafficLoad[i] = trafficLoad[i-1] + (delta*sign);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.network.TravelCostI#getLinkTravelCost(org.matsim.network.Link, int)
	 */
	public double getLinkTravelCost(Link link, double time) {
		int linkId = Integer.parseInt(link.getId().toString());
		int timeSlice = ((int) (time / timeslice)
				+ travTimeDelta[linkId - minLinkId])
				% trafficLoad.length;
		return getLinkMinimumTravelCost(link) * trafficLoad[timeSlice]
				* amplFactor[linkId - minLinkId];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.network.TravelCostI#getLinkTravelTime(org.matsim.network.Link,
	 *      int)
	 */
	public double getLinkTravelTime(Link link, double time) {
		int linkId = Integer.parseInt(link.getId().toString());
		return getLinkMinimumTravelCost(link)
			* travTimeFactor[linkId- minLinkId];
	}

	public double getLinkMinimumTravelCost(Link link) {
		return (link.getLength() / link.getFreespeed());
	}
}
