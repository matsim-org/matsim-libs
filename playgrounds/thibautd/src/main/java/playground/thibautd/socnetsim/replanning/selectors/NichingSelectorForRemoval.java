/* *********************************************************************** *
 * project: org.matsim.*
 * NichingSelectorForRemoval.java
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
package playground.thibautd.socnetsim.replanning.selectors;

import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
public class NichingSelectorForRemoval extends AbstractHighestWeightSelector {
	private final PlanDistance distanceFunction;

	public NichingSelectorForRemoval() {
		this( new PlanDistance() {
			@Override
			public double calcDistance(
					final Plan p1,
					final Plan p2) {
				assert p1.getPerson() == p2.getPerson();
				
				final List<PlanElement> els1 = p1.getPlanElements();
				final List<PlanElement> els2 = p2.getPlanElements();
				
				if ( els1.size() != els2.size() ) {
					throw new IllegalArgumentException( "can only handle plans of the same size" );
				}

				final Iterator<PlanElement> iter1 = els1.iterator();
				final Iterator<PlanElement> iter2 = els2.iterator();

				// consider differences in act end time
				double d = 0;
				while ( iter1.hasNext() ) {
					assert iter2.hasNext();
					final PlanElement el1 = iter1.next();
					final PlanElement el2 = iter2.next();

					if ( el1 instanceof Activity ) {
						final Activity act1 = (Activity) el1;
						final Activity act2 = (Activity) el2;

						final double endTime1 = act1.getEndTime();
						final double endTime2 = act2.getEndTime();

						if ( endTime1 == Time.UNDEFINED_TIME || endTime2 == Time.UNDEFINED_TIME ) {
							// even in correct plans, this happens in the last act
							continue;
						}

						d += Math.abs( endTime1 -endTime2 );
					}
				}
				assert !iter2.hasNext();

				return d;
			}
		});
	}

	public  NichingSelectorForRemoval(final PlanDistance distanceFunction) {
		super( true );
		this.distanceFunction = distanceFunction;
	}

	@Override
	public double getWeight(
			final Plan indivPlan,
			final ReplanningGroup replanningGroup) {
		// substract the best score, so that al fitnesses are negative
		// and the best one is null. Then pass to the minus.
		final double unscaledWeight = - (indivPlan.getScore()
			- bestScore( indivPlan.getPerson().getPlans() ) );

		final double crowdingFactor =
			calcCrowding(
					indivPlan,
					indivPlan.getPerson().getPlans() );

		return unscaledWeight / crowdingFactor;
	}

	private double calcCrowding(
			final Plan indivPlan,
			final List<? extends Plan> plans) {
		double v = 0;

		for ( Plan p : plans ) {
			if ( p == indivPlan ) continue;
			v += 1 / (1 + distanceFunction.calcDistance( p , indivPlan ));
		}

		return v;
	}

	private double bestScore(final List<? extends Plan> plans) {
		double best = Double.NEGATIVE_INFINITY;

		for ( Plan p : plans ) {
			// if score null, NPE
			final double score = p.getScore();
			if ( score > best ) best = score;
		}

		return best;
	}

	public static interface PlanDistance {
		public double calcDistance( Plan p1 , Plan p2 );
	}
}

