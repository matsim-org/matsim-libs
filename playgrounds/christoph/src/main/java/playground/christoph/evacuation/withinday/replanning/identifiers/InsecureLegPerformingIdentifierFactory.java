/* *********************************************************************** *
 * project: org.matsim.*
 * InsecureLegPerformingIdentifierFactory.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;

import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import playground.christoph.withinday.replanning.identifiers.tools.LinkReplanningMap;

public class InsecureLegPerformingIdentifierFactory implements DuringLegIdentifierFactory {

	private LinkReplanningMap linkReplanningMap;
	private Coord centerCoord;
	private double secureDistance;
	private Network network;
	
	public InsecureLegPerformingIdentifierFactory(LinkReplanningMap linkReplanningMap, Network network, Coord centerCoord, double secureDistance) {
		this.linkReplanningMap = linkReplanningMap;
		this.network = network;
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
	}
	
	@Override
	public DuringLegIdentifier createIdentifier() {
		DuringLegIdentifier identifier = new InsecureLegPerformingIdentifier(linkReplanningMap, network, centerCoord, secureDistance);
		identifier.setIdentifierFactory(this);
		return identifier;
	}

}
