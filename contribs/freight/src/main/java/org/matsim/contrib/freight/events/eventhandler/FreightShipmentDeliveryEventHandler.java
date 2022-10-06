/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.events.eventhandler;

import org.matsim.contrib.freight.events.FreightShipmentDeliveryEndEvent;
import org.matsim.core.events.handler.EventHandler;

/**
 * Interface to listen to shipmentDeliveredEvents.
 * 
 * @author sschroeder
 *
 */
public interface FreightShipmentDeliveryEventHandler extends EventHandler {

	public void handleEvent(FreightShipmentDeliveryEndEvent event);

}
