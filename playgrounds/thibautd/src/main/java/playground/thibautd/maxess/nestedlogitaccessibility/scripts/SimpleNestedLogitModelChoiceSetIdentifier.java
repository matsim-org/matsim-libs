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
package playground.thibautd.maxess.nestedlogitaccessibility.scripts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import playground.thibautd.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.thibautd.maxess.nestedlogitaccessibility.framework.ChoiceSetIdentifier;
import playground.thibautd.maxess.nestedlogitaccessibility.framework.Nest;
import playground.thibautd.maxess.nestedlogitaccessibility.framework.NestedChoiceSet;
import playground.thibautd.maxess.prepareforbiogeme.tripbased.Trip;
import playground.thibautd.utils.ConcurrentStopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author thibautd
 */
public class SimpleNestedLogitModelChoiceSetIdentifier implements ChoiceSetIdentifier<ModeNests> {
	public enum Measurement { carTravelTime, ptTravelTime, bikeTravelTime, walkTravelTime, prismSampling; }
	private static final double MU_CAR = 1;
	private static final double MU_PT = 1;
	private static final double MU_BIKE = 6.4;
	private static final double MU_WALK = 1.74;

	private final Random random = MatsimRandom.getLocalInstance();
	private final int nSamples;
	private final TripRouter router;

	private final ActivityFacilities allFacilities;
	private final QuadTree<ActivityFacility> relevantFacilities;
	private final int budget_m;

	private final ConcurrentStopWatch<Measurement> stopWatch;

	public SimpleNestedLogitModelChoiceSetIdentifier(
			final ConcurrentStopWatch<Measurement> stopWatch,
			final String type,
			final int nSamples,
			final TripRouter router,
			final ActivityFacilities allFacilities,
			final int budget_m ) {
		this.stopWatch = stopWatch;
		this.nSamples = nSamples;
		this.router = router;
		this.allFacilities = allFacilities;
		this.budget_m = budget_m;

		final QuadTreeRebuilder<ActivityFacility> builder = new QuadTreeRebuilder<>();
		for ( ActivityFacility f : allFacilities.getFacilities().values() ) {
			if ( f.getActivityOptions().containsKey( type ) ) {
				builder.put( f.getCoord() , f );
			}
		}
		relevantFacilities = builder.getQuadTree();
	}

	@Override
	public NestedChoiceSet<ModeNests> identifyChoiceSet( final Person person ) {
		final Nest.Builder<ModeNests> carNestBuilder =
				new Nest.Builder<ModeNests>()
						.setMu( MU_CAR )
						.setName( ModeNests.car );
		final Nest.Builder<ModeNests> ptNestBuilder =
				new Nest.Builder<ModeNests>()
						.setMu( MU_PT )
						.setName( ModeNests.pt );
		final Nest.Builder<ModeNests> bikeNestBuilder =
				new Nest.Builder<ModeNests>()
						.setMu( MU_BIKE )
						.setName( ModeNests.bike );
		final Nest.Builder<ModeNests> walkNestBuilder =
				new Nest.Builder<ModeNests>()
						.setMu( MU_WALK )
						.setName( ModeNests.walk );

		// Sample and route alternatives
		stopWatch.startMeasurement( Measurement.prismSampling );
		final ActivityFacility origin = getOrigin( person );
		final List<ActivityFacility> prism = calcPrism( origin );
		stopWatch.endMeasurement( Measurement.prismSampling );

		for ( int i= 0; i < nSamples; i++ ) {
			final ActivityFacility f = prism.remove( random.nextInt( prism.size() ) );

			stopWatch.startMeasurement( Measurement.carTravelTime );
			carNestBuilder.addAlternative(
							calcAlternative(
									i,
									TransportMode.car,
									origin,
									f,
									person ) );
			stopWatch.endMeasurement( Measurement.carTravelTime );
			stopWatch.startMeasurement( Measurement.ptTravelTime );
			ptNestBuilder.addAlternative(
							calcAlternative(
									i,
									TransportMode.pt,
									origin,
									f,
									person ) );
			stopWatch.endMeasurement( Measurement.ptTravelTime );
			stopWatch.startMeasurement( Measurement.bikeTravelTime );
			bikeNestBuilder.addAlternative(
							calcAlternative(
									i,
									TransportMode.bike,
									origin,
									f,
									person ) );
			stopWatch.endMeasurement( Measurement.bikeTravelTime );
			stopWatch.startMeasurement( Measurement.walkTravelTime );
			walkNestBuilder.addAlternative(
							calcAlternative(
									i,
									TransportMode.walk,
									origin,
									f,
									person ) );
			stopWatch.endMeasurement( Measurement.walkTravelTime );
		}

		return new NestedChoiceSet<>(
				carNestBuilder.build(),
				ptNestBuilder.build(),
				bikeNestBuilder.build(),
				walkNestBuilder.build() );
	}

	private Alternative<ModeNests> calcAlternative(
			final int i,
			final String mode,
			final ActivityFacility origin,
			final ActivityFacility destination,
			final Person person ) {
		final Trip trip =
				new Trip(
						origin ,
						router.calcRoute(
								mode,
								origin,
								destination,
								12 * 3600,
								person ),
						destination );

		return new Alternative<>(
				ModeNests.valueOf( mode ),
				Id.create( i+"_"+mode , Alternative.class ),
				trip );
	}

	private ActivityFacility getOrigin( Person p ) {
		final Activity act = (Activity) p.getSelectedPlan().getPlanElements().get( 0 );
		final Id<ActivityFacility> facilityId = act.getFacilityId();
		return facilityId != null ?
				allFacilities.getFacilities().get( act.getFacilityId() ) :
				new ActivityFacility() {
					@Override
					public Map<String, ActivityOption> getActivityOptions() {
						throw new UnsupportedOperationException( "This is a dummy facility, only link and coord are available." );
					}

					@Override
					public void addActivityOption( ActivityOption option ) {
						throw new UnsupportedOperationException( "This is a dummy facility, only link and coord are available." );

					}

					@Override
					public Id<Link> getLinkId() {
						return act.getLinkId();
					}

					@Override
					public Coord getCoord() {
						return act.getCoord();
					}

					@Override
					public Map<String, Object> getCustomAttributes() {
						throw new UnsupportedOperationException( "This is a dummy facility, only link and coord are available." );
					}

					@Override
					public Id<ActivityFacility> getId() {
						throw new UnsupportedOperationException( "This is a dummy facility, only link and coord are available." );
					}
				};
	}

	private List<ActivityFacility> calcPrism( ActivityFacility p ) {
		// somehow silly to hard-code f1 and f2 to be the same,
		// but it should allow latter to extend it easier to a really "plan aware" measure,
		// using real prisms... or not.
		final Coord f1 = p.getCoord();
		final Coord f2 = p.getCoord();

		Collection<ActivityFacility> prism = Collections.emptyList();

		final double radius = Math.max( budget_m , 1.1 * CoordUtils.calcDistance( f1, f2 ) );
		for ( int i=1; prism.size() < nSamples; i++ ) {
			prism = relevantFacilities.getElliptical(f1.getX(), f1.getY(), f2.getX(), f2.getY(), i * radius);
		}

		return prism instanceof List ? (List<ActivityFacility>) prism : new ArrayList<>( prism );
	}
}
