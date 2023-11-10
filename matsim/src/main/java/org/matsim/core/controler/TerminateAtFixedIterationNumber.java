
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

import jakarta.inject.Inject;

import org.matsim.core.config.groups.ControllerConfigGroup;

class TerminateAtFixedIterationNumber implements TerminationCriterion {
	private final int lastIteration;

	@Inject
	TerminateAtFixedIterationNumber(ControllerConfigGroup controllerConfigGroup) {
		this.lastIteration = controllerConfigGroup.getLastIteration();
	}

	@Override
	public boolean mayTerminateAfterIteration(int iteration) {
		return iteration >= lastIteration;
	}

	@Override
	public boolean doTerminate(int iteration) {
		return iteration >= lastIteration;
	}

}
