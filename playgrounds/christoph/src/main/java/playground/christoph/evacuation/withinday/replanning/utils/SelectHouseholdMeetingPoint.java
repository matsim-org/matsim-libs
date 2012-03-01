/* *********************************************************************** *
 * project: org.matsim.*
 * SelectHouseholdMeetingPoint.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.utils.misc.Time;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;

/**
 * Decides where a household will meet after the evacuation order has been given.
 * This could be either at home or at another location, if the home location is
 * not treated to be secure. However, households might meet at their insecure home
 * location and then evacuate as a unit.
 * 
 * By default, all households meet at home and the select another location, if
 * their home location is not secure.
 * 
 * @author cdobler
 */
public class SelectHouseholdMeetingPoint implements SimulationInitializedListener, SimulationBeforeSimStepListener {

	private final Scenario scenario;
	private final HouseholdsTracker householdsTracker;
	private final CoordAnalyzer coordAnalyzer;
	
	private double time = Time.UNDEFINED_TIME;
	
	public SelectHouseholdMeetingPoint(Scenario scenario, HouseholdsTracker householdsTracker, CoordAnalyzer coordAnalyzer) {
		this.scenario = scenario;
		this.householdsTracker = householdsTracker;
		this.coordAnalyzer = coordAnalyzer;
	}
	
	/*
	 *  At the moment, there is only a single rescue facility.
	 *  Instead, multiple *real* rescue facilities could be defined.
	 */
	public Id selectRescueMeetingPoint(Id householdId) {
//		HouseholdInfo householdInfo = householdsUtils.getHouseholdInfoMap().get(householdId);
//		Id oldMeetingPointId = householdInfo.getMeetingPointId();
		Id newMeetingPointId = scenario.createId("rescueFacility");

//		/*
//		 * If the meeting point is not changed we have nothing to do.
//		 */
//		if (oldMeetingPointId == newMeetingPointId) return;
//		
//		/*
//		 * If the household is currently joined at the old meeting point and
//		 * a new meeting point is set.
//		 */
//		if (householdInfo.allMembersAtMeetingPoint()) {
//			Event leaveEvent = new HouseholdLeaveMeetingPointEventImpl(this.time, householdId, newMeetingPointId);
//			eventsManager.processEvent(leaveEvent);	
//		}
//			
//		householdsUtils.setMeetingPoint(householdId, newMeetingPointId);
//		
//		/*
//		 * If the household is currently joined at the new meeting point.
//		 */
//		if (householdInfo.allMembersAtMeetingPoint()) {
//			Event enterEvent = new HouseholdEnterMeetingPointEventImpl(this.time, householdId, newMeetingPointId);
//			eventsManager.processEvent(enterEvent);	
//		}
//		
//		Event setEvent = new HouseholdSetMeetingPointEventImpl(this.time, householdId, newMeetingPointId);
//		eventsManager.processEvent(setEvent);
		
		return newMeetingPointId;
	}

	/*
	 * Check, whether the home location of a household is secure or not.
	 */
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
//		for (HouseholdInfo householdInfo : householdsUtils.getHouseholdInfoMap().values()) {
//			setHomeFacilitySecurity(householdInfo);
//		}
	}

	/*
	 * Get the actual simulation time.
	 */
	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		this.time = e.getSimulationTime();
	}
	
}
