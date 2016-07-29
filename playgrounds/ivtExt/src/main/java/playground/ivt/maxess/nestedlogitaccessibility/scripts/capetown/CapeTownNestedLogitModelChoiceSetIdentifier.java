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
package playground.ivt.maxess.nestedlogitaccessibility.scripts.capetown;

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
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.ChoiceSetIdentifier;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Nest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedChoiceSet;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;
import playground.ivt.maxess.prepareforbiogeme.tripbased.capetown.PersonEnums;
import playground.ivt.utils.ConcurrentStopWatch;

import java.util.*;

/**
 * @author thibautd
 */
public class CapeTownNestedLogitModelChoiceSetIdentifier implements ChoiceSetIdentifier<CapeTownModeNests> {
	private final Map<Id<Person>,Id<Household>> person2household = new HashMap<>();
	private final Households households;

	public enum Measurement { carTravelTime, ptTravelTime, walkTravelTime, prismSampling; }

	private final Random random = MatsimRandom.getLocalInstance();
	private final int nSamples;
	private final TripRouter router;

	private final ActivityFacilities allFacilities;
	private final ObjectAttributes personAttributes;
	private final QuadTree<ActivityFacility> relevantFacilities;
	private final int budget_m;

	private final CapeTownNestedLogitModelConfigGroup configGroup;

	private final ConcurrentStopWatch<Measurement> stopWatch;

	public CapeTownNestedLogitModelChoiceSetIdentifier(
			final CapeTownNestedLogitModelConfigGroup configGroup,
			final ConcurrentStopWatch<Measurement> stopWatch,
			final String type,
			final int nSamples,
			final TripRouter router,
			final ActivityFacilities allFacilities,
			final ObjectAttributes personAttributes,
			final Households households,
			final int budget_m ) {
		this.configGroup = configGroup;
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

		this.households = households;
		for ( Household hh : households.getHouseholds().values() ) {
			for ( Id<Person> personId : hh.getMemberIds() ) {
				person2household.put( personId , hh.getId() );
			}
		}
	}

	@Override
	public Map<String, NestedChoiceSet<CapeTownModeNests>> identifyChoiceSet( final Person person ) {
		final ChoiceSetBuilder baseBuilder = new ChoiceSetBuilder( configGroup );
		final ChoiceSetBuilder nocarBuilder = new ChoiceSetBuilder( configGroup );
		final ChoiceSetBuilder noptBuilder = new ChoiceSetBuilder( configGroup );
		final ChoiceSetBuilder nowalkBuilder = new ChoiceSetBuilder( configGroup );
		final ChoiceSetBuilder allBuilder = new ChoiceSetBuilder( configGroup );

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
					nowalkBuilder.ptNestBuilder );
			stopWatch.endMeasurement( Measurement.ptTravelTime );
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
					noptBuilder.walkNestBuilder );
			stopWatch.endMeasurement( Measurement.walkTravelTime );
			//-------------------------------------------------------------
			add(
					calcAlternative(
							i,
							TransportMode.ride,
							origin,
							f,
							person ),
					allBuilder.rideNestBuilder,
					baseBuilder.rideNestBuilder,
					nocarBuilder.rideNestBuilder,
					noptBuilder.rideNestBuilder );
			add(
					calcAlternative(
							i,
							"taxi",
							origin,
							f,
							person ),
					allBuilder.taxiNestBuilder,
					baseBuilder.taxiNestBuilder,
					nocarBuilder.taxiNestBuilder,
					noptBuilder.taxiNestBuilder );
		}

		final Map<String, NestedChoiceSet<CapeTownModeNests>> result = new LinkedHashMap<>();
		result.put( "all" , allBuilder.build() );
		result.put( "base" , baseBuilder.build() );
		result.put( "nocar" , nocarBuilder.build() );
		result.put( "nopt" , noptBuilder.build() );
		result.put( "nowalk" , nowalkBuilder.build() );

		return result;
	}

	private boolean isCarAvailable( Person person ) {
		return (hasCarLicense( person ) || hasMotoLicense( person ) ) &&
				(getHouseholdInteger( person , "numberOfHouseholdCarsOwned" ) > 0 ||
					getHouseholdInteger( person , "numberOfHouseholdMotorcyclesOwned" ) > 0 );
	}

	private Integer getHouseholdInteger( Person decisionMaker , String att ) {
		final Id<Household> hh = person2household.get( decisionMaker.getId() );
		if ( hh == null ) throw new IllegalStateException( "no household ID for person "+decisionMaker.getId() );
		return (Integer) households.getHouseholdAttributes().getAttribute( hh.toString() , att );
	}

	private boolean hasMotoLicense( Person decisionMaker ) {
		final String license = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"license_motorcycle" );
		switch( PersonEnums.LicenseMotorcycle.parseFromDescription( license ) ) {
			case YES:
				return true;
			case UNKNOWN:
			case NO:
				return false;
		}
		throw new RuntimeException();
	}

	private boolean hasCarLicense( Person decisionMaker ) {
		final String license = (String)
				personAttributes.getAttribute(
						decisionMaker.getId().toString(),
						"license_car" );
		switch ( PersonEnums.LicenseCar.parseFromDescription( license ) ) {
			case YES:
				return true;
			case UNKNOWN:
			case NO:
				return false;
		}
		throw new RuntimeException();
	}

	private Alternative<CapeTownModeNests> calcAlternative(
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
				CapeTownModeNests.valueOf( mode ),
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

					@Override
					public void setCoord(Coord coord) {
						// TODO Auto-generated method stub
						throw new RuntimeException("not implemented") ;
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

	private void add( Alternative<CapeTownModeNests> alternative , Nest.Builder<CapeTownModeNests>... sets ) {
		for ( Nest.Builder<CapeTownModeNests> set : sets ) {
			if ( set == null ) continue;
			set.addAlternative( alternative );
		}
	}

	private static class ChoiceSetBuilder {
		final Nest.Builder<CapeTownModeNests> carNestBuilder;
		final Nest.Builder<CapeTownModeNests> ptNestBuilder ;
		final Nest.Builder<CapeTownModeNests> walkNestBuilder ;
		final Nest.Builder<CapeTownModeNests> rideNestBuilder ;
		final Nest.Builder<CapeTownModeNests> taxiNestBuilder ;

		public ChoiceSetBuilder( final CapeTownNestedLogitModelConfigGroup c ) {
			carNestBuilder =
					new Nest.Builder<CapeTownModeNests>()
							.setMu( c.getMuCar() )
							.setName( CapeTownModeNests.car );
			ptNestBuilder =
					new Nest.Builder<CapeTownModeNests>()
							.setMu( c.getMuPt() )
							.setName( CapeTownModeNests.pt );
			walkNestBuilder =
					new Nest.Builder<CapeTownModeNests>()
							.setMu( c.getMuWalk() )
							.setName( CapeTownModeNests.walk );
			rideNestBuilder =
					new Nest.Builder<CapeTownModeNests>()
							.setMu( c.getMuRide() )
							.setName( CapeTownModeNests.ride );
			taxiNestBuilder =
					new Nest.Builder<CapeTownModeNests>()
							.setMu( c.getMuTaxi() )
							.setName( CapeTownModeNests.taxi );

		}

		public NestedChoiceSet<CapeTownModeNests> build() {
			return new NestedChoiceSet<>(
					carNestBuilder.build(),
					ptNestBuilder.build(),
					walkNestBuilder.build(),
					rideNestBuilder.build(),
					taxiNestBuilder.build());
		}
	}
}
