/* *********************************************************************** *
 * project: org.matsim.*
 * JoinedHouseholdsIdentifierFactory.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import org.matsim.vehicles.Vehicles;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;

import playground.christoph.evacuation.mobsim.PassengerTracker;
import playground.christoph.evacuation.withinday.replanning.utils.HouseholdsUtils;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;

public class JoinedHouseholdsIdentifierFactory implements DuringActivityIdentifierFactory {

	private final Vehicles vehicles;
	private final HouseholdsUtils householdUtils;
	private final SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final PassengerTracker passengerTracker;
	
	public JoinedHouseholdsIdentifierFactory(Vehicles vehicles, HouseholdsUtils householdUtils, 
			SelectHouseholdMeetingPoint selectHouseholdMeetingPoint, ModeAvailabilityChecker modeAvailabilityChecker, 
			PassengerTracker passengerTracker) {
		this.vehicles = vehicles;
		this.householdUtils = householdUtils;
		this.selectHouseholdMeetingPoint = selectHouseholdMeetingPoint;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.passengerTracker = passengerTracker;
	}
	
	@Override
	public DuringActivityIdentifier createIdentifier() {
		DuringActivityIdentifier identifier = new JoinedHouseholdsIdentifier(vehicles, householdUtils, selectHouseholdMeetingPoint, 
				modeAvailabilityChecker, passengerTracker);
		identifier.setIdentifierFactory(this);
		return identifier;
	}

}
