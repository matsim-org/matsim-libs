/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToPlans.java
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
package playground.thibautd.utils;

import com.google.common.eventbus.Subscribe;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.*;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.utils.collections.MapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class EventsToPlans implements ActivityHandler, LegHandler {

	private final IdFilter filter;

	private boolean locked = false;
	private final Map<Id, Plan> agentsPlans = new HashMap<Id, Plan>();

	public static interface IdFilter {
		public boolean accept(final Id id);
	}

	public EventsToPlans() {
		this( new IdFilter() {
			@Override
			public boolean accept(final Id id) {
				return true;
			}
		});
	}

	@Override
	public void handleActivity(final PersonExperiencedActivity event) {
		if ( !filter.accept( event.getAgentId()) ) return;
		final Plan plan =
				MapUtils.getArbitraryObject(
						event.getAgentId(),
						agentsPlans,
						new MapUtils.Factory<Plan>() {
							@Override
							public Plan create() {
								return new PlanImpl(PopulationUtils.createPerson(event.getAgentId()));
							}
						});
		plan.addActivity( event.getActivity());

	}

	@Override
	public void handleLeg(final PersonExperiencedLeg event) {
		if ( !filter.accept( event.getAgentId()) ) return;
		final Plan plan =
				MapUtils.getArbitraryObject(
						event.getAgentId(),
						agentsPlans,
						new MapUtils.Factory<Plan>() {
							@Override
							public Plan create() {
								return new PlanImpl(PopulationUtils.createPerson(event.getAgentId()));
							}
						});
		plan.addLeg( event.getLeg() );

	}
	public EventsToPlans(final IdFilter filter) {
		this.filter = filter;
	}

	public Map<Id, Plan> getPlans() {
		if ( !locked ) {
			locked = true;
		}
		return agentsPlans;
	}



}

