
/* *********************************************************************** *
 * project: org.matsim.*
 * TerminateAtFixedIterationNumber.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.controler;

import org.matsim.core.config.groups.ControlerConfigGroup;

import javax.inject.Inject;

class TerminateAtFixedIterationNumber implements TerminationCriterion {

	private final int lastIteration;

	@Inject
	TerminateAtFixedIterationNumber(ControlerConfigGroup controlerConfigGroup) {
		this.lastIteration = controlerConfigGroup.getLastIteration();
	}

	@Override
	public boolean continueIterations(int iteration) {
		return (iteration <= lastIteration);
	}

}
