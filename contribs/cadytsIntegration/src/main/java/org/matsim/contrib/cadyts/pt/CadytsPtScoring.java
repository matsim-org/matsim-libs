/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.cadyts.pt;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ArbitraryEventScoring;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import cadyts.calibrators.analytical.AnalyticalCalibrator;

/**
 * @author nagel
 *
 */
public class CadytsPtScoring implements ArbitraryEventScoring {
	private static final Logger log = Logger.getLogger(CadytsPtScoring.class);

	private double score = 0. ;
	private PtPlanToPlanStepBasedOnEvents ptPlanToPlanStep;
	private AnalyticalCalibrator<TransitStopFacility> matsimCalibrator;
	private Plan plan;
	private final double beta ;
	private double weightOfCadytsCorrection = 1. ;

	CadytsPtScoring(final Plan plan, Config config, final CadytsContext context ) {
		this.ptPlanToPlanStep = context.getPtStep() ;
		this.matsimCalibrator = context.getCalibrator() ;
		this.plan = plan ;
		this.beta = config.planCalcScore().getBrainExpBeta() ;
	}
	
	@Override
	public void finish() {
		cadyts.demand.Plan<TransitStopFacility> currentPlanSteps = this.ptPlanToPlanStep.getPlanSteps(plan);
		double currentPlanCadytsCorrection = this.matsimCalibrator.calcLinearPlanEffect(currentPlanSteps) / this.beta;
		this.score = weightOfCadytsCorrection * currentPlanCadytsCorrection ;
	}

	@Override
	public double getScore() {
		return score ;
	}

	@Override
	public void reset() {
		score = 0. ;
	}

	@Override
	public void handleEvent(Event event) {
		
	}

	public void setWeightOfCadytsCorrection(double weightOfCadytsCorrection) {
		this.weightOfCadytsCorrection = weightOfCadytsCorrection;
	}

}
