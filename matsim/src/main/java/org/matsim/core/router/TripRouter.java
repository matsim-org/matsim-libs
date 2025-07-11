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
package org.matsim.core.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.internal.MatsimExtensionPoint;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * Class acting as an intermediate between clients needing to
 * compute routes and all registered {@link RoutingModule}s.
 * It provides convenience methods to route an individual trip with
 * a desired mode or to identify trips.
 * <p></p>
 *
 * See {@link tutorial.programming.ownMobsimAgentUsingRouter.RunOwnMobsimAgentUsingRouterExample} for an example
 * how to use this API from your own code.
 * See {@link tutorial.programming.example12PluggableTripRouter.RunPluggableTripRouterExample} and {@link tutorial.programming.example13MultiStageTripRouting.RunTeleportationMobsimWithCustomRoutingExample} for examples
 * how to extend or replace this behavior with your own.
 *
 * @author thibautd
 */
public final class TripRouter implements MatsimExtensionPoint {

	private final Map<String, RoutingModule> routingModules = new HashMap<>();
	private final FallbackRoutingModule fallbackRoutingModule;

	private final Config config;
	// (I need the config in the PlanRouter to figure out activity end times. And since the PlanRouter is not
	// injected, I cannot get it there directly.  kai, oct'17)

	public static final class Builder {
		private final Config config;
		private FallbackRoutingModule fallbackRoutingModule = new FallbackRoutingModuleDefaultImpl() ;
		private Map<String, Provider<RoutingModule>> routingModuleProviders = new LinkedHashMap<>() ;
		public Builder( Config config ) {
			this.config = config ;
		}
		public Builder setRoutingModule(String mainMode, RoutingModule routingModule ) {
			// the initial API accepted routing modules.  injection, however, takes routing module providers.  (why?)
			// trying to bring these two into line here.  maybe some other approach would be preferred, don't know.  kai, jun'18
			this.routingModuleProviders.put( mainMode, new Provider<>() {
				@Override
				public RoutingModule get() {
					return routingModule;
				}
			} ) ;
			return this ;
		}
		public TripRouter build() {
			return new TripRouter( routingModuleProviders, config, fallbackRoutingModule ) ;
		}
	}

	@Inject
	TripRouter( Map<String, Provider<RoutingModule>> routingModuleProviders, Config config,
			FallbackRoutingModule fallbackRoutingModule ) {
		this.fallbackRoutingModule = fallbackRoutingModule;

		for (Map.Entry<String, Provider<RoutingModule>> entry : routingModuleProviders.entrySet()) {
			setRoutingModule(entry.getKey(), entry.getValue().get());
		}
		this.config = config ;
	}

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
	private RoutingModule setRoutingModule(
			final String mainMode,
			final RoutingModule module) {
		return routingModules.put( mainMode , module );
	}

	public RoutingModule getRoutingModule(final String mainMode) {
		return routingModules.get( mainMode );
	}

	public Set<String> getRegisteredModes() {
		return Collections.unmodifiableSet( routingModules.keySet() );
	}

	// /////////////////////////////////////////////////////////////////////////
	// Handling methods
	// /////////////////////////////////////////////////////////////////////////
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
	public synchronized List<? extends PlanElement> calcRoute(
			final String mainMode,
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person,
			final Attributes routingAttributes) {
		// I need this "synchronized" since I want mobsim agents to be able to call this during the mobsim.  So when the
		// mobsim is multi-threaded, multiple agents might call this here at the same time.  kai, nov'17

		Gbl.assertNotNull( fromFacility );
		Gbl.assertNotNull( toFacility );

		RoutingModule module = routingModules.get( mainMode );

		if (module == null) {
			throw new UnknownModeException( "unregistered main mode |"+mainMode+"|: does not pertain to "+routingModules.keySet() );
		}
		RoutingRequest request = DefaultRoutingRequest.of(
				fromFacility,
				toFacility,
				departureTime,
				person,
				routingAttributes);

		List<? extends PlanElement> trip = module.calcRoute(request);

		if ( trip == null ) {
			trip = fallbackRoutingModule.calcRoute(request) ;
		}
		for (Leg leg: TripStructureUtils.getLegs(trip)) {
			TripStructureUtils.setRoutingMode(leg, mainMode);
		}
		return trip;
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
	 * Inserts a trip between two activities in the sequence of plan elements
	 * returned by the {@link Plan#getPlanElements()} method of a plan. Note
	 * that the plan will be modified only if the returned list is the internal
	 * reference!
	 * <p></p>
	 * Note that this methods returns a unique solution because it expects the activity object references as arguments, which are unique.
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
	 * <p></p>
	 * Note that this methods returns a unique solution because it expects the activity object references as arguments, which are unique.
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
		int indexOfOrigin = -1;
		int indexOfDestination = -1;

		// search the trip
		int currentIndex = 0;
		for (PlanElement pe : plan) {
			if (pe == origin) {
				indexOfOrigin = currentIndex;
			}
			if (pe == destination) {
				indexOfDestination = currentIndex;
				if (indexOfDestination < indexOfOrigin ) {
					throw new RuntimeException(
							"destination "+destination+" found before origin "+
							origin+" in "+plan );
				}
				break;
			}
			currentIndex++;
		}

		// check validity
		if (indexOfOrigin < 0) {
			throw new RuntimeException( "could not find origin "+origin+" in "+plan );
		}
		if (indexOfDestination < 0) {
			throw new RuntimeException( "could not find destination "+destination+" in "+plan );
		}

		// replace the trip and return the former one
		List<PlanElement> seq = plan.subList( indexOfOrigin + 1 , indexOfDestination );
		List<PlanElement> oldTrip = new ArrayList<>( seq );
		seq.clear();
		assert trip != null;
		seq.addAll( trip );

		return oldTrip;
	}

	public Config getConfig() {
		return config;
	}

	@Deprecated // #deleteBeforeRelease : only used to retrofit plans created since the merge of fallback routing module (sep'-dec'19)
	public static String getFallbackMode(String transportMode) {
		return transportMode + FallbackRoutingModuleDefaultImpl._fallback;
	}

	@Deprecated // #deleteBeforeRelease : only used to retrofit plans created since the merge of fallback routing module (sep'-dec'19)
	public static boolean isFallbackMode(String mode) {
		return mode.endsWith(FallbackRoutingModuleDefaultImpl._fallback);
	}
}

