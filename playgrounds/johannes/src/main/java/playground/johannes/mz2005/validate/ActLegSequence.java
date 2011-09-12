/* *********************************************************************** *
 * project: org.matsim.*
 * ActLegSequence.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.mz2005.validate;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * @author illenberger
 *
 */
public class ActLegSequence implements PlanValidator {

	@Override
	public boolean validate(Plan plan) {
		/*
		 * check act-leg sequence
		 */
		for(int i = 0; i < plan.getPlanElements().size(); i++) {
			PlanElement element = plan.getPlanElements().get(i);
			if(i % 2 == 0) {
				if(!(element instanceof Activity))
					return false;
			} else {
				if(!(element instanceof Leg))
					return false;
			}
		}
		/*
		 * check if first and last element is an activity
		 */
		PlanElement first = plan.getPlanElements().get(0);
		if(!(first instanceof Activity))
			return false;
		
		PlanElement last = plan.getPlanElements().get(plan.getPlanElements().size() - 1);
		if(!(last instanceof Activity))
			return false;
		
		return true;
	}

}
