/* *********************************************************************** *
 * project: org.matsim.*
 * InitialIdentifierImplFactory.java
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

package org.matsim.withinday.replanning.identifiers;

import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifierFactory;

public class InitialIdentifierImplFactory extends InitialIdentifierFactory {

	private MobsimDataProvider mobsimDataProvider;
	
	public InitialIdentifierImplFactory(MobsimDataProvider mobsimDataProvider) {
		this.mobsimDataProvider = mobsimDataProvider;
	}
	
	@Override
	public InitialIdentifier createIdentifier() {
		InitialIdentifier identifier = new InitialIdentifierImpl(mobsimDataProvider);
		identifier.setAgentSelectorFactory(this);
		this.addAgentFiltersToIdentifier(identifier);
		return identifier;
	}

}
