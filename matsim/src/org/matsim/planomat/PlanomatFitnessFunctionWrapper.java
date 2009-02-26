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
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Route;
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

	private double now;
	private double planScore;
	private double travelTime;
	private Act origin, destination;
	private Leg legIntermediate;
	private Route tempRoute;
	private ArrayList<Object> actslegs;
	private int numActs;
	private int legCounter;
	private int subtourIndex, modeIndex;

	@Override
	protected double evaluate(final IChromosome a_subject) {
		this.planScore = 0.0;
		this.origin = null;
		this.destination = null;
		this.legIntermediate = null;
		this.tempRoute = null;

		this.sf.reset();

		this.now = 0.0;

		this.actslegs = this.plan.getActsLegs();
		this.numActs = this.actslegs.size() / 2;

		this.legCounter = 0;
		for (Object o : this.actslegs) {

			if (o instanceof Leg) {

				this.legIntermediate = (Leg) o;

				this.now += (((IntegerGene) a_subject.getGene(this.legCounter)).intValue() + 0.5) * Planomat.TIME_INTERVAL_SIZE;

				this.sf.startLeg(this.now, null);

				this.origin = this.plan.getPreviousActivity(this.legIntermediate);
				this.destination = this.plan.getNextActivity(this.legIntermediate);

				if (Gbl.getConfig().planomat().getPossibleModes().length > 0) {
					// set mode
					this.subtourIndex = this.planAnalyzeSubtours.getSubtourIndexation()[this.legCounter];
					this.modeIndex = ((IntegerGene) a_subject.getGene(this.numActs + this.subtourIndex)).intValue();
					this.legIntermediate.setMode(Gbl.getConfig().planomat().getPossibleModes()[this.modeIndex]);
				} // otherwise leave modes untouched

				// save original route
				if (!this.legIntermediate.getMode().equals(BasicLeg.Mode.car)) {
					this.tempRoute = this.legIntermediate.getRoute();
				}

				// set times
				this.travelTime = this.legTravelTimeEstimator.getLegTravelTimeEstimation(
						this.plan.getPerson().getId(),
						this.now,
						this.origin,
						this.destination,
						this.legIntermediate);

				this.now += this.travelTime;

				if (!this.legIntermediate.getMode().equals(BasicLeg.Mode.car)) {
					// recover original route
					this.legIntermediate.setRoute(this.tempRoute);
				}
				this.sf.endLeg(this.now);

				this.legCounter++;

			}

		}

		this.sf.finish();
		this.planScore = this.sf.getScore();
		// JGAP accepts only fitness values >= 0. bad plans often have negative scores. So we have to
		// - make sure a fitness value will be >= 0, but
		// - see that the fitness landscape will not be distorted too much by this, so we will add an offset (this s**ks, but works)
		// - theoretically is a problem if GA selection is based on score ratio (e.g. weighted roulette wheel selection)
//		return Math.max(0.0, sf.getScore());
		return Math.max(0.0, this.planScore + PlanomatFitnessFunctionWrapper.FITNESS_OFFSET);
	}

}
