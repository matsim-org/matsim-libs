/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayDuringActivityReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.replanning.replanners.interfaces;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;

import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

/*
 * Replans only Agents that are currently performing an Activity.
 */
public abstract class WithinDayDuringActivityReplanner extends WithinDayReplanner<DuringActivityIdentifier> {

	public WithinDayDuringActivityReplanner(Id id, Scenario scenario) {
		super(id, scenario);
	}
	
}
