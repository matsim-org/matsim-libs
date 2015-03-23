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
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;

import playground.christoph.evacuation.mobsim.HouseholdDepartureManager;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;

public class JoinedHouseholdsIdentifierFactory extends DuringActivityIdentifierFactory {

	private final Scenario scenario;
	private final SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	private final MobsimDataProvider mobsimDataProvider;
	private final HouseholdDepartureManager householdDepartureManager;
	
	public JoinedHouseholdsIdentifierFactory(Scenario scenario, SelectHouseholdMeetingPoint selectHouseholdMeetingPoint, 
			ModeAvailabilityChecker modeAvailabilityChecker, JointDepartureOrganizer jointDepartureOrganizer,
			MobsimDataProvider mobsimDataProvider, HouseholdDepartureManager householdDepartureManager) {
		this.scenario = scenario;
		this.selectHouseholdMeetingPoint = selectHouseholdMeetingPoint;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
		this.mobsimDataProvider = mobsimDataProvider;
		this.householdDepartureManager = householdDepartureManager;
	}

	@Override
	public DuringActivityIdentifier createIdentifier() {
		DuringActivityIdentifier identifier = new JoinedHouseholdsIdentifier(this.scenario, this.selectHouseholdMeetingPoint, 
				this.modeAvailabilityChecker.createInstance(), this.jointDepartureOrganizer, this.mobsimDataProvider,
				this.householdDepartureManager);
		this.addAgentFiltersToIdentifier(identifier);
		identifier.setAgentSelectorFactory(this);
		return identifier;
	}

}
