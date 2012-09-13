/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkTravelTimeAndDisutilityVariableWW.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.sergioo.Singapore.TransitRouterVariable;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.Singapore.TransitRouterVariable.TransitRouterNetworkWW.TransitRouterNetworkLink;

/**
 * TravelTime and TravelDisutility calculator to be used with the transit network used for transit routing.
 *
 * @author sergioo
 */
public class TransitRouterNetworkTravelTimeAndDisutilityVariableWW extends TransitRouterNetworkTravelTimeAndDisutility implements IterationEndsListener {
	
	private Network network;
	private final TravelTime travelTime;
	private final WaitTime waitTime;
	private final Map<Id, double[]> linkTravelTimes = new HashMap<Id, double[]>();
	private final double timeSlot;
	private final int numSlots;
	
	public TransitRouterNetworkTravelTimeAndDisutilityVariableWW(final TransitRouterConfig config, Network network, TransitRouterNetworkWW routerNetwork, TravelTime travelTime, WaitTime waitTime) {
		super(config);
		this.network = network;
		this.travelTime = travelTime;
		this.waitTime = waitTime;
		this.timeSlot = ((TravelTimeCalculator)travelTime).getTimeSlice();
		this.numSlots = ((TravelTimeCalculator)travelTime).getNumSlots();
		for(TransitRouterNetworkLink link:routerNetwork.getLinks().values())
			if(link.route!=null)
				linkTravelTimes.put(link.getId(), new double[numSlots]);
	}
	public WaitTime getWaitTime() {
		return waitTime;
	}
	@Override
	public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route != null) {
			//in line link
			int slot = time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1);
			double linksTime = linkTravelTimes.get(wrapped.getId())[slot];
			if(linksTime==0) {
				linksTime += travelTime.getLinkTravelTime(network.getLinks().get(wrapped.fromNode.stop.getStopFacility().getLinkId()), time, person, vehicle);
				for(Id linkId:wrapped.route.getRoute().getSubRoute(wrapped.fromNode.stop.getStopFacility().getLinkId(), wrapped.toNode.stop.getStopFacility().getLinkId()).getLinkIds())
					linksTime += travelTime.getLinkTravelTime(network.getLinks().get(linkId), time, person, vehicle);
				linkTravelTimes.get(wrapped.getId())[slot] = linksTime;
			}
			return linksTime;
		}
		else if(wrapped.fromNode.route==null)
			//wait link
			return waitTime.getRouteStopWaitTime(wrapped.toNode.line.getId(), wrapped.toNode.route.getId(), wrapped.fromNode.stop.getStopFacility().getId(), time);
		else if(wrapped.toNode.route==null)
			//transfer link
			return wrapped.getLength()/this.config.getBeelineWalkSpeed();
		else
			throw new RuntimeException("Bad transit router link");
	}
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
		double cost;
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route != null)
			cost = - getLinkTravelTime(link, time, person, vehicle) * this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
				       - link.getLength() * (this.config.getMarginalUtilityOfTravelDistancePt_utl_m()-2.7726/100000);
		else if (wrapped.fromNode.route==null)
			// it's a wait link
			cost = - getLinkTravelTime(link, time, person, vehicle) * this.config.getMarginalUtiltityOfWaiting_utl_s();
		else if(wrapped.toNode.route==null)
			// it's a transfer link (walk)
			cost = -getLinkTravelTime(link, time, person, vehicle) * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s()
			       - this.config.getUtilityOfLineSwitch_utl();
		else
			throw new RuntimeException("Bad transit router link");
		return cost;
	}
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		for(double[] travelTimes:linkTravelTimes.values())
			for(int i=0; i<travelTimes.length; i++)
				travelTimes[i] = 0;
	}

}
