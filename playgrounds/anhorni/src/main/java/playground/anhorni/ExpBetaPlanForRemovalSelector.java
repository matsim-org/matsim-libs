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

package playground.anhorni;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

/**
 * Selects one of the existing plans of the person based on the
 * weight = exp(beta * ((maxScore - score)/maxScore)).
 *
 * TODO does not seem to be for removal in my opinion! marcel/13jun2009
 * I would expect plans with lower score to be removed more frequently,
 * but this selector chooses plans with higher scores more frequently!
 *
 *  Thanks! Have tentatively changed the weight. Need to think about that again, after some sleep.
 *  anhorni/13jun2009
 *
 * @author anhorni
 */
public class ExpBetaPlanForRemovalSelector extends ExpBetaPlanSelector {

	public ExpBetaPlanForRemovalSelector(
			CharyparNagelScoringConfigGroup charyparNagelScoringConfigGroup) {
		super(charyparNagelScoringConfigGroup);
	}

	@Override
	protected double calcPlanWeight(final Plan plan, final double maxScore) {

		if (plan.getScore() == null) {
			return Double.NaN;
		}

		/*
		 * TODO:
		 * if maxScore == 0 all scores are maybe negative
		 * -> need minScore for scaling
		 * for the moment: do not do scaling in this case while hoping to not face an overflow
		 */
		double scaling = maxScore;
		if (maxScore == 0) scaling = 1.0;

		/*
		 * TODO: Check that again
		 * (interval [min..max] etc.)
		 */
		double weight = Math.exp(this.beta * ((maxScore - plan.getScore()) / scaling));
		if (weight < MIN_WEIGHT) weight = MIN_WEIGHT;
		return weight;
	}
}
