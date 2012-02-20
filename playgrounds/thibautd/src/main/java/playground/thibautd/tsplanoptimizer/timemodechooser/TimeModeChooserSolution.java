/* *********************************************************************** *
 * project: org.matsim.*
 * TimeModeChooserSolution.java
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
package playground.thibautd.tsplanoptimizer.timemodechooser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.router.PlanRouter;
import playground.thibautd.router.TripRouter;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.Value;
import playground.thibautd.tsplanoptimizer.framework.ValueImpl;

/**
 * Represents a plan for which activity end times are optimised.
 *
 * @author thibautd
 */
public class TimeModeChooserSolution implements Solution {
	private final Plan plan;
	private final PlanRouter planRouter;

	// maintain two lists: the values, and the plan elements they are
	// related to, in the same order. Compared to a map, this allows to
	// ensure iteration order.
	private final List<Value> values;
	private final List<PlanElement> elements;

	/**
	 * Creates a solution for the given plan.
	 * @param plan the plan to represent
	 * @param tripRouter the instance to use to create trips
	 * with proper travel times. It should not be the one returned
	 * by the controler (which would compute new routes at each estimation).
	 */
	public TimeModeChooserSolution(
			final Plan plan,
			final TripRouter tripRouter) {
		this.plan = plan;
		this.planRouter = new PlanRouter( tripRouter );

		Tuple<List<Value>, List<PlanElement>> tuple = extractValues( plan );

		this.values = tuple.getFirst();
		this.elements = tuple.getSecond();
	}

	private TimeModeChooserSolution(
			final Plan plan,
			final List<Value> values,
			final List<PlanElement> elements,
			final PlanRouter planRouter) {
		this.plan = plan;
		this.values = values;
		this.elements = elements;
		this.planRouter = planRouter;
	}

	@Override
	public List<? extends Value> getRepresentation() {
		return values;
	}

	@Override
	public Plan getRepresentedPlan() {
		Iterator<Value> valuesIter = values.iterator();
		Iterator<PlanElement> elementsIter = elements.iterator();

		while (valuesIter.hasNext()) {
			int endTime = (Integer) valuesIter.next().getValue();
			((Activity) elementsIter.next()).setEndTime( endTime );
		}

		planRouter.run( plan );

		// make sure time are consistent (activities start at the arrival time, etc.)
		enforceTimeConsistency( plan );

		// sufficient, as the referenced elements are the ones of the plan.
		return plan;
	}

	@Override
	public Solution createClone() {
		List<Value> newValues = new ArrayList<Value>();

		for (Value val : values) {
			newValues.add( val.createClone() );
		}

		return new TimeModeChooserSolution(
				plan,
				Collections.unmodifiableList( newValues ),
				elements,
				planRouter);
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static Tuple<List<Value>, List<PlanElement>> extractValues(final Plan plan) {
		List<Value> values = new ArrayList<Value>();
		List<PlanElement> planElements = new ArrayList<PlanElement>();

		double now = 0;
		for (PlanElement pe : plan.getPlanElements().subList(0 , plan.getPlanElements().size() - 1)) {
			now = updateNow( now , pe );
			if (pe instanceof Activity) {
				if (now == Time.UNDEFINED_TIME) {
					throw new RuntimeException( "cannot infer activitiy end time: "+pe );
				}

				values.add( new ValueImpl<Integer>( (int) now ) );
				planElements.add( pe );
			}
		}

		return new Tuple<List<Value>, List<PlanElement>>(
				Collections.unmodifiableList( values ),
				Collections.unmodifiableList( planElements ));
	}

	private static double updateNow(
			final double now,
			final PlanElement pe) {
		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			double endTime = act.getEndTime();
			double startTime = act.getStartTime();
			double dur = act.getMaximumDuration();
			if (endTime != Time.UNDEFINED_TIME) {
				// use fromAct.endTime as time for routing
				return endTime;
			}
			else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
				// use fromAct.startTime + fromAct.duration as time for routing
				return startTime + dur;
			}
			else if (dur != Time.UNDEFINED_TIME) {
				// use last used time + fromAct.duration as time for routing
				return now + dur;
			}
			else {
				return Time.UNDEFINED_TIME;
				//throw new RuntimeException("activity has neither end-time nor duration." + act);
			}
		}
		else {
			double tt = ((Leg) pe).getTravelTime();

			if (tt == Time.UNDEFINED_TIME) {
				Route route = ((Leg) pe).getRoute();
				
				if ( route != null ) {
					tt = route.getTravelTime();
				}

				if (tt == Time.UNDEFINED_TIME) {
					tt = 0;
				}
			}

			return now + tt;
		}
	}	

	private static void enforceTimeConsistency(final Plan plan) {
		double now = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				((Activity) pe).setStartTime( now );

				double endTime = ((Activity) pe).getEndTime();
				if (endTime != Time.UNDEFINED_TIME) {
					((Activity) pe).setMaximumDuration( endTime - now );
				}
			}
			now = updateNow( now , pe );
		}
	}
}

