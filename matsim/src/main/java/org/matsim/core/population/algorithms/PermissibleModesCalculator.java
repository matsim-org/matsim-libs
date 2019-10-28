
/* *********************************************************************** *
 * project: org.matsim.*
 * PermissibleModesCalculator.java
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

 package org.matsim.core.population.algorithms;

import java.util.Collection;

import org.matsim.api.core.v01.population.Plan;

public interface PermissibleModesCalculator {
	
	/**
	 * @param plan
	 * @return Collection of modes that the agent can in principle use.  For example, cannot use car if no car is available;
	 * cannot use car sharing if not member.
	 */
	Collection<String> getPermissibleModes(Plan plan);

}
