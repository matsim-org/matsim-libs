/* *********************************************************************** *
 * project: org.matsim.*
 * YoungPeopleIdentifierFactory.java
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

package playground.wrashid.parkingSearch.withinday;

import org.matsim.ptproject.qsim.interfaces.Mobsim;

import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;

public class YoungPeopleIdentifierFactory implements DuringLegIdentifierFactory {

	private Mobsim mobsim;
	
	public YoungPeopleIdentifierFactory(Mobsim mobsim) {
		this.mobsim = mobsim;
	}
	
	@Override
	public DuringLegIdentifier createIdentifier() {
		DuringLegIdentifier identifier = new YoungPeopleIdentifier(mobsim);
		identifier.setIdentifierFactory(this);
		return identifier;
	}

}
