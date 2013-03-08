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

package playground.sergioo.singapore2012.transitRouterVariable;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterNetworkWW.TransitRouterNetworkLink;

/**
 * TravelTime and TravelDisutility calculator to be used with the transit network used for transit routing.
 *
 * @author sergioo
 */
public class TransitRouterNetworkTravelTimeAndDisutilityVariableWW extends TransitRouterNetworkTravelTimeAndDisutility implements TravelDisutility {
	
	private Network network;
	private final TravelTime travelTime;
	private final WaitTime waitTime;
	private final Map<Id, double[]> linkTravelTimes = new HashMap<Id, double[]>();
	private final double timeSlot;
	private final int numSlots;
	
	public TransitRouterNetworkTravelTimeAndDisutilityVariableWW(final TransitRouterConfig config, Network network, TransitRouterNetworkWW routerNetwork, TravelTime travelTime, WaitTime waitTime, TravelTimeCalculatorConfigGroup tTConfigGroup, QSimConfigGroup qSimConfigGroup) {
		super(config);
		this.network = network;
		this.travelTime = travelTime;
		this.waitTime = waitTime;
		timeSlot = tTConfigGroup.getTraveltimeBinSize();
		numSlots = (int) ((qSimConfigGroup.getEndTime()-qSimConfigGroup.getStartTime())/timeSlot);
		for(TransitRouterNetworkLink link:routerNetwork.getLinks().values())
			if(link.route!=null)
				linkTravelTimes.put(link.getId(), new double[numSlots]);
	}
	public TransitRouterNetworkTravelTimeAndDisutilityVariableWW(final TransitRouterConfig config, Network network, TransitRouterNetworkWW routerNetwork, TravelTime travelTime, WaitTime waitTime, TravelTimeCalculatorConfigGroup tTConfigGroup, double startTime, double endTime) {
		super(config);
		this.network = network;
		this.travelTime = travelTime;
		this.waitTime = waitTime;
		timeSlot = tTConfigGroup.getTraveltimeBinSize();
		numSlots = (int) ((endTime-startTime)/timeSlot);
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
				for(Id linkId:wrapped.route.getRoute().getSubRoute(wrapped.fromNode.stop.getStopFacility().getLinkId(), wrapped.toNode.stop.getStopFacility().getLinkId()).getLinkIds())
					linksTime += travelTime.getLinkTravelTime(network.getLinks().get(linkId), time, person, vehicle);
				linksTime += travelTime.getLinkTravelTime(network.getLinks().get(wrapped.toNode.stop.getStopFacility().getLinkId()), time, person, vehicle);
				linkTravelTimes.get(wrapped.getId())[slot] = linksTime;
			}
			return linksTime;
		}
		else if(wrapped.toNode.route==null)
			//transfer link
			return wrapped.getLength()/this.config.getBeelineWalkSpeed();
		else if(wrapped.fromNode.route==null)
			//wait link
			return waitTime.getRouteStopWaitTime(wrapped.toNode.line.getId(), wrapped.toNode.route.getId(), wrapped.fromNode.stop.getStopFacility().getId(), time);
		else
			throw new RuntimeException("Bad transit router link");
	}
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
		double disutility;
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route != null)
			disutility = - getLinkTravelTime(link, time, person, vehicle) * this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
				       - link.getLength() * (this.config.getMarginalUtilityOfTravelDistancePt_utl_m()-2.7726/100000);
		else if(wrapped.toNode.route==null)
			// it's a transfer link (walk)
			disutility = -getLinkTravelTime(link, time, person, vehicle) * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s();
		else if (wrapped.fromNode.route==null)
			// it's a wait link
			disutility = - getLinkTravelTime(link, time, person, vehicle) * this.config.getMarginalUtilityOfWaitingPt_utl_s()
					- this.config.getUtilityOfLineSwitch_utl();
		else
			throw new RuntimeException("Bad transit router link");
		return disutility;
	}
	public void reset() {
		for(double[] travelTimes:linkTravelTimes.values())
			for(int i=0; i<travelTimes.length; i++)
				travelTimes[i] = 0;
	}
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		double disutility;
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route != null)
			disutility = - getLinkTravelTime(link, time, person, vehicle) * this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
				       - link.getLength() * (this.config.getMarginalUtilityOfTravelDistancePt_utl_m()-2.7726/100000);
		else if(wrapped.toNode.route==null)
			// it's a transfer link (walk)
			disutility = -getLinkTravelTime(link, time, person, vehicle) * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s();
		else if (wrapped.fromNode.route==null)
			// it's a wait link
			disutility = - getLinkTravelTime(link, time, person, vehicle) * this.config.getMarginalUtilityOfWaitingPt_utl_s()
					- this.config.getUtilityOfLineSwitch_utl();
		else
			throw new RuntimeException("Bad transit router link");
		return disutility;
	}
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}

}
