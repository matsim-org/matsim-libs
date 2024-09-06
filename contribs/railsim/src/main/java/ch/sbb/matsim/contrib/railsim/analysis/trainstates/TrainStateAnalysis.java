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

package ch.sbb.matsim.contrib.railsim.analysis.trainstates;

import ch.sbb.matsim.contrib.railsim.eventhandlers.RailsimTrainStateEventHandler;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler collecting all {@link RailsimTrainStateEvent}s.
 */
public final class TrainStateAnalysis implements RailsimTrainStateEventHandler {

	final List<RailsimTrainStateEvent> events = new ArrayList<>(1000);

	@Override
	public void handleEvent(RailsimTrainStateEvent event) {
		this.events.add(event);
	}

	public List<RailsimTrainStateEvent> getEvents() {
		return events;
	}
}
