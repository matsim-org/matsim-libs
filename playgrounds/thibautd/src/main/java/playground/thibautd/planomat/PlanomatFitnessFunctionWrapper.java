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

package playground.thibautd.planomat;

import java.util.TreeSet;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;
import org.matsim.api.core.v01.population.Plan;
import playground.thibautd.planomat.Planomat.StepThroughPlanAction;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

/**
 * This class connects the JGAP FitnessFunction class with the MATSim ScoringFunction interface.
 * This is done in order to use the MATSim scoring function in a JGAP optimization procedure like planomat.
 *
 * @author meisterk
 *
 */
public class PlanomatFitnessFunctionWrapper extends FitnessFunction {

	/**
	 * A replacement for FitnessFunction.NO_FITNESS_VALUE, which is -1.000...
	 * This default value signalling a fitness value that has not been computed yet doesn't work here because we operate also with negative fitness values
	 * which is not intended by JGAP.
	 */
	public static final double NO_FITNESS_VALUE = Double.NEGATIVE_INFINITY;

	@Override
	public double getFitnessValue(IChromosome a_subject) {
	    return evaluate(a_subject);
	}

	private static final long serialVersionUID = 1L;

	private transient final Planomat planomat;
	private transient final Plan plan;
	private transient final PlanAnalyzeSubtours planAnalyzeSubtours;
	private transient final TreeSet<String> possibleModes;
	private transient final LegTravelTimeEstimator legTravelTimeEstimator;

	public PlanomatFitnessFunctionWrapper(
			Planomat planomat,
			Plan plan,
			PlanAnalyzeSubtours planAnalyzeSubtours,
			TreeSet<String> possibleModes,
			LegTravelTimeEstimator legTravelTimeEstimator) {
		super();
		this.planomat = planomat;
		this.plan = plan;
		this.planAnalyzeSubtours = planAnalyzeSubtours;
		this.possibleModes = possibleModes;
		this.legTravelTimeEstimator = legTravelTimeEstimator;

	}

	@Override
	protected double evaluate(final IChromosome a_subject) {

		double planScore = this.planomat.stepThroughPlan(
				StepThroughPlanAction.EVALUATE,
				a_subject,
				this.plan,
				this.planAnalyzeSubtours,
				this.legTravelTimeEstimator,
				this.possibleModes);

		return planScore;
	}

}
