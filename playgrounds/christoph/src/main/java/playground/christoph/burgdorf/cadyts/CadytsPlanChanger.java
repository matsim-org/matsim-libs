/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsPlanChanger.java
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

package playground.christoph.burgdorf.cadyts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.selectors.PlanSelector;

import cadyts.calibrators.analytical.AnalyticalCalibrator;

/**
 * @author cdobler
 */
/*package*/ class CadytsPlanChanger implements PlanSelector {

	private static final Logger log = Logger.getLogger(CadytsPlanChanger.class);

	private final double beta = 1.0;

	private final PlanToPlanStepBasedOnEvents planToPlanStep;

	private final AnalyticalCalibrator<Link> matsimCalibrator;

	private boolean cadCorrMessGiven = false;

	/*package*/ CadytsPlanChanger(final PlanToPlanStepBasedOnEvents planToPlanStep, final AnalyticalCalibrator<Link> calib) {
		log.error("value for beta currently ignored (set to one)");
		this.planToPlanStep = planToPlanStep;
		this.matsimCalibrator = calib;
	}

	@Override
	public Plan selectPlan(final Person person) {
		Plan currentPlan = person.getSelectedPlan();
		if (person.getPlans().size() <= 1 || currentPlan.getScore() == null) {
			return currentPlan;
		}
		// ChoiceSampler<TransitStopFacility> sampler =
		// ((SamplingCalibrator<TransitStopFacility>)this.matsimCalibrator).getSampler(person) ;

		// random plan:
		Plan otherPlan = null;
		do {
			otherPlan = ((PersonImpl) person).getRandomPlan();
		} while (otherPlan == currentPlan);

		if (otherPlan.getScore() == null) {
			return otherPlan;
		}

		cadyts.demand.Plan<Link> currentPlanSteps = this.planToPlanStep.getPlanSteps(currentPlan);
//		double currentPlanCadytsCorrection = this.matsimCalibrator.getUtilityCorrection(currentPlanSteps) / this.beta;
		double currentPlanCadytsCorrection = this.matsimCalibrator.calcLinearPlanEffect(currentPlanSteps) / this.beta;
		double currentScore = currentPlan.getScore().doubleValue() + currentPlanCadytsCorrection;

		cadyts.demand.Plan<Link> otherPlanSteps = this.planToPlanStep.getPlanSteps(otherPlan);
//		double otherPlanCadytsCorrection = this.matsimCalibrator.getUtilityCorrection(otherPlanSteps) / this.beta;
		double otherPlanCadytsCorrection = this.matsimCalibrator.calcLinearPlanEffect(otherPlanSteps) / this.beta;
		double otherScore = otherPlan.getScore().doubleValue() + otherPlanCadytsCorrection;

		if (currentPlanCadytsCorrection != otherPlanCadytsCorrection && !this.cadCorrMessGiven) {
			log.info("currPlanCadytsCorr: " + currentPlanCadytsCorrection + " otherPlanCadytsCorr: " + otherPlanCadytsCorrection + Gbl.ONLYONCE);
			this.cadCorrMessGiven = true;
		}

		double weight = Math.exp(0.5 * this.beta * (otherScore - currentScore));
		// (so far, this is >1 if otherScore>currentScore, and <=1 otherwise)
		// (beta is the slope (strength) of the operation: large beta means strong reaction)

		Plan selectedPlan = currentPlan;
		cadyts.demand.Plan<Link> selectedPlanSteps = currentPlanSteps;
		if (MatsimRandom.getRandom().nextDouble() < 0.01 * weight) {
			// as of now, 0.01 is hardcoded (proba to change when both scores are the same)

			selectedPlan = otherPlan;
			selectedPlanSteps = otherPlanSteps;
		}

		// sampler.enforceNextAccept();
		// sampler.isAccepted(this.ptPlanToPlanStep.getPlanSteps(selectedPlan));

//		this.matsimCalibrator.registerChoice(selectedPlanSteps);
		this.matsimCalibrator.addToDemand(selectedPlanSteps);

		return selectedPlan;
	}
}