/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelFitnessFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.planomat;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;
import org.matsim.core.api.population.Plan;
import org.matsim.planomat.Planomat.StepThroughPlanAction;

/**
 * This class connects the JGAP FitnessFunction class with the MATSim ScoringFunction interface.
 * This is done in order to use the MATSim scoring function in a JGAP optimization procedure like planomat.
 *
 * @author meisterk
 *
 */
public class PlanomatFitnessFunctionWrapper extends FitnessFunction {

	private double m_lastComputedFitnessValue = FitnessFunction.NO_FITNESS_VALUE;
	
	@Override
	public double getFitnessValue(IChromosome a_subject) {
	    // Delegate to the evaluate() method to actually compute the
	    // fitness value. If the returned value is less than one,
	    // then we throw a runtime exception.
	    // ---------------------------------------------------------
	    double fitnessValue = evaluate(a_subject);
	    this.m_lastComputedFitnessValue = fitnessValue;
	    return fitnessValue;
	}

//	private static final double FITNESS_OFFSET = 10000.0;

	private static final long serialVersionUID = 1L;

	private transient final Planomat planomat;
	private transient final Plan plan;

	public PlanomatFitnessFunctionWrapper(Planomat planomat, Plan plan) {
		super();
		this.planomat = planomat;
		this.plan = plan;
	}

	@Override
	protected double evaluate(final IChromosome a_subject) {

		double planScore = this.planomat.stepThroughPlan(StepThroughPlanAction.EVALUATE, a_subject, this.plan);

		// JGAP accepts only fitness values >= 0. bad plans often have negative scores. So we have to
		// - make sure a fitness value will be >= 0, but
		// - see that the fitness landscape will not be distorted too much by this, so we will add an offset (this s**ks, but works)
		// - theoretically is a problem if GA selection is based on score ratio (e.g. weighted roulette wheel selection)
//		return Math.max(0.0, sf.getScore());
//		return Math.max(0.0, planScore + PlanomatFitnessFunctionWrapper.FITNESS_OFFSET);
		return planScore;
	}

	@Override
	public double getLastComputedFitnessValue() {
	    return this.m_lastComputedFitnessValue;
	}

}
