/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.shipment;

import java.util.Collection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.TimeWindow;
import org.matsim.freight.logistics.HasSimulationTrackers;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * Represents a shipment of goods from one location to another (for example a parcel a carrier moves
 * from A to B). A shipment may be handled by more than one carrier along the way. It is the unit
 * that a Logistics Service Provider ({@link org.matsim.freight.logistics.LSP}) assigns to a
 * transport chain.
 * <p>Questions/comments:
 *
 * <ul>
 *   <li>Within more modern MATSim, we would probably prefer to have from and to in coordinates, not
 *       link IDs.
 * </ul>
 */
public interface LspShipment
    extends Identifiable<LspShipment>, Attributable, HasSimulationTrackers<LspShipment> {

  /** @return the link the shipment is picked up from (same concept as in CarrierShipment). */
  Id<Link> getFrom();

  /** @return the link the shipment is delivered to (same concept as in CarrierShipment). */
  Id<Link> getTo();

  /** @return the time window during which the shipment must be picked up. */
  TimeWindow getPickupTimeWindow();

  /** @return the time window during which the shipment must be delivered. */
  TimeWindow getDeliveryTimeWindow();

  /** @return the size of the shipment, e.g. the number of units or its capacity demand. */
  int getSize();

  /** @return the service time required to deliver the shipment. */
  double getDeliveryServiceTime();

  /** @return the service time required to pick up the shipment. */
  double getPickupServiceTime();

  /**
   * @return the record of what happened to this shipment during the simulation (load, transport,
   *     unload, handling).
   */
  LspShipmentPlan getShipmentLog();

  /** @return the requirements that must be fulfilled for this shipment to be handled. */
  Collection<LspShipmentRequirement> getRequirements();

}
