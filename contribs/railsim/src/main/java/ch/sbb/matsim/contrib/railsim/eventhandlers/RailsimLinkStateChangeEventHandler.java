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

package ch.sbb.matsim.contrib.railsim.eventhandlers;

import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import org.matsim.core.events.handler.EventHandler;

/**
 * Handler for {@link RailsimLinkStateChangeEvent}.
 */
public interface RailsimLinkStateChangeEventHandler extends EventHandler {

	/**
	 * Process given event.
	 */
	void handleEvent(RailsimLinkStateChangeEvent event);
}
