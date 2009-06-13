/* *********************************************************************** *
 * project: org.matsim.*
 * ExpBetaPlanForRemovalSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning.selectors;

import org.matsim.core.api.population.Plan;

/**
 * Selects one of the existing plans of the person based on the
 * weight = exp(beta * 1/f * (score/maxScore)).
 * 
 * TODO does not seem to be for removal in my opinion! marcel/13jun2009
 * I would expect plans with lower score to be removed more frequently,
 * but this selector chooses plans with higher scores more frequently! 
 *
 * @author anhorni
 */
public class ExpBetaPlanForRemovalSelector extends ExpBetaPlanSelector {

	private static final double f = 1.0 / 5.0;
	
	protected double calcPlanWeight(final Plan plan, final double maxScore) {
		if (plan.getScore() == null) {
			return Double.NaN;
		}
		double weight = Math.exp(this.beta * (plan.getScore() / (f * maxScore)));
		if (weight < MIN_WEIGHT) weight = MIN_WEIGHT;
		return weight;
	}
}
