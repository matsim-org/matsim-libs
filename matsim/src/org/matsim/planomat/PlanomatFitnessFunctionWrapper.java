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

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;
import org.jgap.impl.IntegerGene;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.gbl.Gbl;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.population.routes.Route;
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
		BasicLeg.Mode mode = null;
		Act origin = null, destination = null;
		Leg legIntermediate = null;
		Route tempRoute = null;
		
		sf.reset();
		double now = 0.0;
		// process "middle" activities
		int numActs = this.planAnalyzeSubtours.getSubtourIndexation().length;

		ArrayList<Object> actslegs = this.plan.getActsLegs();

		for (int ii=0; ii < numActs; ii++) {

			now += (((IntegerGene) a_subject.getGene(ii)).intValue() + 0.5) * Planomat.TIME_INTERVAL_SIZE;

			sf.startLeg(now, null);

			origin = (Act) actslegs.get(ii * 2);
			legIntermediate = (Leg) actslegs.get(ii * 2 + 1);
			destination = (Act) actslegs.get(ii * 2 + 2);

			if (Gbl.getConfig().planomat().getPossibleModes().length > 0) {
				// set mode
				subtourIndex = this.planAnalyzeSubtours.getSubtourIndexation()[ii];
				modeIndex = ((IntegerGene) a_subject.getGene(numActs + subtourIndex)).intValue();
				mode = Gbl.getConfig().planomat().getPossibleModes()[modeIndex];
				legIntermediate.setMode(mode);
			} // otherwise leave modes untouched

			if (!legIntermediate.getMode().equals(BasicLeg.Mode.car)) {
				tempRoute = legIntermediate.getRoute();
			}
			
			// set times
			travelTime = this.legTravelTimeEstimator.getLegTravelTimeEstimation(
					this.plan.getPerson().getId(),
					now,
					origin,
					destination,
					legIntermediate);

			now += travelTime;

			if (!legIntermediate.getMode().equals(BasicLeg.Mode.car)) {
				// recover original route
				legIntermediate.setRoute(tempRoute);
			}
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
