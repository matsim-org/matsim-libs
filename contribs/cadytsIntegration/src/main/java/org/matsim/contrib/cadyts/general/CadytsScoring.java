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

package org.matsim.contrib.cadyts.general;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.SumScoringFunction;

import cadyts.calibrators.analytical.AnalyticalCalibrator;

/**
 * @author nagel
 */
public class CadytsScoring<T> implements SumScoringFunction.BasicScoring {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(CadytsScoring.class);

	private double score = 0.;
	private PlansTranslator<T> plansTranslator;
	private AnalyticalCalibrator<T> matsimCalibrator;
	private Plan plan;
	private final double beta;
	private double weightOfCadytsCorrection = 1.;

	public CadytsScoring(final Plan plan, Config config, final CadytsContextI<T> context) {
		this.plansTranslator = context.getPlansTranslator();
		this.matsimCalibrator = context.getCalibrator();
		this.plan = plan;
		this.beta = config.planCalcScore().getBrainExpBeta();
	}

	@Override
	public void finish() {
		cadyts.demand.Plan<T> currentPlanSteps = this.plansTranslator.getCadytsPlan(plan);
		double currentPlanCadytsCorrection = this.matsimCalibrator.calcLinearPlanEffect(currentPlanSteps) / this.beta;
		this.score = weightOfCadytsCorrection * currentPlanCadytsCorrection;
//		if ( currentPlanCadytsCorrection!= 0. ){
//			log.warn( "weight=" + weightOfCadytsCorrection + "; corr=" + currentPlanCadytsCorrection );
//		}
	}

	@Override
	public double getScore() {
//		if ( score != 0. && score != -450. && score != 450. ) {
//			log.warn("cadyts correction=" + score );
//		}
		return score;
	}

	public void setWeightOfCadytsCorrection(double weightOfCadytsCorrection) {
		this.weightOfCadytsCorrection = weightOfCadytsCorrection;
	}
}
