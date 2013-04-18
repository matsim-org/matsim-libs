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
package playground.thibautd.socnetsim.cliques.replanning.modules.jointtimemodechooser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.tsplanoptimizer.framework.Solution;
import playground.thibautd.tsplanoptimizer.framework.Value;
import playground.thibautd.tsplanoptimizer.framework.ValueImpl;
import playground.thibautd.utils.PlanAnalyzeSubtours;
import playground.thibautd.utils.RoutingUtils;

/**
 * Similar to the individual version.
 * Uses an encoding by activity durations.
 *
 * @author thibautd
 */
public class JointTimeModeChooserSolution implements Solution<JointPlan> {
	private static final Logger log =
		Logger.getLogger(JointTimeModeChooserSolution.class);

	private static enum SharedStatus { driver , passenger , alone }

	private final static Collection<String> chainBasedModes = Arrays.asList( TransportMode.car , TransportMode.bike );
	private final static double SCENARIO_DUR = 24 * 3600;
	private final JointPlan plan;
	private final PlanAlgorithm planRouter;
	private final TripRouter tripRouter;

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
			final PlanRoutingAlgorithmFactory fact,
			final TripRouter tripRouter) {
		this(
			plan,
			extractValues( plan , tripRouter , new MainModeIdentifierImpl() ),
			fact.createPlanRoutingAlgorithm( tripRouter ),
			tripRouter);
	}

	private JointTimeModeChooserSolution(
			final JointPlan plan,
			final Values values,
			final PlanAlgorithm planRouter,
			final TripRouter tripRouter) {
		this.plan = plan;
		this.values = values;
		this.planRouter = planRouter;
		this.tripRouter = tripRouter;
	}

	@Override
	public List<? extends Value> getGenotype() {
		return values.flattenValues();
	}

	public List<Integer> getLastValuePerPlan() {
		List<Integer> indices = new ArrayList<Integer>();

		int index = -1;
		for (List<Value> planValues : values.values) {
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
			Iterator<Value> valuesIterator = valueListsIter.next().iterator();

			while (valuesIterator.hasNext()) {
				Value val = valuesIterator.next();
				PlanElement pe = pes.next();

				if (pe instanceof Subtour) {
					if ( !((Subtour) pe).isCarAllowed() &&
						TransportMode.car.equals( val.getValue() )) {
						return false;
					}
					if ( ((Subtour) pe).getModeRestriction() != null &&
							!((Subtour) pe).getModeRestriction().equals( val.getValue() ) ) {
						return false;
					}
					if (chainBasedModes.contains( val.getValue() )) {
						int parent = ((SubtourValue) val).getParentSubtourValueIndex();
						List<? extends Value> representation = getGenotype();
						while (parent >= 0) {
							SubtourValue parVal = (SubtourValue) representation.get( parent );
							if ( !parVal.getValue().equals( val.getValue() ) ) {
								return false;
							}
							parent = parVal.getParentSubtourValueIndex();
						}
					}
				}
			}
		}
		return true;
	}

	public boolean fitsInScenarioDuration() {
		Iterator<List<Value>> valueListsIter = values.values.iterator();

		while (valueListsIter.hasNext()) {
			Iterator<Value> valuesIterator = valueListsIter.next().iterator();

			double dur = 0;
			while (valuesIterator.hasNext()) {
				Object val = valuesIterator.next().getValue();

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
	public JointPlan getPhenotype() {
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
			List<PlanElement> routed = RoutingUtils.route(
					planRouter,
					individualPlan.getFirst().getPerson(),
					individualPlan.getSecond() );
			individualPlan.getFirst().getPlanElements().clear();
			individualPlan.getFirst().getPlanElements().addAll( routed );
		}

		// make sure time are consistent (activities start at the arrival time, etc.)
		enforceTimeConsistency( plan );

		// sufficient, as the referenced elements are the ones of the plan.
		return plan;
	}

	@Override
	public JointTimeModeChooserSolution createClone() {
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
				planRouter,
				tripRouter);
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static Values extractValues(
			final JointPlan plan,
			final TripRouter tripRouter,
			final MainModeIdentifier mainModeIdentifier) {
		List<List<Value>> groupValues = new ArrayList<List<Value>>();
		List<List<PlanElement>> groupCodedPlanElements = new ArrayList<List<PlanElement>>();
		List<Tuple<Plan , List<PlanElement>>> planStructures = new ArrayList<Tuple<Plan , List<PlanElement>>>();
		
		int currentValueIndex = 0;
		for (Plan individualPlan : plan.getIndividualPlans().values()) {
			boolean isCarAvailable = !"never".equals( ((PersonImpl) individualPlan.getPerson()).getCarAvail() );

			List<Value> values = new ArrayList<Value>();
			List<PlanElement> codedPlanElements = new ArrayList<PlanElement>();
			List<PlanElement> planStructure =
					RoutingUtils.tripsToLegs(
						individualPlan,
						tripRouter.getStageActivityTypes(),
						mainModeIdentifier);

			double now = 0;
			double lastNow = 0;
			((Activity) planStructure.get( 0 )).setStartTime( Time.UNDEFINED_TIME );
			((Activity) planStructure.get( planStructure.size() - 1 )).setEndTime( Time.UNDEFINED_TIME );
			final Iterator<PlanElement> planIter = individualPlan.getPlanElements().iterator();
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
						values.add( new ValueImpl( value ) );
						currentValueIndex++;
						codedPlanElements.add( pe );
						lastNow = now;
					}
					else {
						act.setStartTime( Time.UNDEFINED_TIME );
						act.setEndTime( Time.UNDEFINED_TIME );
						act.setMaximumDuration( 0 );
					}
				}
				else if ( ((Leg) pe).getMode().equals( JointActingTypes.DRIVER ) ) {
					PlanElement planLeg = planIter.next();
					while ( !(planLeg instanceof Leg) ||
							!((Leg) planLeg).getMode().equals( JointActingTypes.DRIVER ) ) {
						planLeg = planIter.next();
					}

					final Leg l = ((Leg) planLeg);
					// the cast is not necessary, but acts as a type check
					final DriverRoute planRoute = (DriverRoute) l.getRoute();
					((Leg) pe).setRoute( planRoute.clone() );
				}
				else if ( ((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) {
					PlanElement planLeg = planIter.next();
					while ( !(planLeg instanceof Leg) ||
							!((Leg) planLeg).getMode().equals( JointActingTypes.PASSENGER ) ) {
						planLeg = planIter.next();
					}

					final Leg l = ((Leg) planLeg);
					// the cast is not necessary, but acts as a type check
					final PassengerRoute planRoute = (PassengerRoute) l.getRoute();
					((Leg) pe).setRoute( planRoute.clone() );
				}
			}

			List<Subtour> subtours = analyseSubtours( isCarAvailable , planStructure );
			int[] subtourIndicesInValues = new int[ subtours.size() ];
			for (Subtour subtour : subtours) {
				codedPlanElements.add( subtour );
				subtourIndicesInValues[ subtour.getSubtourNumber() ] = currentValueIndex;
				currentValueIndex++;
			}
			for (Subtour subtour : subtours) {
				int parent = subtour.getParentSubtourNumber();
				int parentIndex = parent > 0 ? subtourIndicesInValues[ parent ] : -1;
				values.add( new SubtourValue( parentIndex , subtour.getMode() ) );
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

	private void enforceTimeConsistency(final JointPlan planToActOn) {
		for (Map.Entry< Id , Plan > entry : planToActOn.getIndividualPlans().entrySet()) {
			Id currentAgent = entry.getKey();
			List<PlanElement> planElements = entry.getValue().getPlanElements();
			double now = 0;
			for (PlanElement pe : planElements) {
				if (pe instanceof Activity) {
					if ( now == Time.UNDEFINED_TIME ) {
						throw new RuntimeException( "got an undefined start time for plan element "+pe+" in plan "+planToActOn );
					}
					Activity act = (Activity) pe;
					// System.out.println( "now="+Time.writeTime( now ) );

					if ( !tripRouter.getStageActivityTypes().isStageActivity( act.getType() ) ) {
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
							planToActOn,
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
		// FIXME: invalid if several joint trips with the same OD (the
		// tt for the first driver trip will always be considered)
		assert driver != null;
		for (PlanElement pe : plan.getIndividualPlans().get( driver ).getPlanElements()) {
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

		throwDriverNotFoundException( plan , origin, destination, driver, passenger );
		return Time.UNDEFINED_TIME;
	}

	private static synchronized void throwDriverNotFoundException(
			final JointPlan plan,
			final Id origin,
			final Id destination,
			final Id driver,
			final Id passenger) {
		log.error( " COULD NOT FIND A VALID DRIVER TRIP FOR PLAN "+plan+"! " );
		log.error( "passenger: "+passenger+", with OD "+origin+" -> "+destination );
		log.error( "with plan:" );
		int i=1;
		for (PlanElement pe : plan.getIndividualPlans().get( passenger ).getPlanElements()) {
			log.error( (i++)+": "+pe );
		}
		log.error( "driver: "+driver);
		log.error( "with plan:" );
		i=1;
		for (PlanElement pe : plan.getIndividualPlans().get( driver ).getPlanElements()) {
			log.error( (i++)+": "+pe );
		}
		throw new RuntimeException( "could not find a valid driver trip!" );
	}

	private static List<Subtour> analyseSubtours(
			final boolean isCarAvailable,
			final List<PlanElement> planStructure) {
		PlanAnalyzeSubtours analyzer = new PlanAnalyzeSubtours( planStructure );

		Map<Integer, List<Leg>> subtours = new HashMap<Integer, List<Leg>>();

		int[] subtourIndexation = analyzer.getSubtourIndexation();
		for (int i=0; i<subtourIndexation.length; i++) {
			Leg l = (Leg) planStructure.get( i*2 + 1 );
			List<Leg> s = subtours.get( subtourIndexation[ i ] );

			if (s == null) {
				s = new ArrayList<Leg>();
				subtours.put( subtourIndexation[ i ] , s );
			}

			s.add( l );
		}

		List<Subtour> out = new ArrayList<Subtour>();
		List<Integer> parentSubtours = analyzer.getParentTours();
		for (Map.Entry<Integer, List<Leg>> entry : subtours.entrySet()) {
			final int subtourIndex = entry.getKey();
			final Integer parentSubtourIndex = parentSubtours.get( subtourIndex );
			final List<Leg> subtour = entry.getValue();
			SharedStatus status = isSharedSubtour( subtour );
			Subtour subtourElement;

			switch (status) {
				case driver:
					List<Leg> njl = getNonJointLegs( subtour );
					subtourElement = new Subtour(
							subtourIndex,
							parentSubtourIndex == null ? -1 : parentSubtourIndex,
							isCarAvailable,
							TransportMode.car,
							njl );
					break;
				case passenger:
					List<Leg> npl = getNonJointLegs( subtour );
					subtourElement = new Subtour(
							subtourIndex,
							parentSubtourIndex == null ? -1 : parentSubtourIndex,
							false ,
							null,
							npl );
					break;
				case alone:
					subtourElement = new Subtour(
							subtourIndex,
							parentSubtourIndex == null ? -1 : parentSubtourIndex,
							isCarAvailable,
							null,
							subtour );
					break;
				default:
					throw new RuntimeException( "Unknown: "+status );
			}

			out.add( subtourElement );
		}

		return out;
	}

	private static SharedStatus isSharedSubtour(final List<Leg> subtour) {
		for (Leg l : subtour) {
			final String mode = l.getMode();
			// assume there is no subtour with both driver and passenger legs.
			// This is ok, as we use the subtours without sub-subtours (so that
			// having passenger and driver in the same subtour would be inconsistent)
			if ( mode.equals( JointActingTypes.PASSENGER ) ) {
				return SharedStatus.passenger;
			}
			else if ( mode.equals( JointActingTypes.DRIVER ) ) {
				return SharedStatus.driver;
			}
		}
		return SharedStatus.alone;
	}

	private static List<Leg> getNonJointLegs(final List<Leg> legs) {
		List<Leg> out = new ArrayList<Leg>();

		for (Leg leg : legs) {
			if (!leg.getMode().equals( JointActingTypes.PASSENGER ) &&
				!leg.getMode().equals( JointActingTypes.DRIVER )	) {
				out.add( leg );
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
		private final String modeRestriction;
		private final int subtourNumber;
		private final int parentSubtourNumber;

		public Subtour(
				final int subtourNumber,
				final int parentSubtourNumber,
				final boolean isCarAvailable,
				final String modeRestriction,
				final List<Leg> legs) {
			this.isCarAvailable = isCarAvailable;
			this.legs.addAll( legs );
			this.subtourNumber = subtourNumber;
			this.parentSubtourNumber = parentSubtourNumber;
			this.modeRestriction = modeRestriction;
		}

		public void setMode(final String mode) {
			for (Leg leg : legs) {
				leg.setMode( mode );
			}
		}

		public String getMode() {
			String mode = "undefined";
			Iterator<Leg> i = legs.iterator();
			boolean toContinue = i.hasNext();

			while (toContinue) {
				mode = i.next().getMode();

				toContinue = i.hasNext() &&
					(mode.equals( JointActingTypes.DRIVER ) ||
					mode.equals( JointActingTypes.PASSENGER )); 
			}

			return mode;
		}

		public String getModeRestriction() {
			return modeRestriction;
		}

		public boolean isCarAllowed() {
			return isCarAvailable;
		}

		public int getSubtourNumber() {
			return subtourNumber;
		}

		public int getParentSubtourNumber() {
			return parentSubtourNumber;
		}
	}

	public static class SubtourValue implements Value {
		private final int parentSubtourValueIndex;
		private String value;

		private SubtourValue(
				final int p,
				final String v) {
			this.parentSubtourValueIndex = p;
			this.value = v;
		}

		public int getParentSubtourValueIndex() {
			return parentSubtourValueIndex;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public String setValue(final Object newValue) {
			if ( !(newValue instanceof String) ) {
				throw new IllegalArgumentException( "argument must be a String, got a "+newValue.getClass()+": "+newValue );
			}
			String old = value;
			value = (String) newValue;
			return old;
		}

		@Override
		public Value createClone() {
			return new SubtourValue( parentSubtourValueIndex , value );
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
