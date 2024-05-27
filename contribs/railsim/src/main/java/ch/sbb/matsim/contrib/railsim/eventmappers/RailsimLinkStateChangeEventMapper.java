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

package ch.sbb.matsim.contrib.railsim.eventmappers;

import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.ResourceState;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.vehicles.Vehicle;

/**
 * Converter for railsim events.
 */
public class RailsimLinkStateChangeEventMapper implements MatsimEventsReader.CustomEventMapper {
	@Override
	public RailsimLinkStateChangeEvent apply(GenericEvent event) {
		var attributes = event.getAttributes();
		return new RailsimLinkStateChangeEvent(
			event.getTime(),
			asId(attributes.get(RailsimLinkStateChangeEvent.ATTRIBUTE_LINK), Link.class),
			asId(attributes.get(RailsimLinkStateChangeEvent.ATTRIBUTE_VEHICLE), Vehicle.class),
			ResourceState.valueOf(attributes.get(RailsimLinkStateChangeEvent.ATTRIBUTE_STATE))
		);
	}

	private static <T> Id<T> asId(String value, Class<T> idClass) {
		if (value == null) {
			return null;
		}
		return Id.create(value, idClass);
	}
}
