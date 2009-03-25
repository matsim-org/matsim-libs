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

import java.util.List;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;
import org.jgap.impl.IntegerGene;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Route;
import org.matsim.gbl.Gbl;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
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

	private static final double FITNESS_OFFSET = 10000.0;

	private static final long serialVersionUID = 1L;

	private transient final Plan plan;
	private transient final LegTravelTimeEstimator legTravelTimeEstimator;
	private transient final ScoringFunction sf;
	private transient final PlanAnalyzeSubtours planAnalyzeSubtours;

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
	protected double evaluate(final IChromosome a_subject) {
		double planScore = 0.0;
		Route tempRoute = null;

		this.sf.reset();

		double now = 0.0;

		List<? extends BasicPlanElement> actslegs = this.plan.getPlanElements();
		int numActs = actslegs.size() / 2;

		int legCounter = 0;
		for (BasicPlanElement o : actslegs) {

			if (o instanceof Leg) {

				Leg legIntermediate = (Leg) o;

				now += (((IntegerGene) a_subject.getGene(legCounter)).intValue() + 0.5) * Planomat.TIME_INTERVAL_SIZE;

				this.sf.startLeg(now, null);

				Activity origin = this.plan.getPreviousActivity(legIntermediate);
				Activity destination = this.plan.getNextActivity(legIntermediate);

				if (Gbl.getConfig().planomat().getPossibleModes().length > 0) {
					// set mode
					int subtourIndex = this.planAnalyzeSubtours.getSubtourIndexation()[legCounter];
					int modeIndex = ((IntegerGene) a_subject.getGene(numActs + subtourIndex)).intValue();
					legIntermediate.setMode(Gbl.getConfig().planomat().getPossibleModes()[modeIndex]);
				} // otherwise leave modes untouched

				// save original route
				if (!legIntermediate.getMode().equals(BasicLeg.Mode.car)) {
					tempRoute = legIntermediate.getRoute();
				}

				// set times
				double travelTime = this.legTravelTimeEstimator.getLegTravelTimeEstimation(
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
				this.sf.endLeg(now);

				legCounter++;

			}

		}

		this.sf.finish();
		planScore = this.sf.getScore();
		// JGAP accepts only fitness values >= 0. bad plans often have negative scores. So we have to
		// - make sure a fitness value will be >= 0, but
		// - see that the fitness landscape will not be distorted too much by this, so we will add an offset (this s**ks, but works)
		// - theoretically is a problem if GA selection is based on score ratio (e.g. weighted roulette wheel selection)
//		return Math.max(0.0, sf.getScore());
		return Math.max(0.0, planScore + PlanomatFitnessFunctionWrapper.FITNESS_OFFSET);
	}

}
