/* *********************************************************************** *
 * project: org.matsim.*
 * AgentStuckEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.api.core.v01.events.handler;

import org.matsim.api.core.v01.events.PersonStuckAndContinueEvent;
import org.matsim.core.events.handler.EventHandler;

public interface PersonStuckAndContinueEventHandler extends EventHandler {
	public void handleEvent (PersonStuckAndContinueEvent event);
}
