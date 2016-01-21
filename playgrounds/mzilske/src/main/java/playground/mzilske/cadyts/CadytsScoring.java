/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CadytsScoring.java
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
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.SumScoringFunction;

/**
 * @author nagel
 *
 */
public class CadytsScoring<T> implements SumScoringFunction.BasicScoring {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(CadytsScoring.class);

	private double score = 0.;
	private PlansTranslator<T> ptPlanToPlanStep;
	private AnalyticalCalibrator<T> matsimCalibrator;
	private Plan plan;
	private final double beta;

    public void setWeight(double weight) {
        this.weight = weight;
    }

    private double weight = 1.0;

	public CadytsScoring(final Plan plan, Config config, PlansTranslator<T> ptPlanToPlanStep, AnalyticalCalibrator<T> matsimCalibrator) {
		this.ptPlanToPlanStep = ptPlanToPlanStep;
		this.matsimCalibrator = matsimCalibrator;
		this.plan = plan;
		this.beta = config.planCalcScore().getBrainExpBeta();
	}
	
	@Override
	public void finish() {
		cadyts.demand.Plan<T> currentPlanSteps = this.ptPlanToPlanStep.getCadytsPlan(plan);
        this.score = this.matsimCalibrator.calcLinearPlanEffect(currentPlanSteps) / this.beta;
	}

	@Override
	public double getScore() {
		return weight * score;
	}

}
