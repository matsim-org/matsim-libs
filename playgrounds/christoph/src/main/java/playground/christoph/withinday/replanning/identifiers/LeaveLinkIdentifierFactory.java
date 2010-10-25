/* *********************************************************************** *
 * project: org.matsim.*
 * LeaveLinkIdentifierFactory.java
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

import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import playground.christoph.withinday.replanning.identifiers.tools.LinkReplanningMap;

public class LeaveLinkIdentifierFactory implements DuringLegIdentifierFactory {

	private LinkReplanningMap linkReplanningMap;
	
	public LeaveLinkIdentifierFactory(LinkReplanningMap linkReplanningMap) {
		this.linkReplanningMap = linkReplanningMap;
	}
	
	@Override
	public DuringLegIdentifier createIdentifier() {
		DuringLegIdentifier identifier = new LeaveLinkIdentifier(linkReplanningMap);
		identifier.setIdentifierFactory(this);
		return identifier;
	}

}
