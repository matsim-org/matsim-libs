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
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.scenario.ScenarioImpl;

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
public class SelectHouseholdMeetingPoint implements SimulationInitializedListener {

	private Scenario scenario;
	private HouseholdsUtils householdsUtils;
	private Geometry affectedArea;
		
	private GeometryFactory factory;
	
	public SelectHouseholdMeetingPoint(Scenario scenario, HouseholdsUtils householdsUtils, Geometry affectedArea) {
		this.scenario = scenario;
		this.householdsUtils = householdsUtils;
		this.affectedArea = affectedArea;
		this.factory = new GeometryFactory();
	}
	
	public void setHomeFacilitySecurity(HouseholdInfo householdInfo) {
		Id homeFacilityId = householdInfo.getMeetingPointId();
		Coord homeFacilityCoord = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(homeFacilityId).getCoord();
		householdInfo.setHomeLocationIsSecure(isCoordinateSecure(homeFacilityCoord));
	}
	
	/*
	 * Decide where a household is going to meet. This can be either
	 * the home location or an arbitrary other location if the home
	 * location is not treated to be secure.
	 */
	public void selectMeetingPoint(HouseholdInfo householdInfo) {
		// TODO
	}
	
//	public void selectSecureMeetingPoint(HouseholdInfo householdInfo) {
//		
//	}

	/*
	 *  At the moment, there is only a single rescue facility.
	 *  Instead, multiple *real* rescue facilities could be defined.
	 */
	public void selectRescueMeetingPoint(HouseholdInfo householdInfo) {
		householdInfo.setMeetingPointId(scenario.createId("rescueFacility"));
	}
	
	/*
	 * Decides whether the given coordinate is located in the affected area.
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
	
//	@Override
//	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
//		Map<Id, HouseholdInfo> joinedHouseholds = householdsUtils.getJoinedHouseholds();
//		for (HouseholdInfo householdInfo : joinedHouseholds.values()) {
//			householdInfo.isHomeLocationSecure();
//		}
//	}
}
