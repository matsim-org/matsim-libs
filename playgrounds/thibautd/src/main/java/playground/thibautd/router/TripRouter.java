/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingHandler.java
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
package playground.thibautd.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.misc.Time;

/**
 * Class acting as an intermediate between clients needing to
 * compute routes and all registered {@link RoutingModule}s.
 * It provides convenience methods to route an individual trip with
 * a desired mode or to identify trips.
 *
 * @author thibautd
 */
public class TripRouter {
	private final Map<String, RoutingModule> routingModules =
		new HashMap<String , RoutingModule>();
	private final CompositeStageActivityTypes checker = new CompositeStageActivityTypes();

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// setters / getters
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Sets the {@link RoutingModule} to use for the given (main) mode.
	 * @param mainMode the mode
	 * @param module the module to use with this mode
	 * @return the previously registered {@link RoutingModule} for this mode if any, null otherwise.
	 */
	public RoutingModule setRoutingModule(
			final String mainMode,
			final RoutingModule module) {
		RoutingModule old = routingModules.put( mainMode , module );

		if (old != null) {
			checker.removeActivityTypes( old.getStageActivityTypes() );
		}
		checker.addActivityTypes( module.getStageActivityTypes() );

		return old;
	}

	/**
	 * Gives access to the stage activity types, for all modes.
	 * @return a {@link StageActivityTypes} considering all registered modules
	 */
	public StageActivityTypes getStageActivityTypes() {
		return checker;
	}

	// /////////////////////////////////////////////////////////////////////////
	// Handling methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Creates a list of plan elements reproducing the plan,
	 * except that multi-legs trips are replaced by one single leg,
	 * with mode set to the main mode of the trip.
	 * The argument plan is not modified.
	 * All references in the return list, except the newly created "dummy"
	 * legs, point towards the plan's instances.
	 *
	 * @param plan the plan to analyse
	 * @return the simplified sequence of plan elements
	 */
	public List<PlanElement> tripsToLegs(final Plan plan) {
		return tripsToLegs( plan.getPlanElements() );
	}

	/**
	 * Actual processing method used by {@link #tripsToLegs(Plan)}. Allows
	 * to analyse any sequence of plan elements, even outside of a plan container.
	 * @param plan the sequence of {@link PlanElement} to analyse.
	 * @return the simplified sequence of plan elements
	 */
	public List<PlanElement> tripsToLegs(final List<PlanElement> plan) {
		return tripsToLegs( plan , checker );
	}

	/**
	 * The same as {@link #tripsToLeg(Plan)}, but allowing to use
	 * a specific {@link StageActivityTypes} instance.
	 * @param plan the plan to analyse
	 * @param checker the checker to use
	 * @return the plan structure. See {@link #tripsToLeg(Plan)} for details.
	 */
	public List<PlanElement> tripsToLegs(final Plan plan, final StageActivityTypes checker) {
		return tripsToLegs( plan.getPlanElements() , checker );
	}

	/**
	 * The same as {@link #tripsToLeg(List)}, but allowing to use
	 * a specific {@link StageActivityTypes} instance.
	 * @param plan the plan to analyse
	 * @param checker the checker to use
	 * @return the plan structure. See {@link #tripsToLeg(Plan)} for details.
	 */
	public List<PlanElement> tripsToLegs(final List<PlanElement> plan, final StageActivityTypes checker) {
		List<PlanElement> simplifiedPlan = new ArrayList<PlanElement>();
		List<PlanElement> currentTrip = new ArrayList<PlanElement>();

		double now = 0;
		double startOfTrip = Time.UNDEFINED_TIME;
		for (PlanElement currentElement : plan) {
			if (currentElement instanceof Activity) {
				Activity act = (Activity) currentElement;

				if (checker.isStageActivity( act.getType() )) {
					now = calcEndOfPlanElement( now , currentElement );
					currentTrip.add( act );
				}
				else {
					if (currentTrip.size() == 1) {
						simplifiedPlan.addAll( currentTrip );
					}
					else if (currentTrip.size() > 1) {
						Leg newLeg = new LegImpl( identifyMainMode( currentTrip ) );

						// set the time
						newLeg.setDepartureTime( startOfTrip);
						newLeg.setTravelTime( now - startOfTrip );

						simplifiedPlan.add( newLeg );
					}
					currentTrip.clear();
					now = calcEndOfPlanElement( now , currentElement );
					startOfTrip = now;
					simplifiedPlan.add( act );
				}
			}
			else if (currentElement instanceof Leg) {
				now = calcEndOfPlanElement( now , currentElement );
				currentTrip.add( currentElement );
			}
			else {
				throw new RuntimeException( "unknown PlanElement implementation "+currentElement.getClass() );
			}
		}

		return simplifiedPlan;
	}

	/**
	 * Routes a trip between the given O/D pair, with the given main mode.
	 *
	 * @param mainMode the main mode for the trip
	 * @param fromFacility a {@link Facility} representing the departure location
	 * @param toFacility a {@link Facility} representing the arrival location
	 * @param departureTime the departure time
	 * @param person the {@link Person} to route
	 * @return a list of {@link PlanElement}, in proper order, representing the trip.
	 *
	 * @throws UnknownModeException if no RoutingModule is registered for the
	 * given mode.
	 */
	public List<? extends PlanElement> calcRoute(
			final String mainMode,
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		RoutingModule module = routingModules.get( mainMode );

		if (module != null) {
			return module.calcRoute(
					fromFacility,
					toFacility,
					departureTime,
					person);
		}

		throw new UnknownModeException( "unregistered main mode "+mainMode+": does not pertain to "+routingModules.keySet() );
	}

	/**
	 * This is the method responsible for identifying the "main mode"
	 * of the trip, that is, the mode to which is attached the routing module to use.
	 * This default implementation considers a trip always starts by a leg, and
	 * the main mode is the mode of the first leg.
	 * <br>
	 * Override to change that.
	 */
	protected String identifyMainMode(final List<PlanElement> trip) {
		String mode = ((Leg) trip.get( 0 )).getMode();
		return mode.equals( TransportMode.transit_walk ) ? TransportMode.pt : mode;
	}

	public static class UnknownModeException extends RuntimeException {
		private UnknownModeException(
				final String msg) {
			super( msg );
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// public static convenience methods.
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Helper method, that can be used to compute start time of legs.
	 * (it is also used internally).
	 * It is provided here, because such an operation is mainly useful for routing,
	 * but it may be externalized in a "util" class...
	 */
	public static double calcEndOfPlanElement(
			final double now,
			final PlanElement pe) {
		if (now == Time.UNDEFINED_TIME) {
			throw new RuntimeException("got undefined now to update with plan element" + pe);
		}

		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			double endTime = act.getEndTime();
			double startTime = act.getStartTime();
			double dur = (act instanceof ActivityImpl ? ((ActivityImpl) act).getMaximumDuration() : Time.UNDEFINED_TIME);
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
			}
		}
		else {
			Route route = ((Leg) pe).getRoute();

			double travelTime = route != null ? route.getTravelTime() : Time.UNDEFINED_TIME;
			travelTime = travelTime == Time.UNDEFINED_TIME ? ((Leg) pe).getTravelTime() : travelTime;

			return now + (travelTime != Time.UNDEFINED_TIME ? travelTime : 0);
		}
	}	

	/**
	 * Inserts a trip between two activities in the sequence of plan elements
	 * returned by the {@link Plan#getPlanElements()} method of a plan. Note
	 * that the plan will be modified only if the returned list is the internal
	 * reference!
	 *
	 * @param plan the plan to modify
	 * @param origin the activity to use as origin. It must be a member of the list of plan elements.
	 * @param trip the trip to insert
	 * @param destination the destination activity. It must be a member of the list.
	 * @return the "old trip": the sequence of plan elements originally existing between the origin and the destination
	 */
	public static List<PlanElement> insertTrip(
			final Plan plan,
			final Activity origin,
			final List<? extends PlanElement> trip,
			final Activity destination) {
		return insertTrip(
				plan.getPlanElements(),
				origin,
				trip,
				destination);
	}

	/**
	 * Inserts a trip between two activities in a sequence of plan elements.
	 * @param plan the sequence of plan elements to modify
	 * @param origin the activity to use as origin. It must be a member of the list of plan elements.
	 * @param trip the trip to insert
	 * @param destination the destination activity. It must be a member of the list.
	 * @return the "old trip": the sequence of plan elements originally existing between the origin and the destination
	 */
	public static List<PlanElement> insertTrip(
			final List<PlanElement> plan,
			final Activity origin,
			final List<? extends PlanElement> trip,
			final Activity destination) {
		List<PlanElement> oldTrip = new ArrayList<PlanElement>();

		int index = plan.indexOf( origin );
		if (index == -1) {
			throw new IllegalArgumentException( origin+" does not belongs to "+plan );
		}

		int indexOfDestination = plan.indexOf( destination );
		if (indexOfDestination == -1) {
			throw new IllegalArgumentException( destination+" does not belongs to "+plan );
		}

		// go to the trip
		index++;

		// remove it
		int toRemove = indexOfDestination - index;
		while (toRemove > 0) {
			oldTrip.add( plan.remove( index ) );
			toRemove--;
		}

		// insert new trip
		for (PlanElement pe : trip) {
			plan.add( index , pe );
			index++;
		}

		return oldTrip;
	}
}

