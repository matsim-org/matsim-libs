/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerEvent.java
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

package org.matsim.core.controler.events;

import org.matsim.core.controler.MatsimServices;

/**
 * Basic event class for all Events fired by the Controler
 *
 * @author dgrether
 */
public abstract class ControlerEvent {
	/**
	 * The Controler instance which fired this event
	 */
	protected final MatsimServices services;

	public ControlerEvent(final MatsimServices services) {
		this.services = services;
	}

	/**
	 * Returns an aggregate interface of many services which are available during a MATSim run.
	 * Consider if you can instead only use the concrete services which you need.
	 * Everything which this interface returns is also accessible via the @Inject annotation.
	 *
	 * @return the global services interface
	 */
	public MatsimServices getServices() {
		return this.services;
	}

}
