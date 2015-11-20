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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.maxess.nestedlogitaccessibility.Alternative;
import playground.thibautd.maxess.nestedlogitaccessibility.ChoiceSetIdentifier;
import playground.thibautd.maxess.nestedlogitaccessibility.Nest;
import playground.thibautd.maxess.nestedlogitaccessibility.NestedChoiceSet;
import playground.thibautd.maxess.prepareforbiogeme.tripbased.Trip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class SimpleNestedLogitModelChoiceSetIdentifier implements ChoiceSetIdentifier<ModeNests> {
	private final Random random = MatsimRandom.getLocalInstance();
	private final int nSamples;
	private final TripRouter router;

	private final ActivityFacilities allFacilities;
	private final QuadTree<ActivityFacility> relevantFacilities;
	private final int budget_m;

	public SimpleNestedLogitModelChoiceSetIdentifier(
			final String type,
			final int nSamples,
			final TripRouter router,
			final ActivityFacilities allFacilities,
			final int budget_m ) {
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
						.setMu( 1 )
						.setName( ModeNests.car );
		final Nest.Builder<ModeNests> ptNestBuilder =
				new Nest.Builder<ModeNests>()
						.setMu( 1 )
						.setName( ModeNests.pt );
		final Nest.Builder<ModeNests> bikeNestBuilder =
				new Nest.Builder<ModeNests>()
						.setMu( 6.42 )
						.setName( ModeNests.bike );
		final Nest.Builder<ModeNests> walkNestBuilder =
				new Nest.Builder<ModeNests>()
						.setMu( 1.74 )
						.setName( ModeNests.walk );

		// Sample and route alternatives
		final ActivityFacility origin = getOrigin( person );
		final List<ActivityFacility> prism = calcPrism( origin );
		for ( int i= 0; i < nSamples; i++ ) {
			final ActivityFacility f = prism.remove( random.nextInt( prism.size() ) );

			carNestBuilder.addAlternative(
							calcAlternative(
									i,
									TransportMode.car,
									origin,
									f,
									person ) );
			ptNestBuilder.addAlternative(
							calcAlternative(
									i,
									TransportMode.pt,
									origin,
									f,
									person ) );
			bikeNestBuilder.addAlternative(
							calcAlternative(
									i,
									TransportMode.bike,
									origin,
									f,
									person ) );
			walkNestBuilder.addAlternative(
							calcAlternative(
									i,
									TransportMode.walk,
									origin,
									f,
									person ) );
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
		// TODO: fake facility in case no ID
		return allFacilities.getFacilities().get( act.getFacilityId() );
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
