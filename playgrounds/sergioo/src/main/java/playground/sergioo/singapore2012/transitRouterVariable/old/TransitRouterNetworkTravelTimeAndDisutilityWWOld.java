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

package playground.sergioo.singapore2012.transitRouterVariable.old;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.singapore2012.transitRouterVariable.old.TransitRouterNetworkWW.TransitRouterNetworkLink;

/**
 * TravelTime and TravelDisutility calculator to be used with the transit network used for transit routing.
 *
 * @author sergioo
 */
public class TransitRouterNetworkTravelTimeAndDisutilityWWOld extends TransitRouterNetworkTravelTimeAndDisutility implements TravelDisutility {
	
	private Network network;
	private final TravelTime travelTime;
	private final WaitTimeOld waitTime;
	private final Map<Id<Link>, double[]> linkTravelTimes = new HashMap<Id<Link>, double[]>();
	private final double timeSlot;
	private final int numSlots;
	private double startTime;
	private TransitRouterNetworkWW routerNetwork;
	private Link previousLink;
	private double previousTime;
	private double cachedTravelTime;
	
	public TransitRouterNetworkTravelTimeAndDisutilityWWOld(final TransitRouterConfig config, Network network, TransitRouterNetworkWW routerNetwork, TravelTime travelTime, WaitTimeOld waitTime, TravelTimeCalculatorConfigGroup tTConfigGroup, QSimConfigGroup qSimConfigGroup, PreparedTransitSchedule preparedTransitSchedule) {
		this(config, network, routerNetwork, travelTime, waitTime, tTConfigGroup, qSimConfigGroup.getStartTime(), qSimConfigGroup.getEndTime(), preparedTransitSchedule);
	}
	public TransitRouterNetworkTravelTimeAndDisutilityWWOld(final TransitRouterConfig config, Network network, TransitRouterNetworkWW routerNetwork, TravelTime travelTime, WaitTimeOld waitTime, TravelTimeCalculatorConfigGroup tTConfigGroup, double startTime, double endTime, PreparedTransitSchedule preparedTransitSchedule) {
		super(config, preparedTransitSchedule);
		this.network = network;
		this.travelTime = travelTime;
		this.waitTime = waitTime;
		this.startTime = startTime;
		timeSlot = tTConfigGroup.getTraveltimeBinSize();
		numSlots = (int) ((endTime-startTime)/timeSlot);
		this.routerNetwork = routerNetwork;
		initTravelTimes();
	}
	void initTravelTimes() {
		for(TransitRouterNetworkLink link:routerNetwork.getLinks().values())
			if(link.route!=null) {
				linkTravelTimes.put(link.getId(), new double[numSlots]);
				for(int slot = 0; slot<numSlots; slot++) {
					double linksTime = travelTime.getLinkTravelTime(network.getLinks().get(link.fromNode.stop.getStopFacility().getLinkId()), startTime+slot*timeSlot, null, null);
					for(Id<Link> linkId:link.route.getRoute().getSubRoute(link.fromNode.stop.getStopFacility().getLinkId(), link.toNode.stop.getStopFacility().getLinkId()).getLinkIds())
						linksTime += travelTime.getLinkTravelTime(network.getLinks().get(linkId), startTime+slot*timeSlot, null, null);
					linkTravelTimes.get(link.getId())[slot] = linksTime;
				}
			}
	}
	@Override
	public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
		previousLink = link;
		previousTime = time;
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route != null) {
			//in line link
			cachedTravelTime = linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)];
		}
		else if(wrapped.toNode.route!=null)
			//wait link
			cachedTravelTime = waitTime.getRouteStopWaitTime(wrapped.toNode.line.getId(), wrapped.toNode.route.getId(), wrapped.fromNode.stop.getStopFacility().getId(), time);
		else if(wrapped.fromNode.route==null)
			//walking link
			cachedTravelTime = wrapped.getLength()/this.config.getBeelineWalkSpeed();
		else
			//inside link
			cachedTravelTime = 0;
		return cachedTravelTime;
	}
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
		boolean cachedTravelDisutility = false; 
		if(previousLink==link && previousTime==time)
			cachedTravelDisutility = true;
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route != null)
			return -(cachedTravelDisutility?cachedTravelTime:linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)])*this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
				       - link.getLength() * (this.config.getMarginalUtilityOfTravelDistancePt_utl_m()-2.7726/100000);
		else if (wrapped.toNode.route!=null)
			// it's a wait link
			return -(cachedTravelDisutility?cachedTravelTime:waitTime.getRouteStopWaitTime(wrapped.toNode.line.getId(), wrapped.toNode.route.getId(), wrapped.fromNode.stop.getStopFacility().getId(), time))*this.config.getMarginalUtilityOfWaitingPt_utl_s()
					- this.config.getUtilityOfLineSwitch_utl();
		else if(wrapped.fromNode.route==null)
			// it's a transfer link (walk)
			return -(cachedTravelDisutility?cachedTravelTime:wrapped.getLength()/this.config.getBeelineWalkSpeed())*this.config.getMarginalUtilityOfTravelTimeWalk_utl_s();
		else
			//inside link
			return 0;
	}
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		TransitRouterNetworkLink wrapped = (TransitRouterNetworkLink) link;
		if (wrapped.route != null)
			return - linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)] * this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
				       - link.getLength() * (this.config.getMarginalUtilityOfTravelDistancePt_utl_m()-2.7726/100000);
		else if (wrapped.toNode.route!=null)
			// it's a wait link
			return - waitTime.getRouteStopWaitTime(wrapped.toNode.line.getId(), wrapped.toNode.route.getId(), wrapped.fromNode.stop.getStopFacility().getId(), time) * this.config.getMarginalUtilityOfWaitingPt_utl_s()
					- this.config.getUtilityOfLineSwitch_utl();
		else if(wrapped.fromNode.route==null)
			// it's a transfer link (walk)
			return -wrapped.getLength()/this.config.getBeelineWalkSpeed() * this.config.getMarginalUtilityOfTravelTimeWalk_utl_s();
		else
			//inside link
			return 0;
	}
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}

}
