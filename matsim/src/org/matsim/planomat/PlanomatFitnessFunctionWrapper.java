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
import org.jgap.impl.DoubleGene;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.scoring.ScoringFunction;
import org.matsim.world.Location;

/**
 * This class connects the JGAP FitnessFunction class with the MATSim ScoringFunction interface.
 * This is done in order to use the MATSim scoring function in a JGAP optimization procedure like planomat.
 * 
 * @author meisterk
 *
 */
public class PlanomatFitnessFunctionWrapper extends FitnessFunction {

	public static final double FITNESS_OFFSET = 10000.0;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Plan plan;	
	private LegTravelTimeEstimator legTravelTimeEstimator;
	private ScoringFunction sf;

	public PlanomatFitnessFunctionWrapper(ScoringFunction sf, Plan plan, LegTravelTimeEstimator legTravelTimeEstimator) {
		super();
		this.sf = sf;
		this.plan = plan;
		this.legTravelTimeEstimator = legTravelTimeEstimator;
	}
	
	@Override
	protected double evaluate(IChromosome a_subject) {

		double planScore = 0.0;
		double travelTime;

		sf.reset();
		double now = 0.0;
		// process "middle" activities
		for (int ii=0; ii < a_subject.size(); ii++) {

			now += ((DoubleGene) a_subject.getGene(ii)).doubleValue();

			sf.startLeg(now, null);
			
			Location origin = ((Act) plan.getActsLegs().get(ii * 2)).getLink();
			Location destination = ((Act) plan.getActsLegs().get((ii + 1) * 2)).getLink();
			Route route = ((Leg) plan.getActsLegs().get((ii * 2) + 1)).getRoute();
			travelTime = this.legTravelTimeEstimator.getLegTravelTimeEstimation(this.plan.getPerson().getId(), now, origin, destination, route, "car");

			now += travelTime;

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
