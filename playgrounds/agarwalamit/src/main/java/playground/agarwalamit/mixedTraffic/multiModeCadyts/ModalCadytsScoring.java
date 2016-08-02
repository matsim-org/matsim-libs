/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.multiModeCadyts;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.SumScoringFunction;

import cadyts.calibrators.analytical.AnalyticalCalibrator;

/**
 * @author nagel
 */
public class ModalCadytsScoring<T> implements SumScoringFunction.BasicScoring {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(ModalCadytsScoring.class);

	private double score = 0.;
	private PlansTranslator<T> plansTranslator;
	private AnalyticalCalibrator<T> matsimCalibrator;
	private Plan plan;
	private final double beta;
	private double weightOfCadytsCorrection = 1.;

	public ModalCadytsScoring(final Plan plan, Config config, final ModalCadytsContextI<T> context) {
		this.plansTranslator = context.getPlansTranslator(); // only one is suffice
		
		// first check if all legs have same modes
		String firstLegMode = null;
		for (PlanElement pe :plan.getPlanElements()) {
			if (pe instanceof Leg) {
				String currentLegMode = ((Leg)pe).getMode();
				if (firstLegMode==null) firstLegMode = currentLegMode;
				else if ( ! firstLegMode.equals(currentLegMode)) {
					throw new RuntimeException("Legs have different modes. Not sure if current set up will work for this.");
				}
			}
		}
		
		this.matsimCalibrator = context.getCalibrator(firstLegMode);
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