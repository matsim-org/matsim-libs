/* *********************************************************************** *
 * project: org.matsim.*
 * WarmEmissionEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.emissions.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * Implement this to get notified when WarmEmissionEvents are thrown
 * 
 * @author benjamin
 */

public interface WarmEmissionEventHandler extends EventHandler {
	// leave this public so that external code can generate "standard" emission events. MATSIM-893

	void handleEvent( WarmEmissionEvent event );
}
