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
package playground.johannes.gsv.sim.cadyts;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.CadytsContextI;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ArbitraryEventScoring;

/**
 * @author nagel
 *
 */
public class CadytsScoring<T> implements ArbitraryEventScoring , org.matsim.core.scoring.SumScoringFunction.ArbitraryEventScoring {
//	@SuppressWarnings("unused")
//	private static final Logger log = Logger.getLogger(CadytsScoring.class);

	private double score = 0. ;
	private PlansTranslator<T> ptPlanToPlanStep;
	private AnalyticalCalibrator<T> matsimCalibrator;
	private Plan plan;
//	private final double beta ;
	private double weightOfCadytsCorrection = 1. ;

	public CadytsScoring(final Plan plan, Config config, final CadytsContextI<T> context ) {
		this.ptPlanToPlanStep = context.getPlansTranslator() ;
		this.matsimCalibrator = context.getCalibrator() ;
		this.plan = plan ;
//		this.beta = config.planCalcScore().getBrainExpBeta() ;
	}
	
	@Override
	public void finish() {
		cadyts.demand.Plan<T> currentPlanSteps = this.ptPlanToPlanStep.getCadytsPlan(plan);
//		if(currentPlanSteps != null) {
//			System.err.println();
//		}
		double currentPlanCadytsCorrection = this.matsimCalibrator.calcLinearPlanEffect(currentPlanSteps);// / this.beta;
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
