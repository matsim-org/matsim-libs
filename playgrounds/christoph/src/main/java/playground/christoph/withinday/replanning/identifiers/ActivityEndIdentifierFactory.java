/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityEndIdentifierFactory.java
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

package playground.christoph.withinday.replanning.identifiers;

import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;
import playground.christoph.withinday.replanning.identifiers.tools.ActivityReplanningMap;

public class ActivityEndIdentifierFactory implements DuringActivityIdentifierFactory {

	private ActivityReplanningMap activityReplanningMap;
	
	public ActivityEndIdentifierFactory(ActivityReplanningMap activityReplanningMap) {
		this.activityReplanningMap = activityReplanningMap;
	}
	
	@Override
	public DuringActivityIdentifier createIdentifier() {
		DuringActivityIdentifier identifier = new ActivityEndIdentifier(activityReplanningMap);
		identifier.setIdentifierFactory(this);
		return identifier;
	}

}
