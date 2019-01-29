/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PlanStrategies.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.replanning;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public final class ReplanningUtils {

	public static boolean isOnlySelector(GenericPlanStrategy<Plan, Person> planStrategy) {
		if (planStrategy instanceof PlanStrategyImpl) {
			return ((PlanStrategyImpl) planStrategy).getNumberOfStrategyModules() == 0;
		}
		return false;
	}

}
