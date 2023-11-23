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
package org.matsim.contrib.socnetsim.jointactivities.replanning;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;

/**
 * Links plans with "joinable" activities, that is,
 * plans of social contacts with activities of a given type
 * at the same location.
 * Ideally, time overlap should be included.
 * @author thibautd
 */
public class JoinableActivitiesPlanLinkIdentifier implements PlanLinkIdentifier {
	private static final Logger log =
		LogManager.getLogger(JoinableActivitiesPlanLinkIdentifier.class);

	private final String type;

	public JoinableActivitiesPlanLinkIdentifier(
			final String type) {
		this.type = type;
	}

	@Override
	public boolean areLinked(
			final Plan p1,
			final Plan p2) {
		final Queue<LocationEvent> events = new PriorityQueue<LocationEvent>( p1.getPlanElements().size() + p2.getPlanElements().size() );

		fillEvents( p1 , events );
		fillEvents( p2 , events );

		final Id pers1 = p1.getPerson().getId();
		Id loc1 = null;
		String type1 = null;
		Id loc2 = null;
		String type2 = null;

		while ( !events.isEmpty() ) {
			final LocationEvent event = events.remove();

			if ( log.isTraceEnabled() ) {
				log.trace( "got event "+event+" from queue" );
			}

			if ( pers1.equals( event.getPersonId() ) ) {
				loc1 = event.getLocationId();
				type1 = event.getActType();
			}
			else {
				loc2 = event.getLocationId();
				type2 = event.getActType();
			}

			if ( loc1 != null && loc2 != null &&
					loc1.equals( loc2 ) &&
					type1.equals( type ) && type2.equals( type ) ) {
				return true;
			}
		}

		return false;
	}

	private void fillEvents(
			final Plan plan,
			final Queue<LocationEvent> events) {
		final Id personId = plan.getPerson().getId();
		double lastEnd = 0;
		int ind = 0;
		List<Activity> activities = TripStructureUtils.getActivities(plan,
				StageActivityHandling.ExcludeStageActivities);
		for ( int i = 0; i < activities.size(); i++) {
			Activity act = activities.get(i);
			final Id loc = act.getFacilityId();

			final LocationEvent event =
				new LocationEvent(
						ind++,
						personId,
						act.getType(),
						loc,
						lastEnd );

			if (i < activities.size() - 1) { // skip the last activity
				// correct times if inconsistent
				lastEnd = Math.max(lastEnd, act.getEndTime().isDefined()
						? act.getEndTime().seconds()
						: lastEnd + act.getMaximumDuration().seconds());
			}

			if ( log.isTraceEnabled() ) {
				log.trace( "add event "+event+" to queue" );
			}

			events.add( event );
		}
	}

	private static class LocationEvent implements Comparable<LocationEvent> {
		private final String type;
		private final Id locId;
		private final Id personId;
		private final double startTime;
		private int index;

		public LocationEvent(
				final int index,
				final Id personId,
				final String type,
				final Id locId,
				final double startTime ) {
			if ( locId == null ) throw new NullPointerException( "null location for person "+personId+" activity type "+type );
			this.index = index;
			this.type = type;
			this.locId = locId;
			this.personId = personId;
			this.startTime = startTime;
		}

		public Id getLocationId() { return locId; }
		public Id getPersonId() { return personId; }
		public String getActType() { return type; }

		@Override
		public int compareTo(final LocationEvent o) {
			final int comp = Double.compare( startTime , o.startTime );
			if ( comp != 0 ) return comp;

			// order as in plan.
			if ( personId.equals( o.personId ) ) return index - o.index;

			// ugly, but could not find a better way to enforce consistency
			return personId.compareTo( o.personId );
		}

		@Override
		public String toString() {
			return "[person="+personId+
				"; type="+type+
				"; location="+locId+
				"; startTime="+startTime+"]";
		}
	}
}
