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

import org.matsim.api.core.v01.Scenario;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;

public class JoinedHouseholdsIdentifierFactory implements DuringActivityIdentifierFactory {

	private final Scenario scenario;
	private final SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	private final CoordAnalyzer coordAnalyzer;
	private final VehiclesTracker vehiclesTracker;
	private final HouseholdsTracker householdsTracker;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	
	public JoinedHouseholdsIdentifierFactory(Scenario scenario,SelectHouseholdMeetingPoint selectHouseholdMeetingPoint, 
			CoordAnalyzer coordAnalyzer, VehiclesTracker vehiclesTracker, HouseholdsTracker householdsTracker,
			ModeAvailabilityChecker modeAvailabilityChecker) {
		this.scenario = scenario;
		this.selectHouseholdMeetingPoint = selectHouseholdMeetingPoint;
		this.coordAnalyzer = coordAnalyzer;
		this.vehiclesTracker = vehiclesTracker;
		this.householdsTracker = householdsTracker;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
	}
	
	@Override
	public DuringActivityIdentifier createIdentifier() {
		DuringActivityIdentifier identifier = new JoinedHouseholdsIdentifier(scenario, selectHouseholdMeetingPoint, 
				coordAnalyzer.createInstance(), vehiclesTracker, householdsTracker, modeAvailabilityChecker.createInstance());
		identifier.setIdentifierFactory(this);
		return identifier;
	}

}
