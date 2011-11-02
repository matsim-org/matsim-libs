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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.Time;

import playground.christoph.evacuation.events.HouseholdEnterMeetingPointEventImpl;
import playground.christoph.evacuation.events.HouseholdLeaveMeetingPointEventImpl;
import playground.christoph.evacuation.events.HouseholdSetMeetingPointEventImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

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
	private final EventsManager eventsManager;
	private final HouseholdsUtils householdsUtils;
	private final Geometry affectedArea;
	
	private final GeometryFactory factory;
	private double time = Time.UNDEFINED_TIME;
	
	public SelectHouseholdMeetingPoint(Scenario scenario, EventsManager eventsManager, HouseholdsUtils householdsUtils, Geometry affectedArea) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.householdsUtils = householdsUtils;
		this.affectedArea = affectedArea;
		
		this.factory = new GeometryFactory();
	}
	
	public void setHomeFacilitySecurity(HouseholdInfo householdInfo) {
		Id homeFacilityId = householdInfo.getMeetingPointId();
		Coord homeFacilityCoord = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(homeFacilityId).getCoord();
		householdInfo.setHomeLocationIsSecure(isCoordinateSecure(homeFacilityCoord));
	}
	
//	/*
//	 * Decide where a household is going to meet. This can be either
//	 * the home location or an arbitrary other location if the home
//	 * location is not treated to be secure.
//	 */
//	public void selectMeetingPoint(HouseholdInfo householdInfo) {
//		// TODO
//	}

	/*
	 *  At the moment, there is only a single rescue facility.
	 *  Instead, multiple *real* rescue facilities could be defined.
	 */
	public void selectRescueMeetingPoint(Id householdId) {
		HouseholdInfo householdInfo = householdsUtils.getHouseholdInfoMap().get(householdId);
		Id oldMeetingPointId = householdInfo.getMeetingPointId();
		Id newMeetingPointId = scenario.createId("rescueFacility");

		/*
		 * If the meeting point is not changed we have nothing to do.
		 */
		if (oldMeetingPointId == newMeetingPointId) return;
		
		/*
		 * If the household is currently joined at the old meeting point and
		 * a new meeting point is set.
		 */
		if (householdInfo.allMembersAtMeetingPoint()) {
			Event leaveEvent = new HouseholdLeaveMeetingPointEventImpl(this.time, householdId, newMeetingPointId);
			eventsManager.processEvent(leaveEvent);	
		}
			
		householdsUtils.setMeetingPoint(householdId, newMeetingPointId);
		
		/*
		 * If the household is currently joined at the new meeting point.
		 */
		if (householdInfo.allMembersAtMeetingPoint()) {
			Event enterEvent = new HouseholdEnterMeetingPointEventImpl(this.time, householdId, newMeetingPointId);
			eventsManager.processEvent(enterEvent);	
		}
		
		Event setEvent = new HouseholdSetMeetingPointEventImpl(this.time, householdId, newMeetingPointId);
		eventsManager.processEvent(setEvent);
	}
	
	/**
	 * Decides whether the given facility is located in the affected area.
	 * @param id Id of a facility to be checked
	 */
	public boolean isFacilitySecure(Id facilityId) {
		
		ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(facilityId);
		if (facility == null) return false;
		else {
			Point point = factory.createPoint(new Coordinate(facility.getCoord().getX(), facility.getCoord().getY()));
			return !affectedArea.contains(point);
		}
	}
	
	/**
	 * Decides whether the given coordinate is located in the affected area.
	 * @param coord Coordinate to be checked
	 */
	public boolean isCoordinateSecure(Coord coord) {
		
		Point point = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
		return !affectedArea.contains(point);
	}

	/*
	 * Check, whether the home location of a household is secure or not.
	 */
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		for (HouseholdInfo householdInfo : householdsUtils.getHouseholdInfoMap().values()) {
			setHomeFacilitySecurity(householdInfo);
		}
	}

	/*
	 * Get the actual simulation time.
	 */
	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		this.time = e.getSimulationTime();
	}
	
}
