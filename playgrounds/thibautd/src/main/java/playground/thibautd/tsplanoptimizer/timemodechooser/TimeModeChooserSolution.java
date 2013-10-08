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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.Value;
import playground.thibautd.tsplanoptimizer.framework.ValueImpl;
import playground.thibautd.utils.RoutingUtils;

/**
 * Represents a plan for which activity end times are optimised.
 *
 * @author thibautd
 */
public class TimeModeChooserSolution implements Solution<Plan> {
	private final Plan plan;
	private final PlanRouter planRouter;
	private final MainModeIdentifier mainModeIdentifier;

	// maintain two lists: the values, and the plan elements they are
	// related to, in the same order. Compared to a map, this allows to
	// ensure iteration order.
	//private final List<Value> values;
	//private final List<PlanElement> elements;
	private final Values values;

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
		this(
			plan,
			extractValues( plan , tripRouter , new MainModeIdentifierImpl() ),
			new PlanRouter( tripRouter ),
			new MainModeIdentifierImpl());
	}

	private TimeModeChooserSolution(
			final Plan plan,
			final Values values,
			final PlanRouter planRouter,
			final MainModeIdentifier mainModeIdentifer) {
		this.plan = plan;
		this.values = values;
		this.planRouter = planRouter;
		this.mainModeIdentifier = mainModeIdentifer;
	}

	@Override
	public List<? extends Value> getGenotype() {
		return values.values;
	}

	@Override
	public Plan getPhenotype() {
		Iterator<Value> valuesIter = values.values.iterator();
		Iterator<PlanElement> elementsIter = values.associatedPlanElements.iterator();

		while (valuesIter.hasNext()) {
			Value value = valuesIter.next();
			PlanElement pe = elementsIter.next();

			if (pe instanceof Activity) {
				int endTime = (Integer) value.getValue();
				((Activity) pe).setEndTime( endTime );
			}
			else {
				((Subtour) pe).setMode( (String) value.getValue() );
			}
		}

		plan.getPlanElements().clear();
		plan.getPlanElements().addAll( values.planStructure );
		planRouter.run( plan );

		// make sure time are consistent (activities start at the arrival time, etc.)
		enforceTimeConsistency( plan );

		// System.out.println(  );
		// System.out.println( plan.getPlanElements() );

		// sufficient, as the referenced elements are the ones of the plan.
		return plan;
	}

	@Override
	public TimeModeChooserSolution createClone() {
		List<Value> newValues = new ArrayList<Value>();

		for (Value val : values.values) {
			newValues.add( val.createClone() );
		}

		return new TimeModeChooserSolution(
				plan,
				new Values(
					Collections.unmodifiableList( newValues ),
					values.associatedPlanElements,
					values.planStructure),
				planRouter,
				mainModeIdentifier);
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static Values extractValues(
			final Plan plan,
			final TripRouter tripRouter,
			final MainModeIdentifier mainModeIdentifier) {
		List<Value> values = new ArrayList<Value>();
		List<PlanElement> planStructure =
				RoutingUtils.tripsToLegs(
						plan,
						tripRouter.getStageActivityTypes(),
						mainModeIdentifier);
		List<PlanElement> codedPlanElements = new ArrayList<PlanElement>();
		
		double now = 0;
		int lastValue = Integer.MIN_VALUE;
		for (PlanElement pe : planStructure.subList(0 , planStructure.size() - 1)) {
			now = updateNow( now , pe );

			if (pe instanceof Activity) {
				if (now == Time.UNDEFINED_TIME) {
					throw new RuntimeException( "cannot infer activitiy end time: "+pe );
				}

				// enforce the values to be consistent (ie end time do not produce negative durations)
				int value = Math.max( (int) now , lastValue );
				lastValue = value;
				values.add( new ValueImpl( value ) );
				codedPlanElements.add( pe );
			}
		}

		for (List<PlanElement> subtour : analyseSubtours( planStructure )) {
			Subtour subtourElement = new Subtour( subtour );
			codedPlanElements.add( subtourElement );
			values.add( new ValueImpl( subtourElement.getMode() ) );
		}

		return new Values(
				Collections.unmodifiableList( values ),
				Collections.unmodifiableList( codedPlanElements ),
				Collections.unmodifiableList( planStructure ));
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

	private void enforceTimeConsistency(final Plan planToActOn) {
		double now = 0;
		for (PlanElement pe : planToActOn.getPlanElements()) {
			if (pe instanceof Activity) {
				if ( now == Time.UNDEFINED_TIME ) {
					throw new RuntimeException( "got an undefined score for plan element "+pe+" in plan "+planToActOn.getPlanElements() );
				}
				Activity act = (Activity) pe;

				if ( !planRouter.getTripRouter().getStageActivityTypes().isStageActivity( act.getType() ) ) {
					// router is supposed to set the time properly for activities.
					// setting time consistencies for them could break some
					// properties of the trip (for example, pt interactions have
					// no start nor end time)
					act.setStartTime( now );

					double endTime = act.getEndTime();
					if (endTime != Time.UNDEFINED_TIME) {
						act.setMaximumDuration( endTime - now );
					}
				}
			}
			now = updateNow( now , pe );
		}
	}

	private static List<List<PlanElement>> analyseSubtours(
			final List<PlanElement> planStructure) {
		final Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtours(planStructure, EmptyStageActivityTypes.INSTANCE);

		// XXX this is a quick adaptation to the new way of presenting results
		final List< List<PlanElement> > elements = new ArrayList<List<PlanElement>>();

		for (TripStructureUtils.Subtour s : subtours) {
			final List<PlanElement> subtourElements = new ArrayList<PlanElement>();
			elements.add( subtourElements );
			int i = 0;
			for ( Trip t : s.getTrips() ) {
				if ( i++ == 0 ) subtourElements.add( t.getOriginActivity() );
				subtourElements.addAll( t.getTripElements() );
				subtourElements.add( t.getDestinationActivity() );
			}
		}

		return elements;
	}

	// /////////////////////////////////////////////////////////////////////////
	// class
	// /////////////////////////////////////////////////////////////////////////
	// allows to set mode for a whole subtour in the plan structure
	private static class Subtour implements PlanElement {
		private final List<Leg> legs = new ArrayList<Leg>();

		public Subtour(final List<PlanElement> elements) {
			for (PlanElement pe : elements) {
				if (pe instanceof Leg) {
					legs.add( (Leg) pe );
				}
			}
		}

		public void setMode(final String mode) {
			for (Leg leg : legs) {
				leg.setMode( mode );
			}
		}

		public String getMode() {
			return legs.get(0).getMode();
		}
	}

	private static class Values {
		public final List<Value> values;
		public final List<PlanElement> associatedPlanElements;
		public final List<PlanElement> planStructure;

		public Values(
				final List<Value> values,
				final List<PlanElement> associatedPlanElements,
				final List<PlanElement> planStructure) {
			this.values = values;
			this.associatedPlanElements = associatedPlanElements;
			this.planStructure = planStructure;
		}
	}
}

