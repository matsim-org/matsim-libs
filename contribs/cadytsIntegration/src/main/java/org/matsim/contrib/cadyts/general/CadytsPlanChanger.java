/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.cadyts.general;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

/**
 * @author nagel
 */
public class CadytsPlanChanger<T> implements PlanSelector {

	private final double beta ;
	private double cadytsWeight = 1.0;

	private CadytsContextI<T> cadytsContext;

	public static final String CADYTS_CORRECTION = "cadytsCorrection";

	public CadytsPlanChanger(Scenario scenario, CadytsContextI<T> cadytsContext) {
		this.cadytsContext = cadytsContext;
		this.beta = scenario.getConfig().planCalcScore().getBrainExpBeta() ;
	}
	
	@Override
	public Plan selectPlan(final HasPlansAndId<Plan, Person> person) {
		final Plan currentPlan = person.getSelectedPlan();
		if (person.getPlans().size() <= 1 || currentPlan.getScore() == null) {
			return currentPlan;
		}

		// random plan:
		Plan otherPlan;
		do {
			otherPlan = new RandomPlanSelector<Plan, Person>().selectPlan((person));
		} while (otherPlan == currentPlan);

		if (otherPlan.getScore() == null) {
			return otherPlan;
		}

		cadyts.demand.Plan<T> currentPlanSteps = this.cadytsContext.getPlansTranslator().getCadytsPlan(currentPlan);
		double currentPlanCadytsCorrection = this.cadytsContext.getCalibrator().calcLinearPlanEffect(currentPlanSteps) / this.beta;
		double currentScore = currentPlan.getScore() + cadytsWeight * currentPlanCadytsCorrection;

		cadyts.demand.Plan<T> otherPlanSteps = this.cadytsContext.getPlansTranslator().getCadytsPlan(otherPlan);
		double otherPlanCadytsCorrection = this.cadytsContext.getCalibrator().calcLinearPlanEffect(otherPlanSteps) / this.beta;
		double otherScore = otherPlan.getScore() + cadytsWeight * otherPlanCadytsCorrection;

		Map<String,Object> planAttributes = currentPlan.getCustomAttributes() ;
		planAttributes.put(CadytsPlanChanger.CADYTS_CORRECTION,currentPlanCadytsCorrection) ;

		Map<String,Object> planAttributesOther = otherPlan.getCustomAttributes() ;
		planAttributesOther.put(CadytsPlanChanger.CADYTS_CORRECTION,otherPlanCadytsCorrection) ;

		double weight = Math.exp(0.5 * this.beta * (otherScore - currentScore));

		Plan selectedPlan = currentPlan;
		if (MatsimRandom.getRandom().nextDouble() < 0.01 * weight) {
			// as of now, 0.01 is hardcoded (proba to change when both scores are the same)
			selectedPlan = otherPlan;
		}
		return selectedPlan;
	}

	public void setCadytsWeight(double cadytsWeight) {
		this.cadytsWeight = cadytsWeight;
	}
}