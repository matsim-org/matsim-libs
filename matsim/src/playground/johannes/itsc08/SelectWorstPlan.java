/* *********************************************************************** *
 * project: org.matsim.*
 * SelectWorstPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.johannes.itsc08;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author illenberger
 *
 */
public class SelectWorstPlan implements PlanSelector {

	public Plan selectPlan(Person person) {
		Plan worst = null;
		double worstScore = Double.MAX_VALUE;
		for(Plan p : person.getPlans()) {
			if(p.getScore().doubleValue() < worstScore) {
				worst = p;
				worstScore = p.getScore().doubleValue();
			}
		}
		return worst;
	}

}
