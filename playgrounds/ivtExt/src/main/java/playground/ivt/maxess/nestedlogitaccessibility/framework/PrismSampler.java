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
package playground.ivt.maxess.nestedlogitaccessibility.framework;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author thibautd
 */
public class PrismSampler {

	private final Random random = MatsimRandom.getLocalInstance();
	private final int nSamples;
	private final ActivityFacilities allFacilities;
	private final QuadTree<ActivityFacility> relevantFacilities;
	private final int budget_m;

	public PrismSampler(
			final String activityType,
			final int nSamples,
			final ActivityFacilities allFacilities,
			final int budget_m ) {
		this.nSamples = nSamples;
		this.allFacilities = allFacilities;
		this.budget_m = budget_m;

		final QuadTreeRebuilder<ActivityFacility> builder = new QuadTreeRebuilder<>();
		allFacilities.getFacilities().values().stream()
				.filter( f -> f.getActivityOptions().containsKey( activityType ) )
				.forEach( f -> builder.put( f.getCoord(), f ) );

		this.relevantFacilities = builder.getQuadTree();
	}

	public void resetRandomSeed( final long s ) {
		random.setSeed( s );
		for ( int i = 0; i < 10; i++ ) random.nextDouble();
	}

	public ActivityFacility getOrigin( Person p ) {
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

	public List<ActivityFacility> calcSampledPrism( ActivityFacility f ) {
		final List<ActivityFacility> fullPrism = calcFullPrism( f );
		final List<ActivityFacility> sampled = new ArrayList<>( nSamples );

		for ( int i=0; i < nSamples; i++ ) {
			sampled.add( fullPrism.remove( random.nextInt( fullPrism.size() )));
		}

		return sampled;
	}

	public List<ActivityFacility> calcFullPrism( ActivityFacility p ) {
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

}
