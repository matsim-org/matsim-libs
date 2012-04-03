/* *********************************************************************** *
 * project: org.matsim.*
 * ReScoringFromCustomAttr.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.scoring;

import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.parameterSearch.PatternSearchListenerI;

/**
 * It is assumed that each plan has customized attributes corresponding to the
 * parameters in scoring function
 * 
 * @author yu
 * 
 */
public class ReScoringFromCustomAttr extends AbstractPersonAlgorithm implements
		PlanAlgorithm {

	@Override
	public void run(Plan plan) {
		Map<String, Object> customAttrs = plan.getCustomAttributes();
		double score = 0;
		for (String paramName : customAttrs.keySet()) {
			Object oj = customAttrs.get(paramName);
			if (oj != null) {
				Number attr = (Number) oj;
				score += PatternSearchListenerI.nameParametersMap
						.get(paramName) * attr.doubleValue();
			}
		}
		plan.setScore(score);
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

}
