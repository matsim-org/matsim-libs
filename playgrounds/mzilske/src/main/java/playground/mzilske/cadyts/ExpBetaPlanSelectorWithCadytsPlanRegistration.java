/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ExpBetaPlanSelectorWithCadytsPlanRegistration.java
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

/**
 * 
 */
package playground.mzilske.cadyts;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author nagel
 *
 */
final class ExpBetaPlanSelectorWithCadytsPlanRegistration<T> implements PlanSelector {

	private final ExpBetaPlanSelector<Plan> delegate;
    private final PlansTranslator<T> plansTranslator;
    private final AnalyticalCalibrator<T> calibrator;


    public ExpBetaPlanSelectorWithCadytsPlanRegistration(double beta, PlansTranslator<T> plansTranslator, AnalyticalCalibrator<T> calibrator) {
        delegate = new ExpBetaPlanSelector<Plan>(beta);
        this.plansTranslator = plansTranslator;
        this.calibrator = calibrator;
    }

    @Override
	public Plan selectPlan(HasPlansAndId<Plan> person) {
		Plan selectedPlan = delegate.selectPlan(person);
		cadyts.demand.Plan<T> cadytsPlan = plansTranslator.getPlanSteps(selectedPlan);
		calibrator.addToDemand(cadytsPlan);
		return selectedPlan;
	}
	
}
