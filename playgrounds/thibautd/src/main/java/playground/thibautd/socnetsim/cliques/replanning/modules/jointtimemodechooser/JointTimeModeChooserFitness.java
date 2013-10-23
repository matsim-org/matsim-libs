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
package playground.thibautd.socnetsim.cliques.replanning.modules.jointtimemodechooser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.tsplanoptimizer.framework.FitnessFunction;
import playground.thibautd.tsplanoptimizer.framework.Solution;

/**
 * @author thibautd
 */
public class JointTimeModeChooserFitness implements FitnessFunction<JointPlan> {
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
	public double computeFitnessValue(final Solution<? extends JointPlan> solution) {
		JointPlan plan = solution.getPhenotype();

		if (DEBUG) log.debug( "start scoring" );

		double accumulatedNegativeDuration = 0;

		JointTimes jointTimes = new JointTimes();

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

					Route r = ((Leg) pe).getRoute();
					if (r instanceof DriverRoute) {
						jointTimes.notifyDriverStartTime(
								individualPlan.getPerson().getId(),
								((DriverRoute) r).getPassengersIds(),
								((Leg) pe).getDepartureTime());
					}
					else if (r instanceof PassengerRoute) {
						jointTimes.notifyPassengerStartTime(
								individualPlan.getPerson().getId(),
								((PassengerRoute) r).getDriverId(),
								((Leg) pe).getDepartureTime());
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

		throw new RuntimeException( "JointPlan.getScore() does not exist anymore "+
				// this makes the message confusing, but avoids errors for unused fields
				((accumulatedNegativeDuration * negativeDurationPenalty) -
				(jointTimes.getCumulatedUnsynchronisedTime() * unsynchronizedPenalty)));
//		return  plan.getScore() +
//			(accumulatedNegativeDuration * negativeDurationPenalty) -
//			(jointTimes.getCumulatedUnsynchronisedTime() * unsynchronizedPenalty);
	}

	private static class JointTimes {
		private final Map< Tuple<Id, Id> , List<Double> > driverDepartures = new HashMap< Tuple<Id,Id>, List<Double> >();
		private final Map< Tuple<Id, Id> , List<Double> > passengerDepartures = new HashMap< Tuple<Id,Id>, List<Double> >();
		
		public void notifyDriverStartTime(
				final Id driverId,
				final Collection<Id> passengersIds,
				final double time) {
			for (Id passenger : passengersIds) {
				Tuple<Id, Id> tuple = new Tuple<Id, Id>( driverId , passenger );
				List<Double> times = driverDepartures.get( tuple );
				if (times == null) {
					times = new ArrayList<Double>();
					driverDepartures.put( tuple , times );
				}
				times.add( time );
			}
		}

		public void notifyPassengerStartTime(
				final Id passengerId,
				final Id driverId,
				final double time) {
			Tuple<Id, Id> tuple = new Tuple<Id, Id>( driverId , passengerId );
			List<Double> times = passengerDepartures.get( tuple );
			if (times == null) {
				times = new ArrayList<Double>();
				passengerDepartures.put( tuple , times );
			}
			times.add( time );
		}

		public double getCumulatedUnsynchronisedTime() {
			double cumul = 0;
			for (Map.Entry< Tuple<Id, Id> , List<Double> > entry : driverDepartures.entrySet()) {
				Iterator<Double> passengerTimes = passengerDepartures.get( entry.getKey() ).iterator();
				Iterator<Double> driverTimes = entry.getValue().iterator();

				while (driverTimes.hasNext()) {
					double d = driverTimes.next();
					double p = passengerTimes.next();

					cumul += Math.abs( p - d );
				}
			}
			return cumul;
		}
	}
}
