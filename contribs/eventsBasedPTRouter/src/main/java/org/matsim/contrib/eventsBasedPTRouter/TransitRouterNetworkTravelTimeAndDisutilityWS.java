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

package org.matsim.contrib.eventsBasedPTRouter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.vehicles.Vehicle;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;

import java.util.HashMap;
import java.util.Map;

/**
 * TravelTime and TravelDisutility calculator to be used with the transit network used for transit routing.
 * This version considers waiting time at stops, and takes travel time between stops from a {@link StopStopTime} object.
 *
 * @author sergioo
 */
public class TransitRouterNetworkTravelTimeAndDisutilityWS extends TransitRouterNetworkTravelTimeAndDisutility implements TravelDisutility {

	private Link previousLink;
	private double previousTime;
	private double cachedLinkTime;
	private final Map<Id, double[]> linkTravelTimes = new HashMap<Id, double[]>();
	private final Map<Id, double[]> linkWaitingTimes = new HashMap<Id, double[]>();
	private final int numSlots;
	private final double timeSlot;

	public TransitRouterNetworkTravelTimeAndDisutilityWS(final TransitRouterConfig config, TransitRouterNetworkWW routerNetwork, WaitTime waitTime, StopStopTime stopStopTime, TravelTimeCalculatorConfigGroup tTConfigGroup, QSimConfigGroup qSimConfigGroup, PreparedTransitSchedule preparedTransitSchedule) {
		this(config, routerNetwork, waitTime, stopStopTime, tTConfigGroup, qSimConfigGroup.getStartTime(), qSimConfigGroup.getEndTime(), preparedTransitSchedule);
	}
	public TransitRouterNetworkTravelTimeAndDisutilityWS(final TransitRouterConfig config, TransitRouterNetworkWW routerNetwork, WaitTime waitTime, StopStopTime stopStopTime, TravelTimeCalculatorConfigGroup tTConfigGroup, double startTime, double endTime, PreparedTransitSchedule preparedTransitSchedule) {
		super(config, preparedTransitSchedule);
		timeSlot = tTConfigGroup.getTraveltimeBinSize();
		numSlots = (int) ((endTime-startTime)/timeSlot);
		for(TransitRouterNetworkWW.TransitRouterNetworkLink link:routerNetwork.getLinks().values())
			if(link.route!=null) {
				double[] times = new double[numSlots];
				for(int slot = 0; slot<numSlots; slot++)
					times[slot] = stopStopTime.getStopStopTime(link.fromNode.stop.getStopFacility().getId(), link.toNode.stop.getStopFacility().getId(), startTime+slot*timeSlot);
				linkTravelTimes.put(link.getId(), times);
			}
			else if(link.toNode.route!=null) {
				double[] times = new double[numSlots];
				for(int slot = 0; slot<numSlots; slot++)
					times[slot] = waitTime.getRouteStopWaitTime(link.toNode.line.getId(), link.toNode.route.getId(), link.fromNode.stop.getStopFacility().getId(), startTime+slot*timeSlot);
				linkWaitingTimes.put(link.getId(), times);
			}
	}
	@Override
	public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
		previousLink = link;
		previousTime = time;
		TransitRouterNetworkWW.TransitRouterNetworkLink wrapped = (TransitRouterNetworkWW.TransitRouterNetworkLink) link;
		if (wrapped.route!=null)
			//in line link
			cachedLinkTime = linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)];
		else if(wrapped.toNode.route!=null)
			//wait link
			cachedLinkTime = linkWaitingTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)];
		else if(wrapped.fromNode.route==null)
			//walking link
			cachedLinkTime = wrapped.getLength()/this.config.getBeelineWalkSpeed();
		else
			//inside link
			cachedLinkTime = 0;
		return cachedLinkTime;
	}
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
		boolean cachedTravelDisutility = false; 
		if(previousLink==link && previousTime==time)
			cachedTravelDisutility = true;
		TransitRouterNetworkWW.TransitRouterNetworkLink wrapped = (TransitRouterNetworkWW.TransitRouterNetworkLink) link;
		if (wrapped.route != null)
			return -(cachedTravelDisutility?cachedLinkTime:linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)])*this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
					- link.getLength() * (this.config.getMarginalUtilityOfTravelDistancePt_utl_m());
		else if (wrapped.toNode.route!=null)
			// it's a wait link
			return -(cachedTravelDisutility?cachedLinkTime:linkWaitingTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)])*this.config.getMarginalUtilityOfWaitingPt_utl_s()
					- this.config.getUtilityOfLineSwitch_utl();
		else if(wrapped.fromNode.route==null)
			// it's a transfer link (walk)
			return -(cachedTravelDisutility?cachedLinkTime:wrapped.getLength()/this.config.getBeelineWalkSpeed())*this.config.getMarginalUtilityOfTravelTimeWalk_utl_s();
		else
			//inside link
			return 0;
	}
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		TransitRouterNetworkWW.TransitRouterNetworkLink wrapped = (TransitRouterNetworkWW.TransitRouterNetworkLink) link;
		if (wrapped.route != null)
			return - linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)]*this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
					- link.getLength() * (this.config.getMarginalUtilityOfTravelDistancePt_utl_m());
		else if (wrapped.toNode.route!=null)
			// it's a wait link
			return - linkWaitingTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)]*this.config.getMarginalUtilityOfWaitingPt_utl_s()
					- this.config.getUtilityOfLineSwitch_utl();
		else if(wrapped.fromNode.route==null)
			// it's a transfer link (walk)
			return -wrapped.getLength()/this.config.getBeelineWalkSpeed()*this.config.getMarginalUtilityOfTravelTimeWalk_utl_s();
		else
			//inside link
			return 0;
	}
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}

}
