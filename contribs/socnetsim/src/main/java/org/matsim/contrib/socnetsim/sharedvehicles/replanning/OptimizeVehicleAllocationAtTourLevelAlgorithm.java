/* *********************************************************************** *
 * project: org.matsim.*
 * OptimizeVehicleAllocationAtTourLevel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.sharedvehicles.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;

/**
 * Optimizes vehicle allocation at the tour level, by minimizing the estimated
 * waiting times.
 * It assumes all persons in the group start and end their plans at the same location ("home"),
 * and get the vehicles at the "home" location (ie household-like).
 * If those assumptions are not verified, the algorithm falls back to random allocation.
 *
 * <br>
 * It also assumes departure times are determined by activity end times and routes
 * (or legs) contain good estimation of the travel time.
 * @author thibautd
 */
public class OptimizeVehicleAllocationAtTourLevelAlgorithm implements GenericPlanAlgorithm<GroupPlans> {
	private final GenericPlanAlgorithm<GroupPlans> randomAllocator;
	private final StageActivityTypes stageActs;
	private final Collection<String> vehicularModes;
	private final boolean allowNullRoutes;
	private final VehicleRessources vehicleRessources;

	public OptimizeVehicleAllocationAtTourLevelAlgorithm(
			final StageActivityTypes stageActivitiesForSubtourDetection,
			final Random random,
			final VehicleRessources vehicleRessources,
			final Collection<String> modes,
			final boolean allowNullRoutes) {
		this.randomAllocator = new AllocateVehicleToPlansInGroupPlanAlgorithm(
			random,
			vehicleRessources,
			modes,
			allowNullRoutes,
			false);
		this.vehicularModes = modes;
		this.allowNullRoutes = allowNullRoutes;
		this.stageActs = stageActivitiesForSubtourDetection;
		this.vehicleRessources = vehicleRessources;
	}

	@Override
	public void run(final GroupPlans plan) {
		final List<SubtourRecord> vehicularTours = getVehicularToursSortedByStartTime( plan );
		if ( vehicularTours == null ) {
			randomAllocator.run( plan );
			return;
		}

		allocateVehicles( vehicularTours );

		processAllocation( vehicularTours );
	}

	private void processAllocation(final List<SubtourRecord> vehicularTours) {
		for ( final SubtourRecord r : vehicularTours ) {
			for ( final Trip t : r.subtour.getTrips() ) {
				for ( final Leg leg : t.getLegsOnly() ) {
					if ( !vehicularModes.contains( leg.getMode() ) ) continue;

					if ( allowNullRoutes && leg.getRoute() == null ) {
						// this is not so nice...
						leg.setRoute( new VehicleOnlyNetworkRoute() );
					}

					if ( !( leg.getRoute() instanceof NetworkRoute ) ) {
						throw new RuntimeException( "route for mode "+leg.getMode()+" has non-network route "+leg.getRoute() );
					}

					((NetworkRoute) leg.getRoute()).setVehicleId( r.allocatedVehicle );
				}
			}
		}
	}

	private void allocateVehicles(
			final List<SubtourRecord> toursToAllocate) {
		// greedy algo. Should be ok, but didn't formally prove it
		if ( toursToAllocate.isEmpty() ) return;
		final List<SubtourRecord> remainingSubtours = new ArrayList<SubtourRecord>( toursToAllocate );
		final SubtourRecord currentSubtour = remainingSubtours.remove( 0 );

		final VehicleRecord firstAvailableVehicle =
			Collections.min(
				currentSubtour.possibleVehicles,
				new Comparator<VehicleRecord>() {
					@Override
					public int compare(
							final VehicleRecord o1,
							final VehicleRecord o2) {
						final int timeComp = Double.compare(
							o1.availableFrom,
							o2.availableFrom );
						return timeComp != 0 ? timeComp :
							o1.nAllocs - o2.nAllocs;
					}
				});

		if ( firstAvailableVehicle.availableFrom < currentSubtour.endTime ) {
			firstAvailableVehicle.availableFrom = currentSubtour.endTime;
		}
		firstAvailableVehicle.nAllocs++;
		currentSubtour.allocatedVehicle = firstAvailableVehicle.id;

		allocateVehicles( remainingSubtours );
	}

	/*for tests*/ double calcOverlap(final GroupPlans gps) {
		final List<SubtourRecord> tours = getVehicularToursSortedByStartTime( gps );

		double overlap = 0;
		for ( final SubtourRecord tour : tours ) {
			Id veh = null;
			for ( final Trip t : tour.subtour.getTrips() ) {
				veh = getVehicle( t );
				if ( veh != null ) break;
			}
			
			for ( final VehicleRecord vr : tour.possibleVehicles ) {
				if ( vr.id.equals( veh ) ) {
					overlap += Math.max( 0 , vr.availableFrom - tour.startTime );
					if ( vr.availableFrom < tour.endTime ) {
						vr.availableFrom = tour.endTime;
					}
					break;
				}
			}
		}

		return overlap;
	}

	// /////////////////////////////////////////////////////////////////////////
	// subtour-related methods
	// /////////////////////////////////////////////////////////////////////////
	private List<SubtourRecord> getVehicularToursSortedByStartTime(final GroupPlans plans) {
		final List<SubtourRecord> vehicularTours = new ArrayList<SubtourRecord>();
		final VehicleRecordFactory factory = new VehicleRecordFactory();

		for ( final Plan p : plans.getAllIndividualPlans() ) {
			final Collection<Subtour> subtours =
				TripStructureUtils.getSubtours(
						p,
						stageActs
                );
			for ( final Subtour s : subtours ) {
				if ( s.getParent() != null ) continue; // is not a root tour
				boolean isFirstTrip = true;
				for ( final Trip t : s.getTrips() ) {
					// TODO: check that the sequence of vehicular movements come back to origin
					if ( isFirstTrip && isVehicular( t ) ) {
						vehicularTours.add(
								new SubtourRecord(
									s,
									factory.getRecords(
										vehicleRessources.identifyVehiclesUsableForAgent(
											p.getPerson().getId() ))) );
						break;
					}
					if ( !isFirstTrip && isVehicular( t ) ) {
						// invalid structure
						return null;
					}
					isFirstTrip = false;
				}
			}
		}

		// check validity
		Id homeLoc = null;
		for ( final SubtourRecord record : vehicularTours ) {
			final Subtour s = record.subtour;
			assert s.getParent() == null;
			final Id anchor = s.getTrips().get( 0 ).getOriginActivity().getFacilityId()!=null ?
				s.getTrips().get( 0 ).getOriginActivity().getFacilityId() :
				s.getTrips().get( 0 ).getOriginActivity().getLinkId();

			if ( anchor == null ) throw new NullPointerException( "null anchor location" );
			if ( homeLoc == null ) {
				homeLoc = anchor;
			}
			else if ( !homeLoc.equals( anchor ) ) {
				// invalid
				return null;
			}
		}

		Collections.sort(
				vehicularTours,
				new Comparator<SubtourRecord>() {
					@Override
					public int compare(
							final SubtourRecord o1,
							final SubtourRecord o2) {
						return Double.compare( o1.startTime , o2.startTime );
					}
				});


		return vehicularTours;
	}

	private boolean isVehicular(final Trip t) {
		// note: checking that getVehicle returns null doen't work
		// when allowing for null routes. Hence the duplication of the logic...
		final List<Leg> legs = t.getLegsOnly();
		if ( legs.isEmpty() ) return false;

		// XXX what to do if several legs???
		final Leg l = legs.get( 0 );
		if ( !vehicularModes.contains( l.getMode() ) ) return false;
		if ( !allowNullRoutes && l.getRoute() == null ) return false;
		if ( l.getRoute() != null && !(l.getRoute() instanceof NetworkRoute) ) return false; 
		return true;
	}

	private Id getVehicle( final Trip t ) {
		final List<Leg> legs = t.getLegsOnly();
		if ( legs.isEmpty() ) return null;

		// XXX what to do if several legs???
		final Leg l = legs.get( 0 );
		if ( !vehicularModes.contains( l.getMode() ) ) return null;
		if ( !allowNullRoutes && l.getRoute() == null ) return null;
		if ( l.getRoute() != null && !(l.getRoute() instanceof NetworkRoute) ) return null; 
		return ((NetworkRoute) l.getRoute()).getVehicleId();
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private static class SubtourRecord {
		public final double startTime, endTime;
		public final List<VehicleRecord> possibleVehicles;
		public final Subtour subtour;
		public Id allocatedVehicle = null;

		public SubtourRecord(
				final Subtour subtour,
				final List<VehicleRecord> possibleVehicles) {
			this.possibleVehicles = possibleVehicles; 
			this.subtour = subtour;
			
			final Trip firstTrip = subtour.getTrips().get( 0 );
			this.startTime = firstTrip.getOriginActivity().getEndTime();
			if ( startTime == Time.UNDEFINED_TIME ) throw new RuntimeException( "no end time in "+firstTrip.getOriginActivity() );

			final Trip lastTrip = subtour.getTrips().get( subtour.getTrips().size() - 1 );
			this.endTime = calcArrivalTime( lastTrip );
		}
	}

	private static class VehicleRecord {
		public final Id id;
		public int nAllocs = 0;
		public double availableFrom = Double.NEGATIVE_INFINITY;

		public VehicleRecord(final Id id) {
			this.id = id;
		}
	}

	private static class VehicleRecordFactory {
		private final Map<Id<Vehicle>, VehicleRecord> records = new HashMap<>();

		public List<VehicleRecord> getRecords(final Collection<Id<Vehicle>> ids) {
			final List<VehicleRecord> list = new ArrayList<VehicleRecord>();

			for ( final Id<Vehicle> id : ids ) {
				VehicleRecord r = records.get( id );

				if ( r == null ) {
					r = new VehicleRecord( id );
					records.put( id , r );
				}

				list.add( r );
			}

			return list;
		}
	}

	private static double calcArrivalTime(final Trip trip) {
		double now = trip.getOriginActivity().getEndTime();
		for ( final PlanElement pe : trip.getTripElements() ) {
			if ( pe instanceof Activity ) {
				final double end = ((Activity) pe).getEndTime();
				now = end != Time.UNDEFINED_TIME ? end : now + ((Activity) pe).getMaximumDuration();
				// TODO: do not fail *that* badly, but just revert to random alloc
				if ( now == Time.UNDEFINED_TIME ) throw new RuntimeException( "could not get time from "+pe );
			}
			else if ( pe instanceof Leg ) {
				final Route r = ((Leg) pe).getRoute();
				if ( r != null && r.getTravelTime() != Time.UNDEFINED_TIME ) {
					now += r.getTravelTime();
				}
				else {
					now += ((Leg) pe).getTravelTime() != Time.UNDEFINED_TIME ?
							((Leg) pe).getTravelTime() :
							0; // no info: just assume instantaneous. This will give poor results!
				}
			}
		}
		return now;
	}
}

