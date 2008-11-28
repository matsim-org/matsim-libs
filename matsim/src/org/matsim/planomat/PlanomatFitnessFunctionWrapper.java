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

import java.util.ArrayList;
import java.util.HashMap;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.gbl.Gbl;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.population.Route;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.scoring.ScoringFunction;

/**
 * This class connects the JGAP FitnessFunction class with the MATSim ScoringFunction interface.
 * This is done in order to use the MATSim scoring function in a JGAP optimization procedure like planomat.
 *
 * @author meisterk
 *
 */
public class PlanomatFitnessFunctionWrapper extends FitnessFunction {

	public static final double FITNESS_OFFSET = 10000.0;

	private static final long serialVersionUID = 1L;

	private transient Plan plan;
	private transient LegTravelTimeEstimator legTravelTimeEstimator;
	private transient ScoringFunction sf;
	private transient PlanAnalyzeSubtours planAnalyzeSubtours;

	public PlanomatFitnessFunctionWrapper(
			final ScoringFunction sf,
			final Plan plan,
			final LegTravelTimeEstimator legTravelTimeEstimator,
			final PlanAnalyzeSubtours planAnalyzeSubtours) {

		this.sf = sf;
		this.plan = plan;
		this.legTravelTimeEstimator = legTravelTimeEstimator;
		this.planAnalyzeSubtours = planAnalyzeSubtours;

	}

	@Override
	protected double evaluate(IChromosome a_subject) {

		double planScore = 0.0;
		double travelTime;
		int subtourIndex, modeIndex;
		BasicLeg.Mode modeName;
		Act origin = null, destination = null;
		Leg legIntermediate = null;
		HashMap<Leg, Route> originalRoutes = PlanOptimizeTimes.getLegsRoutes(plan);

		sf.reset();
		double now = 0.0;
		// process "middle" activities
		int numActs = this.planAnalyzeSubtours.getSubtourIndexation().length;

		ArrayList<Object> actslegs = this.plan.getActsLegs();

		for (int ii=0; ii < numActs; ii++) {

			now += ((DoubleGene) a_subject.getGene(ii)).doubleValue();

			sf.startLeg(now, null);

			origin = (Act) actslegs.get(ii * 2);
			legIntermediate = (Leg) actslegs.get(ii * 2 + 1);
			destination = (Act) actslegs.get(ii * 2 + 2);

			// set mode
			subtourIndex = this.planAnalyzeSubtours.getSubtourIndexation()[ii];
			modeIndex = ((IntegerGene) a_subject.getGene(numActs + subtourIndex)).intValue();
			modeName = Gbl.getConfig().planomat().getPossibleModes()[modeIndex];
//			System.out.println(ii + "\t" + subtourIndex + "\t" + modeIndex + "\t" + modeName);
			legIntermediate.setMode(modeName);

			// set times
			travelTime = this.legTravelTimeEstimator.getLegTravelTimeEstimation(
					this.plan.getPerson().getId(),
					now,
					origin,
					destination,
					legIntermediate);

//			System.out.println(Time.writeTime(travelTime));
			now += travelTime;

			// recover original route
			legIntermediate.setRoute(originalRoutes.get(legIntermediate));
			sf.endLeg(now);
		}

		sf.finish();
		planScore = sf.getScore();
		// JGAP accepts only fitness values >= 0. bad plans often have negative scores. So we have to
		// - make sure a fitness value will be >= 0, but
		// - see that the fitness landscape will not be distorted too much by this, so we will add an offset (this s**ks, but works)
		// - could become a problem if some calculation in the GA is based on score ratio (e.g. the calculation of a selection probability)
		return Math.max(0.0, planScore + PlanomatFitnessFunctionWrapper.FITNESS_OFFSET);
	}

}
