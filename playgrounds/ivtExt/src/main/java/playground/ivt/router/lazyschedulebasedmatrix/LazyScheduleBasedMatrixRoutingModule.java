/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.ivt.router.lazyschedulebasedmatrix;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class LazyScheduleBasedMatrixRoutingModule implements RoutingModule {
	private final RoutingModule delegate;
	private final RoutingModule walkRouter;
	private final Cache cache;

	private double timeBinDuration_s;

	@Singleton
	public static class Cache {
		private final Network network;
		private final double timeBinDuration_s;
		private final double cellSize_m;

		// could also store soft references if need arises
		private final TIntObjectHashMap<
				TIntObjectHashMap<
						TIntObjectHashMap<
								TIntObjectHashMap<
										TIntObjectHashMap<
												List<? extends PlanElement>>>>>> matrix = new TIntObjectHashMap<>();
		private final TIntObjectHashMap<
				TIntObjectHashMap<
						Facility>> binnedFacilities = new TIntObjectHashMap<>();

		@Inject
		public Cache(
				final Scenario scenario ) {
			this(
					scenario.getNetwork(),
					ConfigUtils.addOrGetModule(
							scenario.getConfig(),
							LazyScheduleBasedMatrixConfigGroup.GROUP_NAME,
							LazyScheduleBasedMatrixConfigGroup.class ) );
		}

		private Cache( Network network, double timeBinDuration_s, double cellSize_m ) {
			this.network = network;
			this.timeBinDuration_s = timeBinDuration_s;
			this.cellSize_m = cellSize_m;
		}

		public Cache( Network network, LazyScheduleBasedMatrixConfigGroup configGroup ) {
			this( network , configGroup.getTimeBinDuration_s() , configGroup.getCellSize_m() );
		}

		private List<? extends PlanElement> uncache( final Coord origin, final Coord destination, final double time ) {
			final int xo = (int) ( origin.getX() / cellSize_m );
			final int yo = (int) ( origin.getY() / cellSize_m );
			final int xd = (int) ( destination.getX() / cellSize_m );
			final int yd = (int) ( destination.getY() / cellSize_m );
			final int timeSlot = (int) ( time / timeBinDuration_s );

			// just access: no need to synchronize
			final TIntObjectHashMap<TIntObjectHashMap<TIntObjectHashMap<TIntObjectHashMap<List<? extends PlanElement>>>>> atXo =
					matrix.get( xo );
			if ( atXo == null ) return null;

			final TIntObjectHashMap<TIntObjectHashMap<TIntObjectHashMap<List<? extends PlanElement>>>> atXoYo =
					atXo.get( yo );
			if ( atXoYo == null ) return null;

			final TIntObjectHashMap<TIntObjectHashMap<List<? extends PlanElement>>> atXoYoXd = atXoYo.get( xd );
			if ( atXoYoXd == null ) return null;

			final TIntObjectHashMap<List<? extends PlanElement>> atXoYoXdYd = atXoYoXd.get( yd );
			if ( atXoYoXdYd == null ) return null;

			return atXoYoXdYd.get( timeSlot );
		}

		private void cache(
				final Coord origin,
				final Coord destination,
				final double time,
				final List<? extends PlanElement> trip ) {
			final int xo = (int) ( origin.getX() / cellSize_m );
			final int yo = (int) ( origin.getY() / cellSize_m );
			final int xd = (int) ( destination.getX() / cellSize_m );
			final int yd = (int) ( destination.getY() / cellSize_m );
			final int timeSlot = (int) ( time / timeBinDuration_s );

			// Always synchronize on check and add
			// probably mainly conflicts on the full matrix, much less at lower levels
			TIntObjectHashMap<TIntObjectHashMap<TIntObjectHashMap<TIntObjectHashMap<List<? extends PlanElement>>>>> atXo;
			synchronized ( matrix ) {
				atXo = matrix.get( xo );
				if ( atXo == null ) {
					atXo = new TIntObjectHashMap<>();
					matrix.put( xo, atXo );
				}
			}

			TIntObjectHashMap<TIntObjectHashMap<TIntObjectHashMap<List<? extends PlanElement>>>> atXoYo;
			synchronized ( atXo ) {
				atXoYo = atXo.get( yo );
				if ( atXoYo == null ) {
					atXoYo = new TIntObjectHashMap<>();
					atXo.put( yo, atXoYo );
				}
			}

			TIntObjectHashMap<TIntObjectHashMap<List<? extends PlanElement>>> atXoYoXd;
			synchronized ( atXoYo ) {
				atXoYoXd = atXoYo.get( xd );
				if ( atXoYoXd == null ) {
					atXoYoXd = new TIntObjectHashMap<>();
					atXoYo.put( xd, atXoYoXd );
				}
			}

			TIntObjectHashMap<List<? extends PlanElement>> atXoYoXdYd;
			synchronized ( atXoYoXd ) {
				atXoYoXdYd = atXoYoXd.get( yd );
				if ( atXoYoXdYd == null ) {
					atXoYoXdYd = new TIntObjectHashMap<>();
					atXoYoXd.put( xd, atXoYoXdYd );
				}
			}

			synchronized ( atXoYoXdYd ) {
				atXoYoXdYd.put( timeSlot, trip );
			}
		}

		private Facility binFacility( final Facility fromFacility ) {
			final int cellX = (int) ( fromFacility.getCoord().getX() / cellSize_m );
			final int cellY = (int) ( fromFacility.getCoord().getY() / cellSize_m );

			TIntObjectHashMap<Facility> atX;
			synchronized ( binnedFacilities ) {
				atX = binnedFacilities.get( cellX );
				if ( atX == null ) {
					atX = new TIntObjectHashMap<>();
					binnedFacilities.put( cellX, atX );
				}
			}

			Facility f;
			synchronized ( atX ) {
				f = atX.get( cellY );
				if ( f == null ) {
					f = new Facility() {
						private Coord coord = new Coord( cellX * cellSize_m + cellSize_m / 2, cellY * cellSize_m + cellSize_m / 2 );
						private Id<Link> linkId = NetworkUtils.getNearestLinkExactly( network , coord ).getId();

						@Override
						public Id<Link> getLinkId() {
							return linkId;
						}

						@Override
						public Coord getCoord() {
							return coord;
						}

						@Override
						public Map<String, Object> getCustomAttributes() {
							throw new UnsupportedOperationException();
						}

						@Override
						public Id getId() {
							throw new UnsupportedOperationException();
						}
					};
					atX.put( cellY, f );
				}
			}

			return f;
		}
	}

	@Inject
	public LazyScheduleBasedMatrixRoutingModule(
			final Cache cache,
			final Scenario scenario,
			final TransitRouter router,
			@Named(TransportMode.transit_walk)
			final RoutingModule walkRouter ) {
		this( cache,
				ConfigUtils.addOrGetModule(
						scenario.getConfig(),
						LazyScheduleBasedMatrixConfigGroup.GROUP_NAME,
						LazyScheduleBasedMatrixConfigGroup.class ),
				scenario,
				router,
				walkRouter );
	}

	private LazyScheduleBasedMatrixRoutingModule(
			final Cache cache,
			final LazyScheduleBasedMatrixConfigGroup config,
			final Scenario scenario,
			final TransitRouter router,
			final RoutingModule walkRouter ) {
		this(
				config.getTimeBinDuration_s(),
				cache,
				router,
				scenario.getTransitSchedule(),
				scenario.getNetwork(),
				walkRouter );
	}

	public LazyScheduleBasedMatrixRoutingModule(
			final double timeBinDuration_s,
			final Cache cache,
			final TransitRouter router,
			final TransitSchedule transitSchedule,
			final Network network,
			final RoutingModule walkRouter ) {
		this.delegate = new TransitRouterWrapper(
				router,
				transitSchedule,
				network,
				walkRouter );
		this.walkRouter = walkRouter;
		this.timeBinDuration_s = timeBinDuration_s;
		this.cache = cache;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person ) {
		final Coord originCoord = fromFacility.getCoord();
		final Coord destinationCoord = toFacility.getCoord();

		final List<? extends PlanElement> cached = cache.uncache( originCoord, destinationCoord, departureTime );
		if ( cached != null ) return adapt( cached, fromFacility, toFacility );

		final List<? extends PlanElement> trip =
				calcBinnedRoute(
						fromFacility,
						toFacility,
						departureTime );
		cache.cache( originCoord, destinationCoord, departureTime, trim( trip ) );

		return trip;
	}

	private List<? extends PlanElement> calcBinnedRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime ) {
		return delegate.calcRoute(
				cache.binFacility( fromFacility ),
				cache.binFacility( toFacility ),
				// start at the end of the time bin
				Math.ceil( ( departureTime / timeBinDuration_s ) ) * timeBinDuration_s,
				null );
	}


	private List<? extends PlanElement> trim( List<? extends PlanElement> trip ) {
		if ( trip.size() == 1 ) return Collections.emptyList();
		return trip.subList( 1, trip.size() - 1 );
	}

	private List<? extends PlanElement> adapt(
			final List<? extends PlanElement> cached,
			final Facility fromFacility,
			final Facility toFacility ) {
		// the cached trip is already trimed
		if ( cached.isEmpty() ) {
			return calcWalkTrip( fromFacility, toFacility );
		}

		final List<PlanElement> trip = new ArrayList<>( cached.size() + 2 );
		trip.addAll(
				calcWalkTrip(
						fromFacility,
						new ActivityWrapperFacility( (Activity) cached.get( 0 ) ) ) );
		trip.addAll( cached );
		trip.addAll(
				calcWalkTrip(
						new ActivityWrapperFacility( (Activity) trip.get( cached.size() - 1 ) ),
						toFacility ) );

		return trip;
	}

	private List<? extends PlanElement> calcWalkTrip(
			final Facility fromFacility,
			final Facility toFacility ) {
		// deliberately pass null as a person, as we do not want a router that takes
		// the person into account anyway...
		return walkRouter.calcRoute( fromFacility, toFacility, 12 * 3600d, null );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return delegate.getStageActivityTypes();
	}
}
