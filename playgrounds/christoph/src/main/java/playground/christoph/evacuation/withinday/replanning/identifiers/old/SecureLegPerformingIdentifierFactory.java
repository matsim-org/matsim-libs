/* *********************************************************************** *
 * project: org.matsim.*
 * SecureLegPerformingIdentifierFactory.java
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

public class SecureLegPerformingIdentifierFactory extends DuringLegIdentifierFactory {

	private final LinkReplanningMap linkReplanningMap;
	private final MobsimDataProvider mobsimDataProvider;
	private final Coord centerCoord;
	private final double secureDistance;
	private final Network network;
	
	public SecureLegPerformingIdentifierFactory(LinkReplanningMap linkReplanningMap, MobsimDataProvider mobsimDataProvider, 
			Network network, Coord centerCoord, double secureDistance) {
		this.linkReplanningMap = linkReplanningMap;
		this.mobsimDataProvider = mobsimDataProvider;
		this.network = network;
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
	}
	
	@Override
	public DuringLegIdentifier createIdentifier() {
		DuringLegIdentifier identifier = new SecureLegPerformingIdentifier(this.linkReplanningMap, this.mobsimDataProvider, 
				this.network, this.centerCoord, this.secureDistance);
		identifier.setAgentSelectorFactory(this);
		this.addAgentFiltersToIdentifier(identifier);
		return identifier;
	}

}
