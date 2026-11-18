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

package org.matsim.freight.logistics.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * Factory for {@link Event}s that can be created from a generic event. Implementations typically
 * transform a {@link LspShipment} related event into a logistics event together with the activity
 * it belongs to.
 *
 * @author Kai Martins-Turner (kturner)
 */
public interface LspEventCreator {

  /**
   * Create a logistics {@link Event} from a generic event.
   * (I am unsure, if I need the activity or not. kmt 'dec22)
   *
   * @param event the generic event to convert
   * @param lspShipmentId the {@link Id} of the {@link LspShipment} the event belongs to
   * @param activity the {@link Activity} the event belongs to
   * @return the created {@link Event}
   */
  Event createEvent(Event event, Id<LspShipment> lspShipmentId, Activity activity);
}
