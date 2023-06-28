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

import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.vehicles.Vehicle;

public class RailsimTrainStateEventMapper implements MatsimEventsReader.CustomEventMapper {
	@Override
	public RailsimTrainStateEvent apply(GenericEvent event) {
		var attributes = event.getAttributes();
		return new RailsimTrainStateEvent(
			event.getTime(),
			Double.parseDouble(attributes.get(RailsimTrainStateEvent.ATTRIBUTE_EXACT_TIME)),
			asId(attributes.get(RailsimTrainStateEvent.ATTRIBUTE_VEHICLE), Vehicle.class),
			asId(attributes.get(RailsimTrainStateEvent.ATTRIBUTE_HEADLINK), Link.class),
			Double.parseDouble(attributes.get(RailsimTrainStateEvent.ATTRIBUTE_HEADPOSITION)),
			asId(attributes.get(RailsimTrainStateEvent.ATTRIBUTE_TAILLINK), Link.class),
			Double.parseDouble(attributes.get(RailsimTrainStateEvent.ATTRIBUTE_TAILPOSITION)),
			Double.parseDouble(attributes.get(RailsimTrainStateEvent.ATTRIBUTE_SPEED)),
			Double.parseDouble(attributes.get(RailsimTrainStateEvent.ATTRIBUTE_ACCELERATION)),
			Double.parseDouble(attributes.get(RailsimTrainStateEvent.ATTRIBUTE_TARGETSPEED))
		);
	}

	private static <T> Id<T> asId(String value, Class<T> idClass) {
		if (value == null) {
			return null;
		}
		return Id.create(value, idClass);
	}
}
