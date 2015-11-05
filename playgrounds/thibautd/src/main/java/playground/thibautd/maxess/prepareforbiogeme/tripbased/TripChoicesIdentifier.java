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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import playground.thibautd.maxess.prepareforbiogeme.framework.ChoicesIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class TripChoicesIdentifier implements ChoicesIdentifier<TripChoiceSituation> {
	private static final Logger log = Logger.getLogger( TripChoicesIdentifier.class );
	private final ActivityFacilities facilities;
	private final StageActivityTypes stages;
	private final String destinationType;

	// incompatible with prism approach. Make configurable
	private final boolean ignoreLastTrip = true;

	public TripChoicesIdentifier(
			final String destinationType,
			final ActivityFacilities facilities,
			final StageActivityTypes stages) {
		this.destinationType = destinationType;
		this.facilities = facilities;
		this.stages = stages;
	}

	@Override
	public List<TripChoiceSituation> identifyChoices(final Plan p) {
		final List<TripStructureUtils.Trip> trips = Collections.unmodifiableList( TripStructureUtils.getTrips( p , stages ) );

		final List<TripChoiceSituation> choices = new ArrayList<>( trips.size() );

		int i=0;
		for ( TripStructureUtils.Trip t : trips ) {
			if ( t.getDestinationActivity().getType().equals( destinationType ) ) {
				if ( !ignoreLastTrip || i < trips.size() - 1 ) {
					final Trip choice =
							new Trip(
									getFacility( t.getOriginActivity() ),
									t.getTripElements(),
									getFacility( t.getDestinationActivity() ) );
					choices.add( new TripChoiceSituation( choice, trips, i ) );
				}
				else {
					log.warn( "Ignore last trip of "+p );
				}
			}
			i++;
		}

		return choices;
	}

	private ActivityFacility getFacility( final  Activity originActivity ) {
		final ActivityFacility ex = facilities.getFacilities().get( originActivity.getFacilityId() );

		return ex != null ? ex :
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
						return originActivity.getLinkId();
					}

					@Override
					public Coord getCoord() {
						return originActivity.getCoord();
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
}
