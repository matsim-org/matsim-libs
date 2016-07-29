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
package playground.ivt.maxess.nestedlogitaccessibility.scripts.simpleleisure;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.ChoiceSetIdentifier;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Nest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedChoiceSet;
import playground.ivt.maxess.nestedlogitaccessibility.framework.PrismSampler;
import playground.ivt.maxess.nestedlogitaccessibility.scripts.ModeNests;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;
import playground.ivt.utils.ConcurrentStopWatch;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class SimpleNestedLogitModelChoiceSetIdentifier implements ChoiceSetIdentifier<ModeNests> {
	public enum Measurement { carTravelTime, ptTravelTime, bikeTravelTime, walkTravelTime, prismSampling; }

	private final TripRouter router;

	private final ObjectAttributes personAttributes;
	private final PrismSampler prismSampler;

	private final SimpleNestedLogitUtilityConfigGroup configGroup;

	private final ConcurrentStopWatch<Measurement> stopWatch;

	public SimpleNestedLogitModelChoiceSetIdentifier(
			final SimpleNestedLogitUtilityConfigGroup configGroup,
			final ConcurrentStopWatch<Measurement> stopWatch,
			final String type,
			final int nSamples,
			final TripRouter router,
			final ActivityFacilities allFacilities,
			final ObjectAttributes personAttributes,
			final int budget_m ) {
		this.configGroup = configGroup;
		this.stopWatch = stopWatch;
		this.router = router;
		this.personAttributes = personAttributes;

		this.prismSampler = new PrismSampler( type , nSamples , allFacilities , budget_m );
	}

	@Override
	public Map<String, NestedChoiceSet<ModeNests>> identifyChoiceSet( final Person person ) {
		final ChoiceSetBuilder baseBuilder = new ChoiceSetBuilder( configGroup );
		final ChoiceSetBuilder nocarBuilder = new ChoiceSetBuilder( configGroup );
		final ChoiceSetBuilder noptBuilder = new ChoiceSetBuilder( configGroup );
		final ChoiceSetBuilder nobikeBuilder = new ChoiceSetBuilder( configGroup );
		final ChoiceSetBuilder nowalkBuilder = new ChoiceSetBuilder( configGroup );
		final ChoiceSetBuilder allBuilder = new ChoiceSetBuilder( configGroup );

		// Sample and route alternatives
		stopWatch.startMeasurement( Measurement.prismSampling );
		final ActivityFacility origin = prismSampler.getOrigin( person );
		final List<ActivityFacility> prism = prismSampler.calcSampledPrism( origin );
		stopWatch.endMeasurement( Measurement.prismSampling );

		int i = 0;
		for ( ActivityFacility f : prism ) {
			i++;
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

	private void add( Alternative<ModeNests> alternative , Nest.Builder<ModeNests>... sets ) {
		for ( Nest.Builder<ModeNests> set : sets ) {
			if ( set == null ) continue;
			set.addAlternative( alternative );
		}
	}

	private static class ChoiceSetBuilder {
		final Nest.Builder<ModeNests> carNestBuilder;
		final Nest.Builder<ModeNests> ptNestBuilder ;
		final Nest.Builder<ModeNests> bikeNestBuilder ;
		final Nest.Builder<ModeNests> walkNestBuilder ;

		public ChoiceSetBuilder( SimpleNestedLogitUtilityConfigGroup c ) {
			carNestBuilder =
					new Nest.Builder<ModeNests>()
							.setMu( c.getMuCar() )
							.setName( ModeNests.car );
			ptNestBuilder =
					new Nest.Builder<ModeNests>()
							.setMu( c.getMuPt() )
							.setName( ModeNests.pt );
			bikeNestBuilder =
					new Nest.Builder<ModeNests>()
							.setMu( c.getMuBike() )
							.setName( ModeNests.bike );
			walkNestBuilder =
					new Nest.Builder<ModeNests>()
							.setMu( c.getMuWalk() )
							.setName( ModeNests.walk );

		}

		public NestedChoiceSet<ModeNests> build() {
			return new NestedChoiceSet<>(
					carNestBuilder.build(),
					ptNestBuilder.build(),
					bikeNestBuilder.build(),
					walkNestBuilder.build() );
		}
	}
}
