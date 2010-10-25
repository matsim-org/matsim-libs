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

package playground.christoph.withinday.replanning.identifiers;

import org.matsim.ptproject.qsim.QSim;

import playground.christoph.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.InitialIdentifierFactory;

public class InitialIdentifierImplFactory implements InitialIdentifierFactory {

	private QSim qsim;
	
	public InitialIdentifierImplFactory(QSim qsim) {
		this.qsim = qsim;
	}
	
	@Override
	public InitialIdentifier createIdentifier() {
		InitialIdentifier identifier = new InitialIdentifierImpl(qsim);
		identifier.setIdentifierFactory(this);
		return identifier;
	}

}
