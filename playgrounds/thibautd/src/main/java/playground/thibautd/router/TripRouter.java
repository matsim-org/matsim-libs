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

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.LegImpl;

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
		List<PlanElement> simplifiedPlan = new ArrayList<PlanElement>();
		List<PlanElement> currentTrip = new ArrayList<PlanElement>();

		for (PlanElement currentElement : plan.getPlanElements()) {
			if (currentElement instanceof Activity) {
				Activity act = (Activity) currentElement;

				if (checker.isStageActivity( act.getType() )) {
					currentTrip.add( act );
				}
				else {
					if (currentTrip.size() > 0) {
						Leg newLeg = new LegImpl( identifyMainMode( currentTrip ) );
						// set the time attributes?
						simplifiedPlan.add( newLeg );
						currentTrip.clear();
					}
					simplifiedPlan.add( act );
				}
			}
			else if (currentElement instanceof Leg) {
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

	// TODO: include a "main mode" attribute in leg, and use it
	private static String identifyMainMode(final List<PlanElement> trip) {
		String mode = ((Leg) trip.get( 0 )).getMode();

		return mode.equals( TransportMode.transit_walk ) ? TransportMode.pt : mode;
	}

	public static class UnknownModeException extends RuntimeException {
		private UnknownModeException(
				final String msg) {
			super( msg );
		}
	}
}

