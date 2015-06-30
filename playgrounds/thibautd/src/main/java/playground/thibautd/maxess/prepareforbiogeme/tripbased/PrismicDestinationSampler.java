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
package playground.thibautd.maxess.prepareforbiogeme.tripbased;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.maxess.prepareforbiogeme.tripbased.RoutingChoiceSetSampler.DestinationSampler;

import java.util.*;

/**
 * @author thibautd
 */
public class PrismicDestinationSampler implements DestinationSampler {
	private final ActivityFacilities facilities;
	private final int nSamples;
	private final double budget_m;

	private final QuadTree<ActivityFacility> relevantFacilities;

	private final Random random = MatsimRandom.getLocalInstance();

	public PrismicDestinationSampler(
			final String type,
			final ActivityFacilities facilities,
			final int nSamples,
			final double budget_m) {
		this.facilities = facilities;
		this.nSamples = nSamples;
		this.budget_m = budget_m;

		final QuadTreeRebuilder<ActivityFacility> builder = new QuadTreeRebuilder<>();
		for ( ActivityFacility f : facilities.getFacilities().values() ) {
			if ( f.getActivityOptions().containsKey( type ) ) {
				builder.put( f.getCoord() , f );
			}
		}
		relevantFacilities = builder.getQuadTree();
	}


	@Override
	public Collection<ActivityFacility> sampleDestinations(
			final Person decisionMaker,
			final TripChoiceSituation choice) {
		final List<ActivityFacility> prism = getPrism( choice );

		final Collection<ActivityFacility> choiceSet = new ArrayList<>( nSamples );
		for ( int i=0; i < nSamples; i++ ) {
			choiceSet.add( prism.remove( random.nextInt( prism.size() ) ) );
		}

		return choiceSet;
	}

	private List<ActivityFacility> getPrism(final TripChoiceSituation choice) {
		final Coord f1 = choice.getChoice().getOrigin().getCoord();
		final Coord f2 = choice.getTripSequence().get(choice.getPositionInTripSequence() + 1).getDestinationActivity().getCoord();

		Collection<ActivityFacility> prism = Collections.emptyList();

		for ( int i=1; prism.size() < nSamples; i++ ) {
			prism = relevantFacilities.getElliptical(f1.getX(), f1.getY(), f2.getX(), f2.getY(), i * budget_m);
		}

		return prism instanceof List ? (List<ActivityFacility>) prism : new ArrayList<>( prism );
	}


}
