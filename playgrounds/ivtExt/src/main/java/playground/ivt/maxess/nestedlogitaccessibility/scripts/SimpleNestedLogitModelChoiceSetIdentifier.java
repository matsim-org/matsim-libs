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
package playground.ivt.maxess.nestedlogitaccessibility.scripts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.ChoiceSetIdentifier;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Nest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedChoiceSet;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;
import playground.ivt.utils.ConcurrentStopWatch;

import java.util.*;

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
	private final ObjectAttributes personAttributes;
	private final QuadTree<ActivityFacility> relevantFacilities;
	private final int budget_m;

	private final ConcurrentStopWatch<Measurement> stopWatch;

	public SimpleNestedLogitModelChoiceSetIdentifier(
			final ConcurrentStopWatch<Measurement> stopWatch,
			final String type,
			final int nSamples,
			final TripRouter router,
			final ActivityFacilities allFacilities,
			final ObjectAttributes personAttributes,
			final int budget_m ) {
		this.stopWatch = stopWatch;
		this.nSamples = nSamples;
		this.router = router;
		this.allFacilities = allFacilities;
		this.personAttributes = personAttributes;
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
	public Map<String, NestedChoiceSet<ModeNests>> identifyChoiceSet( final Person person ) {
		final ChoiceSetBuilder baseBuilder = new ChoiceSetBuilder();
		final ChoiceSetBuilder nocarBuilder = new ChoiceSetBuilder();
		final ChoiceSetBuilder noptBuilder = new ChoiceSetBuilder();
		final ChoiceSetBuilder nobikeBuilder = new ChoiceSetBuilder();
		final ChoiceSetBuilder nowalkBuilder = new ChoiceSetBuilder();
		final ChoiceSetBuilder allBuilder = new ChoiceSetBuilder();

		// Sample and route alternatives
		stopWatch.startMeasurement( Measurement.prismSampling );
		final ActivityFacility origin = getOrigin( person );
		final List<ActivityFacility> prism = calcPrism( origin );
		stopWatch.endMeasurement( Measurement.prismSampling );

		for ( int i= 0; i < nSamples; i++ ) {
			final ActivityFacility f = prism.remove( random.nextInt( prism.size() ) );

			//-------------------------------------------------------------
			stopWatch.startMeasurement( Measurement.carTravelTime );
			add(
					calcAlternative(
							i,
							TransportMode.car,
							origin,
							f,
							person ),
					allBuilder.carNestBuilder,
					isCarAvailable( person ) ?
							baseBuilder.carNestBuilder :
							null,
					noptBuilder.carNestBuilder,
					nobikeBuilder.carNestBuilder,
					nowalkBuilder.carNestBuilder );
			stopWatch.endMeasurement( Measurement.carTravelTime );
			//-------------------------------------------------------------
			stopWatch.startMeasurement( Measurement.ptTravelTime );
			add(
					calcAlternative(
							i,
							TransportMode.pt,
							origin,
							f,
							person ),
					allBuilder.ptNestBuilder,
					baseBuilder.ptNestBuilder,
					nocarBuilder.ptNestBuilder,
					nobikeBuilder.ptNestBuilder,
					nowalkBuilder.ptNestBuilder );
			stopWatch.endMeasurement( Measurement.ptTravelTime );
			//-------------------------------------------------------------
			stopWatch.startMeasurement( Measurement.bikeTravelTime );
			add(
					calcAlternative(
							i,
							TransportMode.bike,
							origin,
							f,
							person ),
					allBuilder.bikeNestBuilder,
					isBikeAvailable( person ) ?
							baseBuilder.bikeNestBuilder :
							null,
					nocarBuilder.bikeNestBuilder,
					noptBuilder.bikeNestBuilder,
					nowalkBuilder.bikeNestBuilder );
			stopWatch.endMeasurement( Measurement.bikeTravelTime );
			//-------------------------------------------------------------
			stopWatch.startMeasurement( Measurement.walkTravelTime );
			add(
					calcAlternative(
							i,
							TransportMode.walk,
							origin,
							f,
							person ),
					allBuilder.walkNestBuilder,
					baseBuilder.walkNestBuilder,
					nocarBuilder.walkNestBuilder,
					nobikeBuilder.walkNestBuilder,
					noptBuilder.walkNestBuilder );
			stopWatch.endMeasurement( Measurement.walkTravelTime );
			//-------------------------------------------------------------
		}

		final Map<String, NestedChoiceSet<ModeNests>> result = new LinkedHashMap<>();
		result.put( "all" , allBuilder.build() );
		result.put( "base" , baseBuilder.build() );
		result.put( "nocar" , nocarBuilder.build() );
		result.put( "nobike" , nobikeBuilder.build() );
		result.put( "nopt" , noptBuilder.build() );
		result.put( "nowalk" , nowalkBuilder.build() );

		return result;
	}

	private boolean isBikeAvailable( Person person ) {
		final String avail = (String)
				personAttributes.getAttribute(
					person.getId().toString(),
					"availability: bicycle" );
		return avail.equals( "always" );
	}

	private boolean isCarAvailable( Person person ) {
		final String avail = (String)
				personAttributes.getAttribute(
					person.getId().toString(),
					"availability: car" );
		return avail.equals( "always" );
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

		final double radius = Math.max( budget_m, 1.1 * CoordUtils.calcEuclideanDistance( f1, f2 ) );
		for ( int i=1; prism.size() < nSamples; i++ ) {
			prism = relevantFacilities.getElliptical(f1.getX(), f1.getY(), f2.getX(), f2.getY(), i * radius);
		}

		return prism instanceof List ? (List<ActivityFacility>) prism : new ArrayList<>( prism );
	}

	private void add( Alternative<ModeNests> alternative , Nest.Builder<ModeNests>... sets ) {
		for ( Nest.Builder<ModeNests> set : sets ) {
			if ( set == null ) continue;
			set.addAlternative( alternative );
		}
	}

	private static class ChoiceSetBuilder {
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

		public NestedChoiceSet<ModeNests> build() {
			return new NestedChoiceSet<>(
					carNestBuilder.build(),
					ptNestBuilder.build(),
					bikeNestBuilder.build(),
					walkNestBuilder.build() );
		}
	}
}
