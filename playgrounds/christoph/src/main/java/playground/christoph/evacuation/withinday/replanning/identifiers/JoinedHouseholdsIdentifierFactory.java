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

import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;

import playground.christoph.evacuation.withinday.replanning.utils.HouseholdsUtils;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;

public class JoinedHouseholdsIdentifierFactory implements DuringActivityIdentifierFactory {

	private HouseholdsUtils householdUtils;
	private SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	
	public JoinedHouseholdsIdentifierFactory(HouseholdsUtils householdUtils, SelectHouseholdMeetingPoint selectHouseholdMeetingPoint) {
		this.householdUtils = householdUtils;
		this.selectHouseholdMeetingPoint = selectHouseholdMeetingPoint;
	}
	
	@Override
	public DuringActivityIdentifier createIdentifier() {
		DuringActivityIdentifier identifier = new JoinedHouseholdsIdentifier(householdUtils, selectHouseholdMeetingPoint);
		identifier.setIdentifierFactory(this);
		return identifier;
	}

}
