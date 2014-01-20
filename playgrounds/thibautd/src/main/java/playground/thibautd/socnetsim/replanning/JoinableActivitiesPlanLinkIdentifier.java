/* *********************************************************************** *
 * project: org.matsim.*
 * JoinableActivitiesPlanLinkIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;

/**
 * Links plans with "joinable" activities, that is,
 * plans of social contacts with activities of a given type
 * at the same location.
 * Ideally, time overlap should be included.
 * @author thibautd
 */
public class JoinableActivitiesPlanLinkIdentifier implements PlanLinkIdentifier {
	private final String type;

	public JoinableActivitiesPlanLinkIdentifier(
			final String type) {
		this.type = type;
	}

	@Override
	public boolean areLinked(
			final Plan p1,
			final Plan p2) {
		final Set<Id> locs1 = getLocations( p1 );
		final Set<Id> locs2 = getLocations( p2 );

		for ( Id l : locs1 ) {
			if ( locs2.contains( l ) ) return true;
		}

		return false;
	}

	private Set<Id> getLocations(final Plan p) {
		final Set<Id> locs = new HashSet<Id>();

		for ( Activity act : TripStructureUtils.getActivities( p , EmptyStageActivityTypes.INSTANCE ) ) {
			if ( act.getType().equals( type ) ) {
				locs.add(
						act.getFacilityId() != null ?
							act.getFacilityId() :
							act.getLinkId() );
			}
		}

		return locs;
	}
}
