/* *********************************************************************** *
 * project: org.matsim.*
 * JointTimeModeChooserFitness.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.replanning.modules.jointtimemodechooser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.jointtrips.population.JointLeg;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.tsplanoptimizer.framework.FitnessFunction;
import playground.thibautd.tsplanoptimizer.framework.Solution;

/**
 * @author thibautd
 */
public class JointTimeModeChooserFitness implements FitnessFunction {
	private static final Logger log =
		Logger.getLogger(JointTimeModeChooserFitness.class);

	// print a lot of information.
	// to use with one thread only.
	private final static boolean DEBUG = false;
	private final ScoringFunctionFactory factory;
	private final double negativeDurationPenalty;
	private final double unsynchronizedPenalty;

	//private final Map<String, Double> cache = new HashMap<String, Double>();
	//private int n = 0;
	//private int nc = 0;

	//public void finalize() {
	//	System.out.println( nc +" / "+n );
	//}

	public JointTimeModeChooserFitness(
			final double negativeDurationPenalty,
			final double unsynchronizedPenalty,
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.negativeDurationPenalty = negativeDurationPenalty;
		this.unsynchronizedPenalty = unsynchronizedPenalty;
		this.factory = scoringFunctionFactory;
	}

	@Override
	public double computeFitnessValue(final Solution solution) {
	//	String key = toKey( solution );
	//	Double cachedScore = cache.get( key );

	//	n++;
	//	if (cachedScore == null) {
	//		nc++;
	//		cachedScore = reallyComputeFitness( solution );
	//		cache.put( key , cachedScore );
	//	}

	//	return cachedScore;
	//}

	//private static String toKey(final Solution solution) {
	//	StringBuffer buffer = new StringBuffer();

	//	for (Value val : solution.getRepresentation()) {
	//		buffer.append( "-&-"+val.getValue() );
	//	}

	//	return buffer.toString();
	//}

	//private double reallyComputeFitness(final Solution solution) {
		JointPlan plan = (JointPlan) solution.getRepresentedPlan();

		if (DEBUG) log.debug( "start scoring" );

		double accumulatedNegativeDuration = 0;
		LinkedList<JointLeg> sharedLegs = new LinkedList<JointLeg>();

		for (Plan individualPlan : plan.getIndividualPlans().values()) {
			double score;
			ScoringFunction scoringFunction = factory.createNewScoringFunction( individualPlan );

			for (PlanElement pe : individualPlan.getPlanElements()) {
				if (DEBUG) log.debug( "handle plan element "+pe );

				if (pe instanceof Activity) {
					scoringFunction.handleActivity( (Activity) pe );

					double duration = ((Activity) pe).getEndTime() - ((Activity) pe).getStartTime();
					if (duration != Time.UNDEFINED_TIME && duration < 0) {
						// all matsim scoring functions do not take that into account
						accumulatedNegativeDuration += duration;
					}
				}
				else if (pe instanceof Leg) {
					scoringFunction.handleLeg( (Leg) pe );

					if (((JointLeg) pe).getJoint()) {
						sharedLegs.add( (JointLeg) pe );
					}
				}
				else {
					throw new RuntimeException( "unknown PlanElement type "+pe.getClass() );
				}
			}

			scoringFunction.finish();

			score = scoringFunction.getScore();
			if (DEBUG) {
				log.debug( "individual score: "+score );
			}

			if (Double.isNaN( score )) {
				throw new RuntimeException( "got a NaN score for plan "+plan );
			}

			individualPlan.setScore( score );
		}

		double accumulatedUnsynchronizedTime = 0;
		while (sharedLegs.size() > 0) {
			List<JointLeg> linkedLegs = new ArrayList<JointLeg>();
			JointLeg leg = sharedLegs.removeFirst();
			linkedLegs.addAll( leg.getLinkedElements().values() );

			for (JointLeg linkedLeg : linkedLegs) {
				sharedLegs.remove( linkedLeg );
			}

			linkedLegs.add( leg );

			double minDepartureTime = Double.POSITIVE_INFINITY;
			double maxDepartureTime = Double.NEGATIVE_INFINITY;

			for (JointLeg currLeg : linkedLegs) {
				minDepartureTime = Math.min( minDepartureTime , currLeg.getDepartureTime() );
				maxDepartureTime = Math.max( maxDepartureTime , currLeg.getDepartureTime() );
			}

			accumulatedUnsynchronizedTime += maxDepartureTime - minDepartureTime;
		}

		if (DEBUG) {
			log.debug( "scoring ended." );
			log.debug( "score: "+plan.getScore() );
			log.debug( "accumulated negative duration: "+accumulatedNegativeDuration );
			log.debug( "negative duration penalty: "+(accumulatedNegativeDuration * negativeDurationPenalty) );
			log.debug( "accumulated unsynchronized duration: "+accumulatedUnsynchronizedTime );
			log.debug( "unsynchronized duration penalty: "+(accumulatedUnsynchronizedTime * unsynchronizedPenalty) );
		}

		return plan.getScore() +
			(accumulatedNegativeDuration * negativeDurationPenalty) -
			(accumulatedUnsynchronizedTime * unsynchronizedPenalty);
	}
}
