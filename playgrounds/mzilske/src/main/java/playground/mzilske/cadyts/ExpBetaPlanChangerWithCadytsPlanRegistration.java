/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ExpBetaPlanChangerWithCadytsPlanRegistration.java
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
import org.matsim.contrib.cadyts.general.CadytsContextI;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author nagel
 *
 */
public final class ExpBetaPlanChangerWithCadytsPlanRegistration<T> implements PlanSelector {

	private final PlanSelector delegate ;
    private PlansTranslator<T> plansTranslator;
    private AnalyticalCalibrator<T> calibrator;

    public ExpBetaPlanChangerWithCadytsPlanRegistration(double beta, PlansTranslator<T> plansTranslator, AnalyticalCalibrator<T> calibrator) {
        this.plansTranslator = plansTranslator;
        this.calibrator = calibrator;
        delegate = new ExpBetaPlanChanger(beta);
	}
	

	@Override
	public Plan selectPlan(HasPlansAndId<Plan> person) {
		Plan selectedPlan = delegate.selectPlan(person) ;
		cadyts.demand.Plan<T> cadytsPlan = plansTranslator.getPlanSteps(selectedPlan);
		calibrator.addToDemand(cadytsPlan);
		return selectedPlan;
	}
	
}
