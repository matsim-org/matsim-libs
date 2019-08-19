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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.internal.MatsimExtensionPoint;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;

import javax.inject.Inject;
import javax.inject.Provider;

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
	private static final Logger log = Logger.getLogger(TripRouter.class );
	
	private final Map<String, RoutingModule> routingModules = new HashMap<>();
	
	private final CompositeStageActivityTypes checker = new CompositeStageActivityTypes();

	private MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();

	private Config config;
	// (I need the config in the PlanRouter to figure out activity end times. And since the PlanRouter is not
	// injected, I cannot get it there directly.  kai, oct'17)
	
	public static final class Builder {
		private final Config config;
		private MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();
		private Map<String, Provider<RoutingModule>> routingModuleProviders = new LinkedHashMap<>() ;
		public Builder( Config config ) {
			this.config = config ;
		}
		public Builder setMainModeIdentifier( MainModeIdentifier identifier ) {
			this.mainModeIdentifier = identifier ;
			return this ;
		}
		public Builder setRoutingModule(String mainMode, RoutingModule routingModule ) {
			// the initial API accepted routing modules.  injection, however, takes routing module providers.  (why?)
			// trying to bring these two into line here.  maybe some other approach would be preferred, don't know.  kai, jun'18
			this.routingModuleProviders.put( mainMode, new Provider<RoutingModule>(){
				@Override public RoutingModule get() {
					return routingModule ;
				}
			} ) ;
			return this ;
		}
		public TripRouter build() {
			return new TripRouter( routingModuleProviders, mainModeIdentifier, config ) ;
		}
	}

//	@Deprecated // use the Builder instead.  kai, oct'17
//	public TripRouter() {}
//	// yyyyyy I guess this is meant as a way to create the trip router without injection, and to set its internals afterwards.  But
//	// is it so sensible to have this in this way?  The injection stuff states that the material is immutable after injection; here we introduce a
//	// way to get around that again, and even to change the injected material later.
//	// I would expect a Builder instead.
//	// kai, sep'16

	@Inject
	TripRouter( Map<String, Provider<RoutingModule>> routingModuleProviders, MainModeIdentifier mainModeIdentifier, Config config ) {

		for (Map.Entry<String, Provider<RoutingModule>> entry : routingModuleProviders.entrySet()) {
			setRoutingModule(entry.getKey(), entry.getValue().get());
		}
		setMainModeIdentifier(mainModeIdentifier);
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
	@Deprecated // use the Builder instead.  kai, oct'17
	/* package-private */ RoutingModule setRoutingModule(
			final String mainMode,
			final RoutingModule module) {
		RoutingModule old = routingModules.put( mainMode , module );

		if (old != null) {
			final StageActivityTypes oldTypes = old.getStageActivityTypes();
			final boolean removed = checker.removeActivityTypes( oldTypes );
			if ( !removed ) {
				throw new RuntimeException( "could not remove "+oldTypes+" associated to "+old+". This may be due to a routing module creating a new instance at each call of getStageActivityTypes()" );
			}
		}

		final StageActivityTypes types = module.getStageActivityTypes();
		if ( types == null ) {
			// we do not want to accept that, this would risk to mess up
			// with replacement, and it generally makes code messy.
			throw new RuntimeException( module+" returns null stage activity types. This is not a valid value. Return EmptyStageActivityTypes.INSTANCE instead." );
		}
		checker.addActivityTypes( types );

		return old;
	}

	public RoutingModule getRoutingModule(final String mainMode) {
		return routingModules.get( mainMode );
	}

	public Set<String> getRegisteredModes() {
		return Collections.unmodifiableSet( routingModules.keySet() );
	}

	/**
	 * Gives access to the stage activity types, for all modes.
	 * @return a {@link StageActivityTypes} considering all registered modules
	 */
	public StageActivityTypes getStageActivityTypes() {
		return checker;
	}

	/**
	 * Sets the {@link MainModeIdentifier} instance returned by this trip router.
	 * Note that it is not used internally: it is just provided here because it is useful
	 * mainly for users of this class.
	 *
	 * @param newIdentifier the instance to register
	 * @return the previous registered instance
	 */
	@Deprecated // use the Builder instead.  kai, oct'17
	/* package-private */ MainModeIdentifier setMainModeIdentifier(final MainModeIdentifier newIdentifier) {
		final MainModeIdentifier old = this.mainModeIdentifier;
		this.mainModeIdentifier = newIdentifier;
		return old;
	}

	public MainModeIdentifier getMainModeIdentifier() {
		return mainModeIdentifier;
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
			final Person person) {
		// I need this "synchronized" since I want mobsim agents to be able to call this during the mobsim.  So when the
		// mobsim is multi-threaded, multiple agents might call this here at the same time.  kai, nov'17
		
		Gbl.assertNotNull( fromFacility );
		Gbl.assertNotNull( toFacility );
		
		RoutingModule module = routingModules.get( mainMode );
		
		if (module != null) {
			List<? extends PlanElement> trip =
					module.calcRoute(
						fromFacility,
						toFacility,
						departureTime,
						person);

			if ( trip == null ) {
				//				throw new NullPointerException( "Routing module "+module+" returned a null Trip for main mode "+mainMode );
				module = routingModules.get( "fallback") ;
				if ( module==null ) {
					throw new NullPointerException( "Routing module "+module+" returned a null Trip for main mode "+mainMode );
				}
				trip = module.calcRoute( fromFacility, toFacility, departureTime, person ) ;
				for( Leg leg : TripStructureUtils.getLegs( trip ) ){
					leg.setMode( mainMode + "_fallback" );
				}
			}
			return trip;
		}

		throw new UnknownModeException( "unregistered main mode |"+mainMode+"|: does not pertain to "+routingModules.keySet() );
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
	 * @param config TODO
	 */
	public static double calcEndOfPlanElement(
			final double now,
			final PlanElement pe, Config config) {

		if (Time.isUndefinedTime(now)) {
			throw new RuntimeException("got undefined now to update with plan element" + pe);
		}

		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			return PopulationUtils.decideOnActivityEndTime(act, now, config ) ;
		}
		else {
//			// take travel time from route if possible
//			Route route = ((Leg) pe).getRoute();
//			double travelTime = route != null ? route.getTravelTime() : Time.getUndefinedTime();
//
//			// travel time from leg will override this
//			travelTime = Time.isUndefinedTime(travelTime) ? ((Leg) pe).getTravelTime() : travelTime;
//
//			// if still undefined, assume zero:
//			return now + (Time.isUndefinedTime(travelTime) ? 0 : travelTime);

			// replace above by already existing centralized method.  Which, however, does less hedging, and prioritizes route ttime over leg ttime.  Let's run the tests ...

			double ttime = PopulationUtils.decideOnTravelTimeForLeg( (Leg) pe );
			if ( Time.isUndefinedTime( ttime ) ) {
				ttime = 0. ;
			}
			return now + ttime;
		}
	}

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

}

