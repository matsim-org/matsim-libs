/* *********************************************************************** *
 * project: org.matsim.*
 * NewPtBsePlanChanger.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.mmoyo.w_ptCounts_from_kai.ptBseAsPlanStrategy;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import cadyts.interfaces.matsim.MATSimUtilityModificationCalibrator;

/**
 * @author nagel
 *
 */
class NewPtBsePlanChanger implements PlanSelector
{
	private static final Logger log = Logger.getLogger("dummy");

	private static boolean scoreWrnFlag = true;

	private double beta = 1.0 ;

	private PtPlanToPlanStepBasedOnEvents ptPlanToPlanStep;

	private MATSimUtilityModificationCalibrator<TransitStopFacility> matsimCalibrator;
	
	/*
	final String currentOLD = "currentPlanCadytsCorrectionOLD: ";
	final String currentNEW = " currentPlanCadytsCorrectionNEW: ";
	final String otherOLD   = "otherPlanCadytsCorrectionOLD: ";
	final String otherNEW   = " otherPlanCadytsCorrectionNEW: ";
	final String separator = "====================================";
	*/
	
	NewPtBsePlanChanger(PtPlanToPlanStepBasedOnEvents ptStep, MATSimUtilityModificationCalibrator<TransitStopFacility> calib ) {
		log.error( "value for beta currently ignored (set to one)") ;
		this.ptPlanToPlanStep = ptStep ;
		this.matsimCalibrator = calib ;
	}

	@Override
	public Plan selectPlan(Person person) {
		Plan currentPlan = person.getSelectedPlan();
		if ( person.getPlans().size() <= 1 || currentPlan.getScore()==null ) {
			return currentPlan ;
		}
		
		//ChoiceSampler<TransitStopFacility> sampler = ((SamplingCalibrator<TransitStopFacility>)this.matsimCalibrator).getSampler(person) ;
		
		// random plan:
		Plan otherPlan = null ;
		do {
			otherPlan = ((PersonImpl) person).getRandomPlan();
		} while ( otherPlan==currentPlan ) ;

		if ( otherPlan.getScore()==null ) {
			return otherPlan;
		}
		
		cadyts.demand.Plan<TransitStopFacility> currentPlanSteps = this.ptPlanToPlanStep.getPlanSteps( currentPlan );
		double currentPlanCadytsCorrection = this.matsimCalibrator.getUtilityCorrection(currentPlanSteps)/ this.beta;
		double currentScore = currentPlan.getScore().doubleValue() + currentPlanCadytsCorrection;
		
		cadyts.demand.Plan<TransitStopFacility> otherPlanSteps = this.ptPlanToPlanStep.getPlanSteps( otherPlan );
		double otherPlanCadytsCorrection  = this.matsimCalibrator.getUtilityCorrection(otherPlanSteps)/ this.beta;
		double otherScore = otherPlan.getScore().doubleValue() + otherPlanCadytsCorrection ;
		
		if ( currentPlanCadytsCorrection != otherPlanCadytsCorrection) {
			log.info( "currPlanCadytsCorr: " + currentPlanCadytsCorrection+ " otherPlanCadytsCorr: " + otherPlanCadytsCorrection) ;
		}

		double weight = Math.exp( 0.5 * this.beta * (otherScore - currentScore) );
		// (so far, this is >1 if otherScore>currentScore, and <=1 otherwise)
		// (beta is the slope (strength) of the operation: large beta means strong reaction)

		Plan selectedPlan = currentPlan ;
		if (MatsimRandom.getRandom().nextDouble() < 0.01*weight ) { 
			// as of now, 0.01 is hardcoded (proba to change when both scores are the same)

			selectedPlan = otherPlan;
		}

		//sampler.enforceNextAccept();
		//sampler.isAccepted(this.ptPlanToPlanStep.getPlanSteps(selectedPlan));

		this.matsimCalibrator.addToDemand(this.ptPlanToPlanStep.getPlanSteps(selectedPlan));//?
		
		return selectedPlan ;
	}


}
