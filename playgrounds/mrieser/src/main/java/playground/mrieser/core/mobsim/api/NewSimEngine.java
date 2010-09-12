/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.api;

import org.matsim.core.api.experimental.events.EventsManager;

/**
 * @author mrieser
 */
public interface NewSimEngine {

	public EventsManager getEventsManager();

	public double getCurrentTime();

	/**
	 * Handles the agent by ending its current plan element and starting its next plan element.
	 *
	 * @param agent
	 */
	public void handleAgent(final PlanAgent agent);

	public void runSim();

	public void addKeepAlive(final SimKeepAlive keepAlive);

}
