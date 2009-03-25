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

import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author illenberger
 *
 */
public class SelectWorstPlan implements PlanSelector {

	/* (non-Javadoc)
	 * @see org.matsim.replanning.selectors.PlanSelector#selectPlan(org.matsim.population.Person)
	 */
	public Plan selectPlan(Person person) {
		Plan worst = null;
		double worstScore = Double.MAX_VALUE;
		for(Plan p : person.getPlans()) {
			if(p.getScoreAsPrimitiveType() < worstScore) {
				worst = p;
				worstScore = p.getScoreAsPrimitiveType();
			}
		}
		return worst;
	}

}
