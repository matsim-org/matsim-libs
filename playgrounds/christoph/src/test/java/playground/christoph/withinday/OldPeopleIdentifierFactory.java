/* *********************************************************************** *
 * project: org.matsim.*
 * OldPeopleIdentifierFactory.java
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

package playground.christoph.withinday;

import org.matsim.ptproject.qsim.interfaces.Netsim;

import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;

public class OldPeopleIdentifierFactory implements DuringActivityIdentifierFactory {

	private Netsim mobsim;
	
	public OldPeopleIdentifierFactory(Netsim mobsim) {
		this.mobsim = mobsim;
	}
	
	@Override
	public DuringActivityIdentifier createIdentifier() {
		DuringActivityIdentifier identifier = new OldPeopleIdentifier(mobsim);
		identifier.setIdentifierFactory(this);
		return identifier;
	}

}
