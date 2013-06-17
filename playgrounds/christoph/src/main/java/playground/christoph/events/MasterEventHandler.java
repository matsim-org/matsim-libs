/* *********************************************************************** *
 * project: org.matsim.*
 * MasterEventHandler.java
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

package playground.christoph.events;

import org.matsim.core.events.handler.EventHandler;

public interface MasterEventHandler extends EventHandler {

	/**
	 * Creates an instance of an EventHandler. All instances can synchronize their
	 * results in every time step.
	 * @return
	 */
	public EventHandlerInstance createInstance();
	
	/**
	 * Indicates that the events handling can be finished. Therefore, the data from
	 * the instances of the EventHandler can be collected and synchronized. 
	 */
	public void finishEventsHandling();
	
	/**
	 * Collect and synchronize data from the instances.
	 * @param time
	 */
	public void synchronize(double time);
}
