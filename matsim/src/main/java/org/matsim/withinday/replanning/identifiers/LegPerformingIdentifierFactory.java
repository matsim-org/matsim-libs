/* *********************************************************************** *
 * project: org.matsim.*
 * LegPerformingIdentifierFactory.java
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

package org.matsim.withinday.replanning.identifiers;

import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

public class LegPerformingIdentifierFactory extends DuringLegIdentifierFactory {

	private final LinkReplanningMap linkReplanningMap;
	private final MobsimDataProvider mobsimDataProvider;	
	
	public LegPerformingIdentifierFactory(LinkReplanningMap linkReplanningMap, MobsimDataProvider mobsimDataProvider) {
		this.linkReplanningMap = linkReplanningMap;
		this.mobsimDataProvider = mobsimDataProvider;
	}
	
	@Override
	public DuringLegAgentSelector createIdentifier() {
		DuringLegAgentSelector identifier = new LegPerformingIdentifier(this.linkReplanningMap, this.mobsimDataProvider);
		this.addAgentFiltersToIdentifier(identifier);
		identifier.setAgentSelectorFactory(this);
		return identifier;
	}

}
