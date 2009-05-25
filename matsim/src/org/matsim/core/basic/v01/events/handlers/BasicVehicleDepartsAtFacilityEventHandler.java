/* *********************************************************************** *
 * project: org.matsim.*
 * BasicVehicleArrivesAtFacilityEventHandler.java
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

package org.matsim.core.basic.v01.events.handlers;

import org.matsim.core.basic.v01.events.BasicVehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.EventHandler;

/**
 * @author mrieser
 */
public interface BasicVehicleDepartsAtFacilityEventHandler extends EventHandler {

	public void handleEvent(BasicVehicleDepartsAtFacilityEvent event);
	
}
