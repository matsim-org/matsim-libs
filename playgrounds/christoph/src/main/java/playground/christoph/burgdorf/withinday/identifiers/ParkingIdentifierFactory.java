/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingIdentifierFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.burgdorf.withinday.identifiers;

import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifier;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

public class ParkingIdentifierFactory extends DuringLegIdentifierFactory {

	private final LeaveLinkIdentifierFactory leaveLinkIdentifierFactory;
	
	public ParkingIdentifierFactory(LinkReplanningMap linkReplanningMap, MobsimDataProvider mobsimDataProvider) {
		this.leaveLinkIdentifierFactory = new LeaveLinkIdentifierFactory(linkReplanningMap, mobsimDataProvider);
	}
	
	@Override
	public DuringLegAgentSelector createIdentifier() {
		/*
		 * Here, the leaveLinkIdentifier has to do apply the filters. Therefore,
		 * add them to it instead of to the  ParkingIdentifier. The later one only
		 * selects a parking for all identified agents.
		 */
		LeaveLinkIdentifier leaveLinkIdentifier = (LeaveLinkIdentifier) leaveLinkIdentifierFactory.createIdentifier();
		this.addAgentFiltersToIdentifier(leaveLinkIdentifier);
		
		DuringLegAgentSelector identifier = new ParkingIdentifier(leaveLinkIdentifier);
		identifier.setAgentSelectorFactory(this);
		return identifier;
	}

}