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
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.router.population.LegWithMainMode;

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
	 * Sets the {@link RoutingModule} to use for the given (main) mode
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
	 * Gives access to the stage activity types, for all modes
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
		List<PlanElement> simplifiedPlan = new ArrayList<PlanElement>();
		List<PlanElement> currentTrip = new ArrayList<PlanElement>();

		double now = 0;
		double startOfTrip = Time.UNDEFINED_TIME;
		for (PlanElement currentElement : plan) {
			if (currentElement instanceof Activity) {
				now = updateNow( now , currentElement );
				Activity act = (Activity) currentElement;

				if (checker.isStageActivity( act.getType() )) {
					currentTrip.add( act );
				}
				else {
					if (currentTrip.size() > 0) {
						Leg newLeg = new LegImpl( identifyMainMode( currentTrip ) );

						// set the time
						newLeg.setDepartureTime( startOfTrip);
						newLeg.setTravelTime( now - startOfTrip );

						simplifiedPlan.add( newLeg );
						currentTrip.clear();
					}
					else {
						startOfTrip = now;
					}
					simplifiedPlan.add( act );
				}
			}
			else if (currentElement instanceof Leg) {
				now = updateNow( now , currentElement );
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

	private static String identifyMainMode(final List<PlanElement> trip) {
		// check better (ie, main mode should be the same for all legs in trip)?
		Leg firstLeg = (Leg) trip.get( 0 );

		return firstLeg instanceof LegWithMainMode ?
			((LegWithMainMode) firstLeg).getMainMode() :
			firstLeg.getMode();
	}

	public static class UnknownModeException extends RuntimeException {
		private UnknownModeException(
				final String msg) {
			super( msg );
		}
	}

	private static double updateNow(
			final double now,
			final PlanElement pe) {
		if (now == Time.UNDEFINED_TIME) {
			throw new RuntimeException("got wrong now to update with plan element" + pe);
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

			double travelTime = route != null ? route.getTravelTime() : ((Leg) pe).getTravelTime();

			return now + (travelTime != Time.UNDEFINED_TIME ? travelTime : 0);
		}
	}	


}

