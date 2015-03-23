/* *********************************************************************** *
 * project: org.matsim.*
 * InsecureActivityPerformingIdentifierFactory.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers.old;

import org.matsim.api.core.v01.Coord;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

public class InsecureActivityPerformingIdentifierFactory extends DuringActivityIdentifierFactory {

	private final ActivityReplanningMap activityReplanningMap;
	private final MobsimDataProvider mobsimDataProvider;
	private final Coord centerCoord;
	private final double secureDistance;
	
	public InsecureActivityPerformingIdentifierFactory(ActivityReplanningMap activityReplanningMap, 
			MobsimDataProvider mobsimDataProvider, Coord centerCoord, double secureDistance) {
		this.activityReplanningMap = activityReplanningMap;
		this.mobsimDataProvider = mobsimDataProvider;
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
	}
	
	@Override
	public DuringActivityIdentifier createIdentifier() {
		DuringActivityIdentifier identifier = new InsecureActivityPerformingIdentifier(this.activityReplanningMap, 
				this.mobsimDataProvider, this.centerCoord, this.secureDistance);
		identifier.setAgentSelectorFactory(this);
		this.addAgentFiltersToIdentifier(identifier);
		return identifier;
	}

}
