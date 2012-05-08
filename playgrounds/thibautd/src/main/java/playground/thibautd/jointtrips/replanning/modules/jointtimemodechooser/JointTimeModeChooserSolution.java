/* *********************************************************************** *
 * project: org.matsim.*
 * JointTimeModeChooserSolution.java
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
import java.util.Collections;
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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanomatConfigGroup.TripStructureAnalysisLayerOption;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

import playground.thibautd.jointtrips.population.DriverRoute;
import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.population.PassengerRoute;
import playground.thibautd.jointtrips.router.JointPlanRouter;
import playground.thibautd.router.TripRouter;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.Value;
import playground.thibautd.tsplanoptimizer.framework.ValueImpl;

/**
 * Similar to the individual version.
 * Uses an encoding by activity durations.
 *
 * @author thibautd
 */
public class JointTimeModeChooserSolution implements Solution {
	private static final Logger log =
		Logger.getLogger(JointTimeModeChooserSolution.class);

	private static enum SharedStatus { driver , passenger , alone }

	private final static double SCENARIO_DUR = 24 * 3600;
	private final JointPlan plan;
	private final JointPlanRouter planRouter;

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
	public JointTimeModeChooserSolution(
			final JointPlan plan,
			final TripRouter tripRouter) {
		this(
			plan,
			extractValues( plan , tripRouter ),
			new JointPlanRouter( tripRouter ));
	}

	private JointTimeModeChooserSolution(
			final JointPlan plan,
			final Values values,
			final JointPlanRouter planRouter) {
		this.plan = plan;
		this.values = values;
		this.planRouter = planRouter;
	}

	@Override
	public List<? extends Value> getRepresentation() {
		return values.flattenValues();
	}

	public List<Integer> getLastValuePerPlan() {
		List<Integer> indices = new ArrayList<Integer>();

		int index = -1;
		for (List planValues : values.values) {
			index += planValues.size();
			indices.add( index );
		}

		return indices;
	}

	public boolean respectsModeConstraints() {
		Iterator<List<PlanElement>> peListsIter = values.associatedPlanElements.iterator();
		Iterator<List<Value>> valueListsIter = values.values.iterator();

		while (valueListsIter.hasNext()) {
			Iterator<PlanElement> pes = peListsIter.next().iterator();
			Iterator<Value> values = valueListsIter.next().iterator();

			while (values.hasNext()) {
				Value val = values.next();
				PlanElement pe = pes.next();

				if (pe instanceof Subtour &&
						!((Subtour) pe).isCarAllowed() &&
						TransportMode.car.equals( val.getValue() )) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean fitsInScenarioDuration() {
		Iterator<List<Value>> valueListsIter = values.values.iterator();

		while (valueListsIter.hasNext()) {
			Iterator<Value> values = valueListsIter.next().iterator();

			double dur = 0;
			while (values.hasNext()) {
				Object val = values.next().getValue();

				if (val instanceof Integer) {
					dur += (Integer) val;
					if (dur > SCENARIO_DUR) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public Plan getRepresentedPlan() {
		//log.warn( "TODO: synchronisation! (do it in a joint router?)" );
		Iterator<List<Value>> individualValues = values.values.iterator();
		Iterator<List<PlanElement>> individualElements =  values.associatedPlanElements.iterator();

		while (individualValues.hasNext()) {
			Iterator<Value> valuesIter = individualValues.next().iterator();
			Iterator<PlanElement> elementsIter = individualElements.next().iterator();

			double now = 0;
			while (valuesIter.hasNext()) {
				Value value = valuesIter.next();
				PlanElement pe = elementsIter.next();

				if (pe instanceof Activity) {
					int duration = (Integer) value.getValue();
					((Activity) pe).setMaximumDuration( duration );
					now += duration;
					((Activity) pe).setEndTime( now );
				}
				else {
					((Subtour) pe).setMode( (String) value.getValue() );
				}
			}
		}

		for (Tuple<Plan , List<PlanElement>> individualPlan : values.planStructures) {
			List<PlanElement> routed = planRouter.run( individualPlan.getFirst().getPerson() , individualPlan.getSecond() );
			planRouter.updatePlanElements( individualPlan.getFirst() , routed );
		}

		// make sure time are consistent (activities start at the arrival time, etc.)
		enforceTimeConsistency( plan );

		// sufficient, as the referenced elements are the ones of the plan.
		return plan;
	}

	@Override
	public Solution createClone() {
		List<List<Value>> newValues = new ArrayList<List<Value>>();

		for (List<Value> individualValues : values.values) {
			List<Value> newIndivValues = new ArrayList<Value>();

			for (Value val : individualValues) {
				newIndivValues.add( val.createClone() );
			}

			newValues.add( Collections.unmodifiableList( newIndivValues ) );
		}

		return new JointTimeModeChooserSolution(
				plan,
				new Values(
					Collections.unmodifiableList( newValues ),
					values.associatedPlanElements,
					values.planStructures),
				planRouter);
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static Values extractValues(
			final JointPlan plan,
			final TripRouter tripRouter) {
		List<List<Value>> groupValues = new ArrayList<List<Value>>();
		List<List<PlanElement>> groupCodedPlanElements = new ArrayList<List<PlanElement>>();
		List<Tuple<Plan , List<PlanElement>>> planStructures = new ArrayList<Tuple<Plan , List<PlanElement>>>();
		
		for (Plan individualPlan : plan.getIndividualPlans().values()) {
			boolean isCarAvailable = !"never".equals( ((PersonImpl) individualPlan.getPerson()).getCarAvail() );

			List<Value> values = new ArrayList<Value>();
			List<PlanElement> codedPlanElements = new ArrayList<PlanElement>();
			List<PlanElement> planStructure = tripRouter.tripsToLegs( individualPlan.getPlanElements() );

			double now = 0;
			double lastNow = 0;
			((Activity) planStructure.get( 0 )).setStartTime( Time.UNDEFINED_TIME );
			((Activity) planStructure.get( planStructure.size() - 1 )).setEndTime( Time.UNDEFINED_TIME );
			for (PlanElement pe : planStructure.subList(0 , planStructure.size() - 1)) {
				now = updateNow( now , pe );

				if (pe instanceof Activity) {
					if (now == Time.UNDEFINED_TIME) {
						throw new RuntimeException( "cannot infer activitiy end time: "+pe );
					}

					Activity act = (Activity) pe;
					if ( !(act.getType().equals( JointActingTypes.PICK_UP ) ||
								act.getType().equals( JointActingTypes.DROP_OFF )) ) {
						// enforce the values to be consistent (ie end time do not produce negative durations)
						int value = (int) (now - lastNow);
						values.add( new ValueImpl<Integer>( value ) );
						codedPlanElements.add( pe );
						lastNow = now;
					}
					else {
						act.setStartTime( Time.UNDEFINED_TIME );
						act.setEndTime( Time.UNDEFINED_TIME );
						act.setMaximumDuration( 0 );
					}
				}
			}

			for (List<PlanElement> subtour : analyseSubtours( planStructure )) {
				SharedStatus status = isSharedSubtour( subtour );
				Subtour subtourElement = new Subtour( isCarAvailable , subtour );

				switch (status) {
					case driver:
						break;
					case passenger:
						List<PlanElement> npl = getNonPassengerLegs( subtour );
						if (npl.size() > 0) {
							// if no non-passenger leg, do not add anything
							subtourElement = new Subtour( false , npl );
							codedPlanElements.add( subtourElement );
							values.add( new ValueImpl<String>( subtourElement.getMode() ) );
						}
						break;
					case alone:
						subtourElement = new Subtour( isCarAvailable , subtour );
						codedPlanElements.add( subtourElement );
						values.add( new ValueImpl<String>( subtourElement.getMode() ) );
						break;
					default:
						throw new RuntimeException( "Unknown: "+status );
				}
			}

			groupCodedPlanElements.add( Collections.unmodifiableList( codedPlanElements ) );
			groupValues.add( Collections.unmodifiableList( values ) );
			planStructures.add(
					new Tuple<Plan, List<PlanElement>>(
						individualPlan,
						Collections.unmodifiableList( planStructure ) ) );
		}

		return new Values(
				Collections.unmodifiableList( groupValues ),
				Collections.unmodifiableList( groupCodedPlanElements ),
				Collections.unmodifiableList( planStructures ));
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

	private void enforceTimeConsistency(final JointPlan plan) {
		for (Map.Entry< Id , List<PlanElement> > entry : plan.getIndividualPlanElements().entrySet()) {
			Id currentAgent = entry.getKey();
			List<PlanElement> planElements = entry.getValue();
			double now = 0;
			for (PlanElement pe : planElements) {
				if (pe instanceof Activity) {
					if ( now == Time.UNDEFINED_TIME ) {
						throw new RuntimeException( "got an undefined start time for plan element "+pe+" in plan "+plan.getPlanElements() );
					}
					Activity act = (Activity) pe;
					// System.out.println( "now="+Time.writeTime( now ) );

					if ( !planRouter.getTripRouter().getStageActivityTypes().isStageActivity( act.getType() ) ) {
						if ( !( act.getType().equals( JointActingTypes.PICK_UP ) ||
								act.getType().equals( JointActingTypes.DROP_OFF ) ) ) {
							// router is supposed to set the time properly for activities.
							// setting time consistencies for them could break some
							// properties of the trip (for example, pt interactions have
							// no start nor end time)
							// System.out.println( "start="+Time.writeTime( now ) );
							act.setStartTime( now );

							double endTime = act.getEndTime();
							if (endTime != Time.UNDEFINED_TIME) {
								if (endTime < now) {
									endTime = now;
									act.setEndTime( endTime );
								}
								// System.out.println( "end="+Time.writeTime( endTime ) );
								// act.setMaximumDuration( endTime - now );
								act.setMaximumDuration( Time.UNDEFINED_TIME );
							}
						}
						//else {
						//	act.setStartTime( now );
						//	act.setEndTime( now );
						//}
					}
				}
				else {
					((Leg) pe).setDepartureTime( now );
					if ( ((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) {
						PassengerRoute route = (PassengerRoute) ((Leg) pe).getRoute();
						double tt =	searchDriverTravelTime(
							plan,
							route.getStartLinkId(),
							route.getEndLinkId(),
							route.getDriverId(),
							currentAgent);
						((Leg) pe).setTravelTime( tt );
					}
				}
				now = updateNow( now , pe );
			}
			((Activity) planElements.get( 0 )).setStartTime( Time.UNDEFINED_TIME );
		}
	}

	private static double searchDriverTravelTime(
			final JointPlan plan,
			final Id origin,
			final Id destination,
			final Id driver,
			final Id passenger) {
		for (PlanElement pe : plan.getIndividualPlanElements().get( driver )) {
			if (pe instanceof Leg) {
				Route r = ((Leg) pe).getRoute();

				if (r instanceof DriverRoute &&
						r.getStartLinkId().equals( origin ) &&
						r.getEndLinkId().equals( destination ) &&
						((DriverRoute) r).getPassengersIds().contains( passenger )) {
					return r.getTravelTime();
				}
			}
		}
		return Time.UNDEFINED_TIME;
	}

	private static List<List<PlanElement>> analyseSubtours(
			final List<PlanElement> planStructure) {
		PlanAnalyzeSubtours analyzer = new PlanAnalyzeSubtours();
		analyzer.setTripStructureAnalysisLayer( TripStructureAnalysisLayerOption.link );

		// XXX: quick workaround: change that
		Plan fakePlan = new PlanImpl();
		fakePlan.getPlanElements().addAll( planStructure );
		analyzer.run( fakePlan );

		return analyzer.getSubtours();
	}

	private static SharedStatus isSharedSubtour(final List<PlanElement> subtour) {
		boolean hasPickUp = false;
		boolean passenger = false;
		for (PlanElement pe : subtour) {
			if (pe instanceof Activity && ((Activity) pe).getType().equals( JointActingTypes.PICK_UP ) ) {
				hasPickUp = true;
			}
			if (pe instanceof Leg && ((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) {
				passenger = true;
			}
			if (pe instanceof Activity && ((Activity) pe).getType().equals( JointActingTypes.DROP_OFF ) ) {
				if (hasPickUp) return passenger ? SharedStatus.passenger : SharedStatus.driver;
			}
		}
		return SharedStatus.alone;
	}

	private static List<PlanElement> getNonPassengerLegs(final List<PlanElement> pes) {
		List<PlanElement> out = new ArrayList<PlanElement>();

		for (PlanElement pe : pes) {
			if ( pe instanceof Leg &&
					!((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) {
				out.add( pe );
			}
		}

		return out;
	}

	// /////////////////////////////////////////////////////////////////////////
	// class
	// /////////////////////////////////////////////////////////////////////////
	// allows to set mode for a whole subtour in the plan structure
	private static class Subtour implements PlanElement {
		private final List<Leg> legs = new ArrayList<Leg>();
		private final boolean isCarAvailable;

		public Subtour(final boolean isCarAvailable, final List<PlanElement> elements) {
			this.isCarAvailable = isCarAvailable;
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

		public boolean isCarAllowed() {
			return isCarAvailable;
		}
	}

	private static class Values {
		public final List<List<Value>> values;
		public final List<List<PlanElement>> associatedPlanElements;
		public final List< Tuple<Plan, List<PlanElement>> > planStructures;

		public Values(
				final List<List<Value>> values,
				final List<List<PlanElement>> associatedPlanElements,
				final List< Tuple<Plan, List<PlanElement>> > planStructures) {
			this.values = values;
			this.associatedPlanElements = associatedPlanElements;
			this.planStructures = planStructures;
		}

		public List<Value> flattenValues() {
			List<Value> flat = new ArrayList<Value>();

			for (List<Value> elem : values) {
				flat.addAll( elem );
			}

			return flat;
		}
	}
}
