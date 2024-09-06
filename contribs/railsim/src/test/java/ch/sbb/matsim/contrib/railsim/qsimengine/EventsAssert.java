/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.Condition;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

import java.util.Collection;
import java.util.stream.StreamSupport;

class EventsAssert extends AbstractCollectionAssert<EventsAssert, Collection<? extends Event>, Event, EventAssert> {
	protected EventsAssert(Collection<? extends Event> events, Class<?> selfType) {
		super(events, selfType);
	}

	public EventsAssert hasTrainState(String veh, double time, double headPosition, double speed) {
		return haveExactly(1,
			new Condition<>(event ->
				(event instanceof RailsimTrainStateEvent ev)
					&& ev.getVehicleId().equals(Id.createVehicleId(veh))
					&& FuzzyUtils.equals(ev.getTime(), time)
					&& FuzzyUtils.equals(ev.getHeadPosition(), headPosition)
					&& FuzzyUtils.equals(ev.getSpeed(), speed),
				String.format("event with veh %s time %.0f headPosition: %.2f speed: %.2f", veh, time, headPosition, speed))
		);
	}

	public EventsAssert hasTrainState(String veh, double time, String headLink, double speed) {
		return haveAtLeast(1,
			new Condition<>(event ->
				(event instanceof RailsimTrainStateEvent ev)
					&& ev.getVehicleId().toString().equals(veh)
					&& FuzzyUtils.equals(ev.getTime(), time)
					&& ev.getHeadLink().toString().equals(headLink)
					&& FuzzyUtils.equals(ev.getSpeed(), speed),
				String.format("event with veh %s time %.0f link: %s speed: %.2f", veh, time, headLink, speed))
		);
	}


	@Override
	protected EventAssert toAssert(Event value, String description) {
		return null;
	}

	@Override
	protected EventsAssert newAbstractIterableAssert(Iterable<? extends Event> iterable) {
		return new EventsAssert(StreamSupport.stream(iterable.spliterator(), false).toList(), EventsAssert.class);
	}
}
