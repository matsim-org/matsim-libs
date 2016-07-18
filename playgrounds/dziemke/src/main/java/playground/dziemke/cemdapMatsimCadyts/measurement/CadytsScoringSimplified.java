/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package playground.dziemke.cemdapMatsimCadyts.measurement;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.SumScoringFunction;

import cadyts.calibrators.analytical.AnalyticalCalibrator;

/**
 * @author nagel
 */
public class CadytsScoringSimplified<T> implements SumScoringFunction.BasicScoring {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(CadytsScoringSimplified.class);

	private double score = 0.;
	private PlansTranslator<T> plansTranslator;
	private AnalyticalCalibrator<T> matsimCalibrator;
	private Plan plan;
	private final double beta;
	private double weightOfCadytsCorrection = 1.;

	public CadytsScoringSimplified(final Plan plan, Config config, PlansTranslator<T> plansTranslator, AnalyticalCalibrator<T> matsimCalibrator) {
		this.plansTranslator = plansTranslator;
		this.matsimCalibrator = matsimCalibrator;
		this.plan = plan;
		this.beta = config.planCalcScore().getBrainExpBeta();
	}

	@Override
	public void finish() {
		cadyts.demand.Plan<T> currentPlanSteps = this.plansTranslator.getCadytsPlan(plan);
		double currentPlanCadytsCorrection = this.matsimCalibrator.calcLinearPlanEffect(currentPlanSteps) / this.beta;
		this.score = weightOfCadytsCorrection * currentPlanCadytsCorrection;
	}

	@Override
	public double getScore() {
		return score;
	}

	public void setWeightOfCadytsCorrection(double weightOfCadytsCorrection) {
		this.weightOfCadytsCorrection = weightOfCadytsCorrection;
	}
}