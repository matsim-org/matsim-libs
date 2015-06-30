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

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacilities;
import playground.thibautd.maxess.prepareforbiogeme.framework.ChoicesIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author thibautd
 */
public class TripChoicesIdentifier implements ChoicesIdentifier<TripChoiceSituation> {
	private final ActivityFacilities facilities;
	private final StageActivityTypes stages;

	public TripChoicesIdentifier(
			final ActivityFacilities facilities,
			final StageActivityTypes stages) {
		this.facilities = facilities;
		this.stages = stages;
	}

	@Override
	public List<TripChoiceSituation> identifyChoices(final Plan p) {
		final List<TripStructureUtils.Trip> trips = Collections.unmodifiableList( TripStructureUtils.getTrips( p , stages ) );

		final List<TripChoiceSituation> choices = new ArrayList<>( trips.size() );

		int i=0;
		for ( TripStructureUtils.Trip t : trips ) {
			final Trip choice =
					new Trip(
							facilities.getFacilities().get( t.getOriginActivity().getFacilityId() ),
							t.getTripElements(),
							facilities.getFacilities().get( t.getDestinationActivity().getFacilityId() ) );
			choices.add( new TripChoiceSituation( choice , trips , i++ ) );
		}

		return choices;
	}
}
